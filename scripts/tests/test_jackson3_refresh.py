import json
from datetime import datetime, timezone, timedelta
from pathlib import Path
import sys
import subprocess
import tempfile
import unittest
from unittest.mock import Mock, patch

sys.path.insert(0, str(Path(__file__).parents[1]))
import refresh_jackson3_qualifications as refresh
import prepare_jackson3_consumer_promotion as promotion

NOW = datetime(2026, 10, 26, tzinfo=timezone.utc)
HEALTHY = NOW + timedelta(days=90)
DUE = NOW + timedelta(days=30)
POLICY, CORE = "a" * 40, "b" * 40
REFS = {"yaml-rule": "c" * 40, "http-client": "d" * 40}


def entry():
    return {"policy_sha": POLICY, "core_sha": CORE, "policy_ref": refresh.policy_ref(POLICY), "source_refs": REFS}


def run(run_id=1, status="completed", conclusion="success", **changes):
    result = dict(id=run_id, head_sha=POLICY, event="workflow_dispatch",
                  path=".github/workflows/" + refresh.WORKFLOW, status=status, conclusion=conclusion)
    result.update(changes)
    return result


class Clock:
    def __init__(self):
        self.value = 0
        self.sleeps = []

    def __call__(self):
        return self.value

    def sleep(self, seconds):
        self.sleeps.append(seconds)
        self.value += seconds


class RefreshTest(unittest.TestCase):
    def setUp(self):
        self.clock = Clock()
        self.status = Mock(return_value=run())
        self.submit = Mock(return_value=1)
        self.health = Mock(side_effect=[DUE, HEALTHY])
        self.calls = []

    def github(self, endpoint, timeout):
        self.calls.append((endpoint, timeout))
        if endpoint.startswith("compare/"):
            return {"status": "ahead"}
        if endpoint.startswith("commits/"):
            return {"sha": POLICY}
        if endpoint.startswith("actions/runs/"):
            return self.status(int(endpoint.rsplit("/", 1)[1]))
        self.fail("Unexpected API call: " + endpoint)

    def refresh(self, entries=None, **kwargs):
        return refresh.refresh({"schema": 1, "qualifications": [entry()] if entries is None else entries},
                               self.github, self.submit, health=self.health, now=lambda: NOW,
                               clock=self.clock, sleep=self.clock.sleep, **kwargs)

    def test_empty_registry_is_an_explicit_pre_promotion_noop(self):
        self.assertEqual(0, self.refresh([]))
        self.assertEqual([], self.calls)
        self.health.assert_not_called()

    def test_healthy_proof_does_not_dispatch(self):
        self.health = Mock(return_value=HEALTHY)
        self.assertEqual(0, self.refresh())
        self.submit.assert_not_called()
        self.status.assert_not_called()
        self.assertEqual(1, self.health.call_count)

    def test_explicit_smoke_forces_dispatch_but_uses_real_monitor_and_final_verifier(self):
        candidate = dict(entry(), policy_ref="fix-jackson3-mapper-policy")
        self.health = Mock(return_value=HEALTHY)
        self.assertEqual(1, self.refresh([candidate], smoke=True))
        self.assertFalse(any(endpoint.startswith("compare/") for endpoint, _ in self.calls))
        self.assertEqual(1, self.submit.call_count)
        self.assertEqual("fix-jackson3-mapper-policy", self.submit.call_args.args[0]["ref"])
        self.assertEqual(1, self.health.call_count)
        self.assertEqual(1, self.health.call_args.kwargs["run_id"])
        self.assertEqual(1, self.status.call_count)
        with self.assertRaisesRegex(ValueError, "immutable registered policy"):
            self.refresh([candidate])

    def test_smoke_requires_one_explicit_tuple_and_still_checks_policy_ref(self):
        for entries in ([], [entry(), dict(entry(), core_sha="e" * 40)]):
            with self.subTest(entries=entries), self.assertRaisesRegex(ValueError, "exactly one"):
                self.refresh(entries, smoke=True)
        with patch.object(self, "github", return_value={"sha": "e" * 40}):
            with self.assertRaisesRegex(ValueError, "Pinned policy ref moved"):
                self.refresh(smoke=True)
        self.submit.assert_not_called()

    def test_smoke_cli_refuses_the_real_registry_before_any_dispatch(self):
        script = Path(__file__).parents[1] / "refresh_jackson3_qualifications.py"
        registry = script.with_name("jackson3-promoted-qualifications.json")
        result = subprocess.run([sys.executable, str(script), "--smoke-tuple", str(registry)],
                                capture_output=True, text=True)
        self.assertEqual(2, result.returncode)
        self.assertIn("separate from the promotion registry", result.stderr)

    def test_only_status_is_polled_until_the_created_run_completes(self):
        self.status.side_effect = [run(status="queued"), run(status="in_progress"), run()]
        self.assertEqual(1, self.refresh())
        self.assertEqual([60, 60], self.clock.sleeps)
        self.assertEqual(3, self.status.call_count)
        self.assertEqual(2, self.health.call_count)
        self.assertNotIn("run_id", self.health.call_args_list[0].kwargs)
        self.assertEqual(1, self.health.call_args_list[1].kwargs["run_id"])

    def test_refresh_preserves_all_pins_and_qualified_prerequisites(self):
        self.assertEqual(1, self.refresh())
        payload = self.submit.call_args.args[0]
        self.assertEqual(refresh.policy_ref(POLICY), payload["ref"])
        self.assertEqual(CORE, payload["inputs"]["source_sha"])
        self.assertEqual(REFS, json.loads(payload["inputs"]["source_refs"]))

    def test_failed_or_cancelled_run_fails_immediately_without_rechecking_old_proof(self):
        for conclusion in ("failure", "cancelled", "timed_out"):
            with self.subTest(conclusion=conclusion):
                self.setUp()
                self.status.return_value = run(conclusion=conclusion)
                with self.assertRaisesRegex(ValueError, "ended " + conclusion):
                    self.refresh()
                self.assertEqual(1, self.health.call_count)
                self.assertEqual([], self.clock.sleeps)

    def test_api_rate_limit_is_reported_not_swallowed_as_a_retry(self):
        self.status.side_effect = subprocess.CalledProcessError(1, ["gh", "api"], stderr="rate limit")
        with self.assertRaisesRegex(ValueError, "failed pairs"):
            self.refresh()
        self.assertEqual(1, self.status.call_count)
        self.assertEqual(1, self.health.call_count)
        self.assertEqual([], self.clock.sleeps)

    def test_status_metadata_must_match_created_run_and_policy(self):
        for changes in ({"id": 9}, {"head_sha": "e" * 40}, {"event": "push"}, {"path": "other.yml"}):
            with self.subTest(changes=changes):
                self.setUp()
                self.status.return_value = run(**changes)
                with self.assertRaisesRegex(ValueError, "metadata does not match"):
                    self.refresh()
                self.assertEqual(1, self.health.call_count)

    def test_completed_run_with_missing_or_invalid_proof_fails_without_retry(self):
        self.health.side_effect = [DUE, ValueError("missing new proof")]
        with self.assertRaisesRegex(ValueError, "missing new proof"):
            self.refresh()
        self.assertEqual(2, self.health.call_count)
        self.assertEqual([], self.clock.sleeps)

    def test_setup_and_queueing_are_inside_the_absolute_deadline(self):
        def health(*args, **kwargs):
            self.clock.value += 100
            return DUE
        self.health = Mock(side_effect=health)
        self.status.return_value = run(status="queued")
        with self.assertRaisesRegex(ValueError, "including setup, queueing and verification"):
            self.refresh(wait_seconds=150)
        self.assertEqual([50], self.clock.sleeps)
        self.assertEqual(150, self.clock.value)
        self.assertTrue(all(0 < timeout <= 30 for _, timeout in self.calls))

    def test_final_verification_cannot_finish_past_the_deadline(self):
        def health(*args, **kwargs):
            if kwargs.get("run_id"):
                self.clock.value += 151
                return HEALTHY
            return DUE
        self.health = Mock(side_effect=health)
        with self.assertRaisesRegex(ValueError, "renewal timed out"):
            self.refresh(wait_seconds=150)

    def test_initial_healthy_proof_cannot_finish_past_the_deadline(self):
        def health(*args, **kwargs):
            self.clock.value = 151
            return HEALTHY
        self.health = Mock(side_effect=health)
        with self.assertRaisesRegex(ValueError, "renewal timed out"):
            self.refresh(wait_seconds=150)
        self.submit.assert_not_called()

    def test_missing_dispatch_run_id_fails_closed(self):
        self.submit.return_value = None
        with self.assertRaisesRegex(ValueError, "no run ID"):
            self.refresh()
        self.status.assert_not_called()

    def test_all_due_pairs_are_dispatched_before_status_polling(self):
        other = dict(entry(), core_sha="e" * 40)
        events = []
        def health(pair, **kwargs):
            events.append("verify" if kwargs.get("run_id") else "initial")
            return HEALTHY if kwargs.get("run_id") else DUE
        def submit(payload, **kwargs):
            events.append("dispatch")
            return events.count("dispatch")
        self.health = Mock(side_effect=health)
        self.submit = Mock(side_effect=submit)
        self.status.side_effect = lambda run_id: run(run_id)
        self.assertEqual(2, self.refresh([entry(), other]))
        self.assertEqual(["initial", "dispatch", "initial", "dispatch", "verify", "verify"], events)

    def test_dispatch_failure_does_not_block_other_due_pairs(self):
        other = dict(entry(), core_sha="e" * 40)
        self.submit.side_effect = [subprocess.CalledProcessError(1, ["gh", "api"]), 1]
        self.health.side_effect = [DUE, DUE, HEALTHY]
        with self.assertRaisesRegex(ValueError, "Queued 1 refreshes"):
            self.refresh([entry(), other])
        self.assertEqual(2, self.submit.call_count)
        self.assertEqual(1, self.status.call_count)

    def test_unmerged_or_moved_policy_cannot_be_dispatched(self):
        for response in ({"status": "behind"}, {"status": "diverged"}):
            with self.subTest(response=response), self.assertRaises(ValueError):
                refresh.refresh({"schema": 1, "qualifications": [entry()]},
                                Mock(return_value=response), self.submit, health=self.health)
        self.submit.assert_not_called()

    def test_dispatch_requests_and_returns_the_created_run_id(self):
        with patch.object(refresh.subprocess, "check_output", return_value='{"workflow_run_id":123}') as request:
            self.assertEqual(123, refresh.dispatch({"ref": "fixed", "inputs": {}}, timeout=12))
        payload = json.loads(request.call_args.kwargs["input"])
        self.assertTrue(payload["return_run_details"])
        self.assertEqual(12, request.call_args.kwargs["timeout"])

    def test_dispatch_without_run_details_does_not_guess_a_run(self):
        for response in ("", "{}", '{"workflow_run_id":null}'):
            with self.subTest(response=response):
                with patch.object(refresh.subprocess, "check_output", return_value=response):
                    with self.assertRaisesRegex(ValueError, "inspect Actions"):
                        refresh.dispatch({"ref": "fixed", "inputs": {}})

    def test_proof_health_requires_the_created_run_and_bounds_subprocesses(self):
        with tempfile.TemporaryDirectory() as directory:
            health = refresh.ProofHealth(directory)
            health.checkouts[POLICY] = Path(directory) / "policy"
            def verify(*args, **kwargs):
                report = Path(directory) / (POLICY + "-" + CORE + ".json")
                report.write_text(json.dumps({"qualification": {"run_id": 2, "artifact_id": 10}}))
                return subprocess.CompletedProcess(args, 0, stdout="", stderr="")
            with patch.object(refresh.subprocess, "run", side_effect=verify) as command:
                with self.assertRaisesRegex(ValueError, "another run"):
                    health(entry(), run_id=1, timeout=12)
                self.assertLessEqual(command.call_args.kwargs["timeout"], 12)
                self.assertIn(str(health.checkouts[POLICY] / "scripts/jackson3_core_qualification.py"),
                              command.call_args.args[0])

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
