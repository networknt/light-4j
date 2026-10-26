import importlib.util
import json
from pathlib import Path
import unittest
from unittest.mock import patch

spec = importlib.util.spec_from_file_location("qualification", Path(__file__).parents[1] / "jackson3_core_qualification.py")
qualification = importlib.util.module_from_spec(spec)
spec.loader.exec_module(qualification)
SHA = "a" * 40
REFS = {"yaml-rule": "b" * 40, "http-client": "c" * 40}


def run_record(**changes):
    result = dict(id=1, head_sha=SHA, path=qualification.WORKFLOW, event="pull_request",
                  run_number=1, run_attempt=1, status="completed", conclusion="success",
                  html_url="https://example/run/1")
    result.update(changes)
    return result


def evidence_record(**changes):
    result = dict(schema=1, repository="networknt/light-4j", source_sha=SHA, run_id=1,
                  run_attempt=1, event="pull_request", workflow=qualification.WORKFLOW,
                  suite="full-reactor", replay_input={}, source_refs={"yaml-rule": "b" * 40, "http-client": "c" * 40},
                  tests=dict(tests=1365, failures=0, errors=0, skipped=89))
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
        self.assertEqual(SHA, qualification.qualify(self.client, SHA, REFS)["evidence"]["source_sha"])

    def test_merge_checkout_is_rejected_even_when_run_head_matches(self):
        self.client.proof["source_sha"] = "d" * 40
        with self.assertRaises(ValueError):
            qualification.qualify(self.client, SHA, REFS)

    def test_manual_dispatch_is_never_qualification(self):
        self.client.records[0]["event"] = "workflow_dispatch"
        with self.assertRaises(ValueError):
            qualification.qualify(self.client, SHA, REFS)

    def test_custom_replay_input_is_rejected(self):
        self.client.proof["replay_input"] = {"yaml-rule": "e" * 40}
        with self.assertRaises(ValueError):
            qualification.qualify(self.client, SHA, REFS)

    def test_prerequisite_sources_must_match_consumer_checkouts(self):
        self.client.proof["source_refs"] = dict(REFS, **{"http-client": "d" * 40})
        with self.assertRaises(ValueError):
            qualification.qualify(self.client, SHA, REFS)

    def test_later_pending_failed_cancelled_run_does_not_remove_valid_evidence(self):
        for state, result in (("in_progress", None), ("completed", "failure"), ("completed", "cancelled")):
            with self.subTest(state=state, result=result):
                self.client.records = [run_record(), run_record(id=2, run_number=2, status=state, conclusion=result)]
                self.assertEqual(1, qualification.qualify(self.client, SHA, REFS)["run_id"])

    def test_prior_successful_attempt_survives_cancelled_rerun(self):
        self.client.records = [run_record(run_attempt=2, conclusion="cancelled")]
        self.assertEqual(1, qualification.qualify(self.client, SHA, REFS)["run_attempt"])

    def test_failed_attempt_cannot_publish_qualification(self):
        self.client.attempt_record["conclusion"] = "failure"
        with self.assertRaises(ValueError):
            qualification.qualify(self.client, SHA, REFS)

    def test_wrong_or_missing_source_run_and_empty_test_evidence(self):
        for changes in ({"run_id": 2}, {"run_attempt": 2}, {"source_refs": {}},
                        {"tests": {"tests": 0, "failures": 0, "errors": 0}}):
            with self.subTest(changes=changes), self.assertRaises(ValueError):
                self.client.proof = evidence_record(**changes)
                qualification.qualify(self.client, SHA, REFS)

    def test_no_legacy_metadata_only_or_expired_qualification(self):
        for artifacts in ([], [{"id": 10, "name": qualification.ARTIFACT_PREFIX + "1", "expired": True}]):
            self.client.artifact_records = artifacts
            with self.assertRaises(ValueError):
                qualification.qualify(self.client, SHA, REFS)

    def test_wrong_workflow_or_sha_is_rejected(self):
        for changes in ({"path": "other.yml"}, {"head_sha": "f" * 40}):
            self.client.records = [run_record(**changes)]
            with self.assertRaises(ValueError):
                qualification.qualify(self.client, SHA, REFS)

    def test_workflow_run_pages_are_all_read(self):
        pages = [{"workflow_runs": [run_record(id=i) for i in range(100)]}, {"workflow_runs": [run_record(id=101)]}]
        with patch.object(qualification.subprocess, "check_output", return_value=json.dumps(pages)) as command:
            self.assertEqual(101, len(qualification.GitHub().runs(SHA)))
            self.assertIn("--paginate", command.call_args.args[0])
            self.assertIn("--slurp", command.call_args.args[0])
