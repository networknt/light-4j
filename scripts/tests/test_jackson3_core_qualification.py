import importlib.util
import json
from pathlib import Path
import unittest
import sys
import tempfile
from unittest.mock import patch

sys.path.insert(0, str(Path(__file__).parents[1]))

spec = importlib.util.spec_from_file_location("qualification", Path(__file__).parents[1] / "jackson3_core_qualification.py")
qualification = importlib.util.module_from_spec(spec)
spec.loader.exec_module(qualification)
SHA = "a" * 40
TRUST = "9" * 40
REFS = {"yaml-rule": "b" * 40, "http-client": "c" * 40}


def run_record(**changes):
    result = dict(id=1, head_sha=TRUST, path=qualification.WORKFLOW, event="workflow_dispatch",
                  run_number=1, run_attempt=1, status="completed", conclusion="success",
                  html_url="https://example/run/1", head_repository={"full_name": qualification.REPOSITORY})
    result.update(changes)
    return result


def evidence_record(**changes):
    modules = {name: dict(tests=max(1, entry["minimum_executed"]), failures=0, errors=0, skipped=0) for name, entry in qualification.load_inventory().items()}
    result = dict(schema=2, policy_sha=TRUST, modules=modules, repository="networknt/light-4j", source_sha=SHA, run_id=1,
                  run_attempt=1, event="workflow_dispatch", workflow=qualification.WORKFLOW,
                  suite="full-reactor", replay_input={}, source_refs={"yaml-rule": "b" * 40, "http-client": "c" * 40},
                  tests=dict(tests=sum(m["tests"] for m in modules.values()), failures=0, errors=0, skipped=0))
    result.update(changes)
    return result


class FakeGitHub:
    def __init__(self):
        self.records = [run_record()]
        self.proof = evidence_record()
        self.artifact_records = [{"id": 10, "name": qualification.ARTIFACT_PREFIX + "1", "expired": False}]
        self.attempt_record = run_record()

    def runs(self, sha):
        return self.records

    def artifacts(self, run_id):
        return self.artifact_records if run_id == 1 else []

    def attempt(self, run_id, number):
        return self.attempt_record

    def evidence(self, artifact):
        return self.proof


class CoreQualificationTest(unittest.TestCase):
    def setUp(self):
        self.client = FakeGitHub()

    def test_exact_checkout_with_successful_attempt_evidence(self):
        self.assertEqual(SHA, qualification.qualify(self.client, SHA, REFS, TRUST)["evidence"]["source_sha"])

    def test_merge_checkout_is_rejected_even_when_run_head_matches(self):
        self.client.proof["source_sha"] = "d" * 40
        with self.assertRaises(ValueError):
            qualification.qualify(self.client, SHA, REFS, TRUST)

    def test_candidate_pr_ci_is_never_trusted_qualification(self):
        self.client.records[0]["event"] = "pull_request"
        with self.assertRaises(ValueError):
            qualification.qualify(self.client, SHA, REFS, TRUST)

    def test_wrong_policy_revision_is_rejected(self):
        self.client.proof["policy_sha"] = "e" * 40
        with self.assertRaises(ValueError):
            qualification.qualify(self.client, SHA, REFS, TRUST)

    def test_prerequisite_sources_must_match_consumer_checkouts(self):
        self.client.proof["source_refs"] = dict(REFS, **{"http-client": "d" * 40})
        with self.assertRaises(ValueError):
            qualification.qualify(self.client, SHA, REFS, TRUST)

    def test_later_pending_failed_cancelled_run_does_not_remove_valid_evidence(self):
        for state, result in (("in_progress", None), ("completed", "failure"), ("completed", "cancelled")):
            with self.subTest(state=state, result=result):
                self.client.records = [run_record(), run_record(id=2, run_number=2, status=state, conclusion=result)]
                self.assertEqual(1, qualification.qualify(self.client, SHA, REFS, TRUST)["run_id"])

    def test_prior_successful_attempt_survives_cancelled_rerun(self):
        self.client.records = [run_record(run_attempt=2, conclusion="cancelled")]
        self.assertEqual(1, qualification.qualify(self.client, SHA, REFS, TRUST)["run_attempt"])

    def test_failed_attempt_cannot_publish_qualification(self):
        self.client.attempt_record["conclusion"] = "failure"
        with self.assertRaises(ValueError):
            qualification.qualify(self.client, SHA, REFS, TRUST)

    def test_wrong_or_missing_source_run_and_empty_test_evidence(self):
        for changes in ({"run_id": 2}, {"run_attempt": 2}, {"source_refs": {}},
                        {"tests": {"tests": 0, "failures": 0, "errors": 0}}):
            with self.subTest(changes=changes), self.assertRaises(ValueError):
                self.client.proof = evidence_record(**changes)
                qualification.qualify(self.client, SHA, REFS, TRUST)

    def test_no_legacy_metadata_only_or_expired_qualification(self):
        for artifacts in ([], [{"id": 10, "name": qualification.ARTIFACT_PREFIX + "1", "expired": True}]):
            self.client.artifact_records = artifacts
            with self.assertRaises(ValueError):
                qualification.qualify(self.client, SHA, REFS, TRUST)

    def test_wrong_workflow_or_sha_is_rejected(self):
        for changes in ({"path": "other.yml"}, {"head_sha": "f" * 40}):
            self.client.records = [run_record(**changes)]
            with self.assertRaises(ValueError):
                qualification.qualify(self.client, SHA, REFS, TRUST)

    def test_workflow_run_pages_are_all_read(self):
        pages = [{"workflow_runs": [run_record(id=i) for i in range(100)]}, {"workflow_runs": [run_record(id=101)]}]
        with patch.object(qualification.subprocess, "check_output", return_value=json.dumps(pages)) as command:
            self.assertEqual(101, len(qualification.GitHub().runs(TRUST)))
            self.assertIn("--paginate", command.call_args.args[0])
            self.assertIn("--slurp", command.call_args.args[0])

    def test_no_independent_override_uses_recorded_prerequisites(self):
        result = qualification.qualify(self.client, SHA, {}, TRUST)
        self.assertEqual(REFS, result["evidence"]["source_refs"])

    def test_all_skipped_reports_cannot_qualify(self):
        for counts in self.client.proof["modules"].values():
            counts["skipped"] = counts["tests"]
        with self.assertRaisesRegex(ValueError, "Insufficient executed"):
            qualification.qualify(self.client, SHA, {}, TRUST)

    def test_missing_module_cannot_be_hidden_by_positive_total(self):
        self.client.proof["modules"].pop("config")
        with self.assertRaisesRegex(ValueError, "missing=.*config"):
            qualification.qualify(self.client, SHA, {}, TRUST)

    def test_missing_and_expired_evidence_explains_rerun(self):
        self.client.artifact_records = []
        with self.assertRaisesRegex(ValueError, "missing, expired.*rerun.*source_sha=" + SHA):
            qualification.qualify(self.client, SHA, {}, TRUST)

    def test_unpinned_or_candidate_controlled_policy_run_is_rejected(self):
        with self.assertRaisesRegex(ValueError, "approved full-SHA"):
            qualification.qualify(self.client, SHA, {}, "master")
        self.client.records[0]["head_sha"] = SHA
        with self.assertRaises(ValueError):
            qualification.qualify(self.client, SHA, {}, TRUST)

    def test_run_from_other_repository_is_rejected(self):
        self.client.records[0]["head_repository"] = {"full_name": "other/light-4j"}
        with self.assertRaises(ValueError):
            qualification.qualify(self.client, SHA, {}, TRUST)

    def test_selection_cli_outputs_recorded_sources_before_any_checkouts(self):
        with tempfile.TemporaryDirectory() as directory:
            report, output = Path(directory) / "report.json", Path(directory) / "outputs"
            argv = ["qualify", "--source-sha", SHA, "--trusted-revision", TRUST,
                    "--report", str(report), "--github-output", str(output)]
            with patch.object(sys, "argv", argv), patch.object(qualification, "GitHub", return_value=self.client), \
                    patch.object(qualification.subprocess, "check_output", side_effect=[TRUST, ""]):
                qualification.main()
            self.assertEqual(REFS, json.loads(report.read_text())["source_refs"])
            self.assertIn("yaml_rule=" + REFS["yaml-rule"], output.read_text())
            self.assertIn("http_client=" + REFS["http-client"], output.read_text())

    def test_selection_cli_rejects_a_verifier_from_the_candidate(self):
        with tempfile.TemporaryDirectory() as directory:
            report = Path(directory) / "report.json"
            argv = ["qualify", "--source-sha", SHA, "--trusted-revision", TRUST, "--report", str(report)]
            with patch.object(sys, "argv", argv), \
                    patch.object(qualification.subprocess, "check_output", return_value=SHA), \
                    self.assertRaisesRegex(SystemExit, "Verifier checkout"):
                qualification.main()

    def test_inventory_rejection_requires_review_instead_of_same_pin_rerun(self):
        self.client.proof["modules"]["new-module"] = dict(tests=5, failures=0, errors=0, skipped=0)
        with self.assertRaises(ValueError) as caught:
            qualification.qualify(self.client, SHA, {}, TRUST)
        message = str(caught.exception)
        self.assertIn("unexpected=['new-module']", message)
        self.assertIn("Promote a reviewed master policy", message)
        self.assertNotIn("rerun jackson3-source-build.yml", message)

    def test_large_module_cannot_drop_to_one_test(self):
        name = max(qualification.load_inventory(), key=lambda name: qualification.load_inventory()[name]["minimum_executed"])
        self.client.proof["modules"][name]["tests"] = 1
        with self.assertRaises(ValueError) as caught:
            qualification.qualify(self.client, SHA, {}, TRUST)
        self.assertIn("required=", str(caught.exception))
        self.assertIn("intentional test-corpus reduction", str(caught.exception))
        self.assertNotIn("rerun jackson3-source-build.yml", str(caught.exception))

    def test_wrong_candidate_evidence_does_not_suggest_policy_changes(self):
        self.client.proof["source_sha"] = "d" * 40
        self.client.proof["modules"] = {}
        with self.assertRaises(ValueError) as caught:
            qualification.qualify(self.client, SHA, {}, TRUST)
        self.assertNotIn("Test module coverage differs", str(caught.exception))
        self.assertIn("Only for an absent run or expired evidence", str(caught.exception))
