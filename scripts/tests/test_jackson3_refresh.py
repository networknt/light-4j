import json
from pathlib import Path
import sys
import subprocess
import unittest
from unittest.mock import Mock

sys.path.insert(0, str(Path(__file__).parents[1]))
import refresh_jackson3_qualifications as refresh
import prepare_jackson3_consumer_promotion as promotion

POLICY, CORE = "a" * 40, "b" * 40
REFS = {"yaml-rule": "c" * 40, "http-client": "d" * 40}


def entry():
    return {"policy_sha": POLICY, "core_sha": CORE, "policy_ref": refresh.policy_ref(POLICY), "source_refs": REFS}


class RefreshTest(unittest.TestCase):
    def test_empty_registry_is_an_explicit_pre_promotion_noop(self):
        github, submit = Mock(), Mock()
        self.assertEqual(0, refresh.refresh({"schema": 1, "qualifications": []}, github, submit))
        github.assert_not_called()
        submit.assert_not_called()

    def test_refresh_preserves_all_pins_and_qualified_prerequisites(self):
        github = Mock(side_effect=[{"status": "ahead"}, {"status": "identical"}, {"sha": POLICY}])
        submit = Mock()
        self.assertEqual(1, refresh.refresh({"schema": 1, "qualifications": [entry()]}, github, submit))
        payload = submit.call_args.args[0]
        self.assertEqual(refresh.policy_ref(POLICY), payload["ref"])
        self.assertEqual(CORE, payload["inputs"]["source_sha"])
        self.assertEqual(REFS, json.loads(payload["inputs"]["source_refs"]))
        self.assertEqual(2, sum("compare/" in call.args[0] for call in github.call_args_list))

    def test_unmerged_or_moved_policy_cannot_be_dispatched(self):
        for replies in ([{"status": "behind"}], [{"status": "ahead"}, {"status": "diverged"}],
                        [{"status": "ahead"}, {"status": "ahead"}, {"sha": "e" * 40}]):
            github, submit = Mock(side_effect=replies), Mock()
            with self.subTest(replies=replies), self.assertRaises(ValueError):
                refresh.refresh({"schema": 1, "qualifications": [entry()]}, github, submit)
            submit.assert_not_called()

    def test_bad_ref_does_not_stop_other_registered_pairs_from_refreshing(self):
        other = dict(entry(), core_sha="e" * 40)
        github = Mock(side_effect=[{"status": "ahead"}, {"status": "ahead"}, {"sha": POLICY},
                                   {"status": "behind"}])
        submit = Mock()
        with self.assertRaisesRegex(ValueError, "Queued 1 refreshes"):
            refresh.refresh({"schema": 1, "qualifications": [entry(), other]}, github, submit)
        self.assertEqual(1, submit.call_count)

    def test_failed_dispatch_is_reported_without_blocking_another_valid_pair(self):
        other = dict(entry(), core_sha="e" * 40)
        github = Mock(side_effect=[{"status": "ahead"}, {"status": "ahead"}, {"sha": POLICY}] * 2)
        submit = Mock(side_effect=[subprocess.CalledProcessError(1, ["gh", "api"]), None])
        with self.assertRaisesRegex(ValueError, "Queued 1 refreshes; failed pairs"):
            refresh.refresh({"schema": 1, "qualifications": [entry(), other]}, github, submit)
        self.assertEqual(2, submit.call_count)

    def test_registry_rejects_unknown_fields_moving_refs_and_missing_prerequisite_pins(self):
        for changes in ({"core_sha": "master"}, {"policy_ref": "master"}, {"source_refs": {}}, {"extra": True}):
            bad = dict(entry(), **changes)
            with self.subTest(changes=changes), self.assertRaises(ValueError):
                refresh.validate_registry({"schema": 1, "qualifications": [bad]})
        with self.assertRaisesRegex(ValueError, "Duplicate"):
            refresh.validate_registry({"schema": 1, "qualifications": [entry(), entry()]})

    def test_promotion_registers_verified_tuple_without_mutating_existing_pairs(self):
        registry = {"schema": 1, "qualifications": []}
        evidence = {"policy_sha": POLICY, "source_sha": CORE, "source_refs": REFS}
        promoted = promotion.registered_qualification(registry, POLICY, CORE, evidence)
        self.assertEqual([], registry["qualifications"])
        self.assertEqual([entry()], promoted["qualifications"])
        self.assertEqual(promoted, promotion.registered_qualification(promoted, POLICY, CORE, evidence))
        changed = dict(evidence, source_refs=dict(REFS, **{"yaml-rule": "e" * 40}))
        with self.assertRaisesRegex(ValueError, "different prerequisites"):
            promotion.registered_qualification(promoted, POLICY, CORE, changed)
        with self.assertRaisesRegex(ValueError, "does not match"):
            promotion.registered_qualification(registry, POLICY, "f" * 40, evidence)
