#!/usr/bin/env python3
"""Re-dispatch registered immutable qualifications; never promote or move a pin."""
import argparse
import json
from pathlib import Path
import re
import subprocess
import sys
import tempfile
import time
from datetime import datetime, timezone, timedelta


PREFIX = "repos/networknt/light-4j/"
WORKFLOW = "jackson3-source-build.yml"
SHA = re.compile("[0-9a-f]{40}")


def policy_ref(sha):
    return "jackson3-qualification-policy-" + sha


def validate_registry(registry, smoke=False):
    if (not isinstance(registry, dict) or set(registry) != {"schema", "qualifications"}
            or type(registry["schema"]) is not int or registry["schema"] != 1):
        raise ValueError("Invalid promoted-qualification registry")
    entries = registry["qualifications"]
    if not isinstance(entries, list):
        raise ValueError("Qualification registry must contain a list")
    if smoke and len(entries) != 1:
        raise ValueError("Smoke input must contain exactly one throwaway tuple")
    seen = set()
    for entry in entries:
        if not isinstance(entry, dict) or set(entry) != {"policy_sha", "core_sha", "policy_ref", "source_refs"}:
            raise ValueError("Invalid registered qualification")
        for key in ("policy_sha", "core_sha"):
            if not isinstance(entry[key], str) or not SHA.fullmatch(entry[key]):
                raise ValueError("Registered qualifications require full SHAs")
        if smoke and (not isinstance(entry["policy_ref"], str)
                      or not re.fullmatch(r"[A-Za-z0-9_][A-Za-z0-9_./-]*", entry["policy_ref"])):
            raise ValueError("Smoke policy ref must be a literal branch/tag")
        if not smoke and entry["policy_ref"] != policy_ref(entry["policy_sha"]):
            raise ValueError("Policy ref must name the immutable registered policy")
        refs = entry["source_refs"]
        if not isinstance(refs, dict) or set(refs) != {"yaml-rule", "http-client"} or any(
                not isinstance(value, str) or not SHA.fullmatch(value) for value in refs.values()):
            raise ValueError("Registered prerequisites must be exact qualified SHAs")
        key = (entry["policy_sha"], entry["core_sha"])
        if key in seen:
            raise ValueError("Duplicate registered policy/core pair")
        seen.add(key)
    return entries


def api(endpoint, timeout=30):
    return json.loads(subprocess.check_output(["gh", "api", PREFIX + endpoint], text=True, timeout=timeout))


def dispatch(payload, timeout=30):
    response = subprocess.check_output(
        ["gh", "api", "--method", "POST", PREFIX + "actions/workflows/" + WORKFLOW + "/dispatches",
         "--input", "-"], input=json.dumps(dict(payload, return_run_details=True)),
        text=True, timeout=timeout)
    try:
        run_id = json.loads(response).get("workflow_run_id")
    except (ValueError, AttributeError) as error:
        raise ValueError("Dispatch returned no run ID; inspect Actions before dispatching again") from error
    if type(run_id) is not int or run_id <= 0:
        raise ValueError("Dispatch returned no run ID; inspect Actions before dispatching again")
    return run_id


RENEWAL_MARGIN = timedelta(days=30)
WAIT_SECONDS = 80 * 60


class ProofHealth:
    """Use each registered policy's verifier/inventory, never moving master's."""

    def __init__(self, directory):
        self.directory = Path(directory)
        self.checkouts = {}

    def __call__(self, entry, run_id=None, timeout=180):
        deadline = time.monotonic() + timeout

        def remaining():
            value = deadline - time.monotonic()
            if value <= 0:
                raise TimeoutError("Proof verification exhausted its time budget")
            return value

        policy = entry["policy_sha"]
        if policy not in self.checkouts:
            checkout = self.directory / policy
            subprocess.run(["git", "clone", "--quiet", "--shared", "--no-checkout",
                            str(Path(__file__).resolve().parents[1]), str(checkout)], check=True, timeout=min(30, remaining()))
            subprocess.run(["git", "-C", str(checkout), "checkout", "--quiet", "--detach", policy], check=True, timeout=min(30, remaining()))
            self.checkouts[policy] = checkout
        checkout = self.checkouts[policy]
        report = self.directory / (policy + "-" + entry["core_sha"] + ".json")
        result = subprocess.run([
            sys.executable, str(checkout / "scripts/jackson3_core_qualification.py"),
            "--source-sha", entry["core_sha"], "--trusted-revision", policy,
            "--overrides", json.dumps(entry["source_refs"]), "--report", str(report)],
            capture_output=True, text=True, timeout=remaining())
        if result.returncode:
            raise ValueError(result.stderr.strip() or result.stdout.strip())
        proof = json.loads(report.read_text())["qualification"]
        if run_id is not None and proof["run_id"] != run_id:
            raise ValueError("Verified proof belongs to another run, not dispatched run " + str(run_id))
        artifact = api("actions/artifacts/" + str(proof["artifact_id"]), timeout=min(30, remaining()))
        if artifact.get("expired"):
            raise ValueError("Verified proof has expired")
        return datetime.fromisoformat(artifact["expires_at"].replace("Z", "+00:00"))


def refresh(registry, github=None, submit=None, health=None,
            now=lambda: datetime.now(timezone.utc), clock=time.monotonic, sleep=time.sleep,
            wait_seconds=WAIT_SECONDS, smoke=False):
    # Includes validation, initial proof checks, dispatch/runner queueing, API work
    # and final verification, rather than granting an extra 80 minutes afterward.
    deadline = clock() + wait_seconds

    def remaining():
        value = deadline - clock()
        if value <= 0:
            raise TimeoutError("renewal timed out (including setup, queueing and verification)")
        return value

    def get(endpoint):
        return (github or api)(endpoint, timeout=min(30, remaining()))

    entries = validate_registry(registry, smoke=smoke)
    eligible, failures = [], []
    errors = (ValueError, KeyError, RuntimeError, subprocess.SubprocessError, TimeoutError)
    for entry in entries:
        try:
            if not smoke:
                for sha in (entry["policy_sha"], entry["core_sha"]):
                    if get("compare/" + sha + "...master")["status"] not in {"ahead", "identical"}:
                        raise ValueError("Registered qualification is not on master: " + sha)
            if get("commits/" + entry["policy_ref"])["sha"] != entry["policy_sha"]:
                raise ValueError("Pinned policy ref moved; restore it instead of changing consumer pins")
            eligible.append(entry)
        except errors as error:
            failures.append(entry["core_sha"] + ": " + str(error))
    if eligible and health is None:
        raise ValueError("A pinned-policy proof health checker is required")
    queued = 0
    pending = []
    for entry in eligible:
        try:
            if not smoke:
                try:
                    expiry = health(entry, timeout=min(180, remaining()))
                    remaining()
                    if expiry > now() + RENEWAL_MARGIN:
                        print("Healthy retained proof; no reactor dispatched:", entry["core_sha"], flush=True)
                        continue
                except ValueError as error:
                    print("Proof needs renewal:", entry["core_sha"], str(error), flush=True)
            run_id = (submit or dispatch)({"ref": entry["policy_ref"], "inputs": {
                "source_sha": entry["core_sha"],
                "source_refs": json.dumps(entry["source_refs"], sort_keys=True, separators=(",", ":"))}},
                timeout=min(30, remaining()))
            if type(run_id) is not int or run_id <= 0:
                raise ValueError("Dispatch returned no run ID; inspect Actions before retrying")
            queued += 1
            pending.append((entry, run_id))
            print("Dispatched qualification run:", run_id, flush=True)
        except errors as error:
            failures.append(entry["core_sha"] + ": dispatch/proof check failed: " + str(error))
    # One cheap status lookup per pending run per minute. Never repeatedly scan
    # workflow history/artifacts or download evidence while producers are running.
    while pending:
        for entry, run_id in pending[:]:
            try:
                run = get("actions/runs/" + str(run_id))
                if (run.get("id") != run_id or run.get("head_sha") != entry["policy_sha"]
                        or run.get("event") != "workflow_dispatch"
                        or run.get("path", "").split("@")[0] != ".github/workflows/" + WORKFLOW):
                    raise ValueError("Dispatched run metadata does not match registered policy")
                if run.get("status") != "completed":
                    continue
                if run.get("conclusion") != "success":
                    raise ValueError("Dispatched qualification " + str(run_id) + " ended "
                                     + str(run.get("conclusion")))
                # Exactly one full pinned-policy verification after completion.
                expiry = health(entry, run_id=run_id, timeout=min(180, remaining()))
                if expiry <= now() + RENEWAL_MARGIN:
                    raise ValueError("Replacement proof still has 30 days or less remaining")
                remaining()
                pending.remove((entry, run_id))
                print("Verified renewed proof:", entry["core_sha"], "run", run_id, flush=True)
            except errors as error:
                pending.remove((entry, run_id))
                failures.append(entry["core_sha"] + ": run " + str(run_id) + ": " + str(error))
        if pending:
            try:
                sleep(min(60, remaining()))
            except errors as error:
                failures.extend(entry["core_sha"] + ": " + str(error) for entry, _ in pending)
                pending.clear()
    if failures:
        raise ValueError("Queued " + str(queued) + " refreshes; failed pairs: " + "; ".join(failures))
    return queued


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    mode = parser.add_mutually_exclusive_group()
    mode.add_argument("--registry", type=Path)
    mode.add_argument("--smoke-tuple", type=Path,
                      help="Manually dispatch/monitor one throwaway tuple; never read/write the promotion registry")
    args = parser.parse_args()
    smoke = args.smoke_tuple is not None
    path = args.smoke_tuple or args.registry or Path(__file__).with_name("jackson3-promoted-qualifications.json")
    if smoke:
        if path.resolve() == Path(__file__).with_name("jackson3-promoted-qualifications.json").resolve():
            parser.error("Smoke input must be separate from the promotion registry")
        print("SMOKE ONLY: dispatch/poll/verify; no master approval, registration or consumer promotion", flush=True)
    with tempfile.TemporaryDirectory(prefix="jackson3-refresh-") as directory:
        queued = refresh(json.loads(path.read_text()), health=ProofHealth(directory), smoke=smoke)
    if smoke:
        print("SMOKE PASSED: verified", queued, "throwaway qualification; registry unchanged")
        return
    print("Verified renewal of", queued, "qualifications; healthy proofs did not trigger builds")


if __name__ == "__main__":
    main()
