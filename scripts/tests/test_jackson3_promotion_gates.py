import copy
import json
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest
from unittest.mock import patch

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
            "registration_sha": "e" * 40, "ruleset_id": 123,
            "forced_renewal": {"coordinator_run_id": 10, "producer_run_id": 11,
                               "conclusion": "success", "artifact_id": 12, "artifact_sha256": "f" * 64},
            "fresh_proof_reuse": {"coordinator_run_id": 13, "conclusion": "success", "dispatched": 0}}


def markdown(data):
    return "# Acceptance\n\n```json\n" + json.dumps(data) + "\n```\n"


class ProtectionTest(unittest.TestCase):
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
            self.assertEqual({}, promotion.consumer_patches("registration", None, None, ENTRY, registered, 123))
            gate.assert_not_called()
            with self.assertRaisesRegex(ValueError, "Merge registration"):
                promotion.consumer_patches("adoption", None, None, ENTRY, {"qualifications": []}, 123)
            gate.assert_not_called()
            with self.assertRaisesRegex(ValueError, "acceptance missing"):
                promotion.consumer_patches("adoption", None, None, ENTRY, registered, 123)
            gate.assert_called_once_with(None, ENTRY, 123)

    def test_metadata_requires_accepted_matching_tuple_and_completed_distinct_runs(self):
        self.assertEqual(record(), gates.validate_acceptance(markdown(record()), ENTRY))
        cases = []
        for key, value in (("decision", "blocked"), ("tuple", dict(ENTRY, core_sha="9" * 40)),
                           ("registration_sha", "master"), ("ruleset_id", True), ("schema", True),
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

    def test_only_a_committed_fetched_master_record_is_accepted(self):
        with tempfile.TemporaryDirectory() as directory:
            def git(*args):
                return subprocess.check_output(["git", "-C", directory, *args], text=True).strip()
            git("init", "--quiet")
            git("config", "user.name", "Fixture")
            git("config", "user.email", "fixture@example.invalid")
            git("commit", "--quiet", "--allow-empty", "-m", "registration")
            registration = git("rev-parse", "HEAD")
            git("update-ref", "refs/remotes/origin/master", registration)
            data = record()
            data["registration_sha"] = registration
            path = Path(directory) / gates.acceptance_path(ENTRY)
            path.parent.mkdir(parents=True)
            path.write_text(markdown(data))
            with self.assertRaisesRegex(ValueError, "Merge reviewed acceptance"):
                gates.require_accepted_record(directory, ENTRY, 123)
            git("add", ".")
            git("commit", "--quiet", "-m", "reviewed acceptance")
            # A committed PR branch alone is still insufficient.
            with self.assertRaisesRegex(ValueError, "Merge reviewed acceptance"):
                gates.require_accepted_record(directory, ENTRY, 123)
            git("update-ref", "refs/remotes/origin/master", git("rev-parse", "HEAD"))
            self.assertEqual(data, gates.require_accepted_record(directory, ENTRY, 123))
            with self.assertRaisesRegex(ValueError, "ruleset differs"):
                gates.require_accepted_record(directory, ENTRY, 456)
