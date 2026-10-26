import copy
import json
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest
from unittest.mock import patch
from unittest.mock import Mock

sys.path.insert(0, str(Path(__file__).parents[1]))
import jackson3_promotion_gates as gates
import prepare_jackson3_consumer_promotion as promotion

ENTRY = {"policy_sha": "a" * 40, "core_sha": "b" * 40,
         "policy_ref": "jackson3-qualification-policy-" + "a" * 40,
         "source_refs": {"yaml-rule": "c" * 40, "http-client": "d" * 40}}


def ruleset():
    return {"target": "branch", "enforcement": "active", "bypass_actors": [],
            "conditions": {"ref_name": {"include": ["refs/heads/" + ENTRY["policy_ref"]], "exclude": []}},
            "rules": [{"type": name} for name in sorted(gates.REQUIRED_RULES)]}


def record():
    return {"schema": 1, "decision": "accepted", "tuple": copy.deepcopy(ENTRY),
            "registration_sha": "e" * 40, "observed_ruleset_id": 123, "refresh_protocol": 1,
            "forced_renewal": {"coordinator_run_id": 10, "coordinator_attempt": 1,
                               "producer_run_id": 11, "producer_attempt": 1,
                               "conclusion": "success", "artifact_id": 12, "artifact_sha256": "f" * 64},
            "fresh_proof_reuse": {"coordinator_run_id": 13, "coordinator_attempt": 1,
                                  "conclusion": "success", "dispatched": 0}}


def markdown(data):
    return "# Acceptance\n\n```json\n" + json.dumps(data) + "\n```\n"


class ProtectionTest(unittest.TestCase):
    def test_equivalent_recreated_ruleset_and_additional_rules_are_allowed(self):
        config = ruleset()
        config["rules"].append({"type": "required_signatures"})
        def github(endpoint):
            if endpoint.startswith("commits/"):
                return {"sha": ENTRY["policy_sha"]}
            if endpoint.startswith("rules/branches/"):
                return [{"type": name, "ruleset_id": 456} for name in gates.REQUIRED_RULES]
            self.assertEqual("rulesets/456", endpoint)
            return config
        self.assertEqual(456, gates.require_protected_ref(ENTRY, github))

    def check(self, config=None, effective=None, sha=None, **options):
        effective = effective if effective is not None else [
            {"type": name, "ruleset_id": 123} for name in gates.REQUIRED_RULES]
        responses = {"commits/" + ENTRY["policy_ref"]: {"sha": sha or ENTRY["policy_sha"]},
                     "rules/branches/" + ENTRY["policy_ref"]: effective, "rulesets/123": config or ruleset()}
        return gates.require_protected_ref(ENTRY, responses.__getitem__, **options)

    def test_read_only_token_must_still_prove_effective_rules_but_cannot_read_bypass_list(self):
        config = ruleset()
        del config["bypass_actors"]
        with self.assertRaisesRegex(ValueError, "active exact-ref"):
            self.check(config=config)
        self.assertEqual(123, self.check(config=config, require_bypass_visibility=False))
        config["bypass_actors"] = [{"actor_id": 1}]
        with self.assertRaises(ValueError):
            self.check(config=config, require_bypass_visibility=False)
        del config["bypass_actors"]
        with self.assertRaises(ValueError):
            self.check(config=config, effective=[], require_bypass_visibility=False)

    def test_requires_live_rules_and_active_exact_ref_ruleset(self):
        self.assertEqual(123, self.check())
        cases = []
        for key, value in (("enforcement", "disabled"), ("target", "tag"),
                           ("bypass_actors", [{"actor_id": 1}]), ("rules", [])):
            cases.append(dict(ruleset(), **{key: value}))
        for refs in ({"include": ["refs/heads/*"], "exclude": []},
                     {"include": ["refs/heads/" + ENTRY["policy_ref"]], "exclude": ["~ALL"]}):
            cases.append(dict(ruleset(), conditions={"ref_name": refs}))
        for config in cases:
            with self.subTest(config=config), self.assertRaisesRegex(ValueError, "active exact-ref"):
                self.check(config=config)
        for effective in ([], [{"type": "update", "ruleset_id": 123}]):
            with self.subTest(effective=effective), self.assertRaises(ValueError):
                self.check(effective=effective)
        with self.assertRaisesRegex(ValueError, "ref moved"):
            self.check(sha="9" * 40)


class AcceptanceTest(unittest.TestCase):
    def test_registration_has_no_consumer_patches_and_adoption_checks_before_reading_worktrees(self):
        registered = {"schema": 1, "qualifications": [ENTRY]}
        with patch.object(promotion, "require_accepted_record", side_effect=ValueError("acceptance missing")) as gate:
            self.assertEqual({}, promotion.consumer_patches("registration", None, None, ENTRY, registered))
            gate.assert_not_called()
            with self.assertRaisesRegex(ValueError, "Merge registration"):
                promotion.consumer_patches("adoption", None, None, ENTRY, {"qualifications": []})
            gate.assert_not_called()
            with self.assertRaisesRegex(ValueError, "acceptance missing"):
                promotion.consumer_patches("adoption", None, None, ENTRY, registered)
            gate.assert_called_once_with(None, ENTRY)

    def test_metadata_requires_accepted_matching_tuple_and_completed_distinct_runs(self):
        self.assertEqual(record(), gates.validate_acceptance(markdown(record()), ENTRY))
        cases = []
        for key, value in (("decision", "blocked"), ("tuple", dict(ENTRY, core_sha="9" * 40)),
                           ("registration_sha", "master"), ("refresh_protocol", 2), ("schema", True),
                           ("forced_renewal", None)):
            cases.append(dict(record(), **{key: value}))
        for key, value in (("conclusion", "failure"), ("artifact_sha256", "missing"), ("producer_run_id", 0)):
            data = record()
            data["forced_renewal"][key] = value
            cases.append(data)
        for key, value in (("conclusion", "failure"), ("dispatched", 1), ("coordinator_run_id", 10)):
            data = record()
            data["fresh_proof_reuse"][key] = value
            cases.append(data)
        for data in cases:
            with self.subTest(data=data), self.assertRaises(ValueError):
                gates.validate_acceptance(markdown(data), ENTRY)
        for text in ("accepted", markdown(record()) * 2):
            with self.assertRaises(ValueError):
                gates.validate_acceptance(text, ENTRY)

    def test_ruleset_observation_is_optional_and_not_an_authorization_field(self):
        data = record()
        del data["observed_ruleset_id"]
        self.assertEqual(data, gates.validate_acceptance(markdown(data), ENTRY))
        data["observed_ruleset_id"] = "historical observation only"
        self.assertEqual(data, gates.validate_acceptance(markdown(data), ENTRY))

    def test_only_a_committed_fetched_master_record_is_accepted(self):
        with tempfile.TemporaryDirectory() as directory:
            def git(*args):
                return subprocess.check_output(["git", "-C", directory, *args], text=True).strip()
            git("init", "--quiet")
            git("config", "user.name", "Fixture")
            git("config", "user.email", "fixture@example.invalid")
            git("commit", "--quiet", "--allow-empty", "-m", "before registration")
            before_registration = git("rev-parse", "HEAD")
            registry = Path(directory) / "scripts/jackson3-promoted-qualifications.json"
            registry.parent.mkdir(parents=True)
            registry.write_text(json.dumps({"schema": 1, "qualifications": [ENTRY]}))
            git("add", ".")
            git("commit", "--quiet", "-m", "registration")
            registration = git("rev-parse", "HEAD")
            git("update-ref", "refs/remotes/origin/master", registration)
            data = record()
            data["registration_sha"] = registration
            path = Path(directory) / gates.acceptance_path(ENTRY)
            path.parent.mkdir(parents=True)
            path.write_text(markdown(data))
            with self.assertRaisesRegex(ValueError, "Merge reviewed acceptance"):
                gates.require_accepted_record(directory, ENTRY)
            git("add", ".")
            git("commit", "--quiet", "-m", "reviewed acceptance")
            # A committed PR branch alone is still insufficient.
            with self.assertRaisesRegex(ValueError, "Merge reviewed acceptance"):
                gates.require_accepted_record(directory, ENTRY)
            git("update-ref", "refs/remotes/origin/master", git("rev-parse", "HEAD"))
            with patch.object(gates, "verify_acceptance_facts") as facts:
                self.assertEqual(data, gates.require_accepted_record(directory, ENTRY, github=lambda _: None))
                facts.assert_called_once()
            with self.assertRaisesRegex(ValueError, "does not contain the tuple"):
                gates.require_registry_at(directory, before_registration, ENTRY)
            with self.assertRaisesRegex(ValueError, "does not contain the tuple"):
                gates.require_registry_at(directory, registration, dict(ENTRY, core_sha="9" * 40))


class AcceptanceFactsTest(unittest.TestCase):
    def setUp(self):
        self.data = record()
        def run(run_id, workflow, **kwargs):
            return dict(id=run_id, run_attempt=1, status="completed", conclusion="success",
                        event="workflow_dispatch", path=".github/workflows/" + workflow, **kwargs)
        coordinator = "jackson3-qualification-refresh.yml"
        self.responses = {
            "actions/runs/10/attempts/1": run(10, coordinator, head_branch="master", head_sha="e" * 40),
            "actions/runs/13/attempts/1": run(13, coordinator, head_branch="master", head_sha="e" * 40),
            "actions/runs/11/attempts/1": run(11, "jackson3-source-build.yml", head_sha=ENTRY["policy_sha"],
                                             actor={"login": "github-actions[bot]"}),
            "actions/artifacts/12": {"id": 12, "expired": False, "workflow_run": {"id": 11},
                                     "name": "core-source-qualification-attempt-1", "digest": "sha256:" + "f" * 64},
            "actions/runs/10/attempts/1/jobs?per_page=100": {"jobs": [{"id": 100, "name": "refresh", "conclusion": "success"}]},
            "actions/runs/13/attempts/1/jobs?per_page=100": {"jobs": [{"id": 130, "name": "refresh", "conclusion": "success"}]},
        }
        self.logs = {
            100: "Dispatched qualification run: 11\nVerified renewed proof: " + ENTRY["core_sha"]
                 + " run 11\nVerified forced registered renewal of 1 qualifications",
            130: "Healthy retained proof; no reactor dispatched: " + ENTRY["core_sha"]
                 + "\nVerified renewal of 0 qualifications; healthy proofs did not trigger builds",
        }

    def verify(self):
        with patch.object(gates, "require_master_ancestor") as ancestor, patch.object(gates, "require_registry_at") as registry:
            github = Mock(side_effect=self.responses.__getitem__)
            gates.verify_acceptance_facts("checkout", ENTRY, self.data, github, self.logs.__getitem__)
            return github, ancestor, registry

    def test_fetches_exact_attempts_artifact_digest_and_both_coordinator_logs(self):
        github, ancestor, registry = self.verify()
        self.assertEqual(6, github.call_count)
        self.assertEqual(2, ancestor.call_count)
        self.assertEqual(2, registry.call_count)
        self.assertTrue(all("/attempts/1" in call.args[0] or "artifacts/12" in call.args[0]
                            for call in github.call_args_list))

    def test_changed_run_identity_artifact_or_log_claim_is_rejected(self):
        mutations = [
            ("actions/runs/11/attempts/1", "head_sha", "9" * 40),
            ("actions/runs/11/attempts/1", "conclusion", "failure"),
            ("actions/runs/11/attempts/1", "run_attempt", 2),
            ("actions/runs/10/attempts/1", "head_branch", "feature"),
            ("actions/runs/13/attempts/1", "path", "other.yml"),
            ("actions/artifacts/12", "id", 99),
            ("actions/artifacts/12", "digest", "sha256:" + "9" * 64),
            ("actions/artifacts/12", "expired", True),
            ("actions/artifacts/12", "workflow_run", {"id": 99}),
            ("actions/artifacts/12", "name", "core-source-qualification-attempt-2"),
        ]
        for endpoint, key, value in mutations:
            with self.subTest(endpoint=endpoint, key=key):
                self.setUp()
                self.responses[endpoint][key] = value
                with self.assertRaises(ValueError):
                    self.verify()
        self.setUp()
        self.logs[100] = "Verified forced registered renewal of 1 qualifications"
        with self.assertRaisesRegex(ValueError, "forced-renewal log"):
            self.verify()
        self.setUp()
        self.logs[130] += "\nDispatched qualification run: 99"
        with self.assertRaisesRegex(ValueError, "zero dispatches"):
            self.verify()
        self.setUp()
        self.logs[100] = self.logs[100].replace("run 11", "run 110").replace("run: 11", "run: 110")
        with self.assertRaisesRegex(ValueError, "forced-renewal log"):
            self.verify()
