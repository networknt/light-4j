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

from jackson3_core_qualification import GitHub

PREFIX = "repos/networknt/light-4j/"
WORKFLOW = "jackson3-source-build.yml"
SHA = re.compile("[0-9a-f]{40}")


def policy_ref(sha):
    return "jackson3-qualification-policy-" + sha


def validate_registry(registry):
    if (not isinstance(registry, dict) or set(registry) != {"schema", "qualifications"}
            or type(registry["schema"]) is not int or registry["schema"] != 1):
        raise ValueError("Invalid promoted-qualification registry")
    entries = registry["qualifications"]
    if not isinstance(entries, list):
        raise ValueError("Qualification registry must contain a list")
    seen = set()
    for entry in entries:
        if not isinstance(entry, dict) or set(entry) != {"policy_sha", "core_sha", "policy_ref", "source_refs"}:
            raise ValueError("Invalid registered qualification")
        for key in ("policy_sha", "core_sha"):
            if not isinstance(entry[key], str) or not SHA.fullmatch(entry[key]):
                raise ValueError("Registered qualifications require full SHAs")
        if entry["policy_ref"] != policy_ref(entry["policy_sha"]):
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


def api(endpoint):
    return json.loads(subprocess.check_output(["gh", "api", PREFIX + endpoint], text=True))


def dispatch(payload):
    subprocess.run(["gh", "api", "--method", "POST", PREFIX + "actions/workflows/" + WORKFLOW + "/dispatches",
                    "--input", "-"], input=json.dumps(payload), text=True, check=True, capture_output=True)


RENEWAL_MARGIN = timedelta(days=30)
WAIT_SECONDS = 80 * 60


class ProofHealth:
    """Use each registered policy's verifier/inventory, never moving master's."""

    def __init__(self, directory):
        self.directory = Path(directory)
        self.checkouts = {}

    def __call__(self, entry):
        policy = entry["policy_sha"]
        if policy not in self.checkouts:
            checkout = self.directory / policy
            subprocess.run(["git", "clone", "--quiet", "--shared", "--no-checkout",
                            str(Path(__file__).resolve().parents[1]), str(checkout)], check=True)
            subprocess.run(["git", "-C", str(checkout), "checkout", "--quiet", "--detach", policy], check=True)
            self.checkouts[policy] = checkout
        checkout = self.checkouts[policy]
        report = self.directory / (policy + "-" + entry["core_sha"] + ".json")
        result = subprocess.run([
            sys.executable, str(checkout / "scripts/jackson3_core_qualification.py"),
            "--source-sha", entry["core_sha"], "--trusted-revision", policy,
            "--overrides", json.dumps(entry["source_refs"]), "--report", str(report)],
            capture_output=True, text=True)
        if result.returncode:
            raise ValueError(result.stderr.strip() or result.stdout.strip())
        proof = json.loads(report.read_text())["qualification"]
        artifact = GitHub().api("actions/artifacts/" + str(proof["artifact_id"]))
        if artifact.get("expired"):
            raise ValueError("Verified proof has expired")
        return datetime.fromisoformat(artifact["expires_at"].replace("Z", "+00:00"))


def refresh(registry, github=api, submit=dispatch, health=None,
            now=lambda: datetime.now(timezone.utc), clock=time.monotonic, sleep=time.sleep,
            wait_seconds=WAIT_SECONDS):
    entries = validate_registry(registry)
    eligible, failures = [], []
    for entry in entries:
        try:
            for sha in (entry["policy_sha"], entry["core_sha"]):
                if github("compare/" + sha + "...master")["status"] not in {"ahead", "identical"}:
                    raise ValueError("Registered qualification is not on master: " + sha)
            if github("commits/" + entry["policy_ref"])["sha"] != entry["policy_sha"]:
                raise ValueError("Pinned policy ref moved; restore it instead of changing consumer pins")
            eligible.append(entry)
        except (ValueError, KeyError, subprocess.CalledProcessError) as error:
            failures.append(entry["core_sha"] + ": " + str(error))
    if eligible and health is None:
        raise ValueError("A pinned-policy proof health checker is required")
    queued = 0
    pending = []
    for entry in eligible:
        try:
            try:
                if health(entry) > now() + RENEWAL_MARGIN:
                    print("Healthy retained proof; no reactor dispatched:", entry["core_sha"], flush=True)
                    continue
            except ValueError as error:
                print("Proof needs renewal:", entry["core_sha"], str(error), flush=True)
            submit({"ref": entry["policy_ref"], "inputs": {
                "source_sha": entry["core_sha"],
                "source_refs": json.dumps(entry["source_refs"], sort_keys=True, separators=(",", ":"))}})
            queued += 1
            pending.append(entry)
        except subprocess.CalledProcessError as error:
            failures.append(entry["core_sha"] + ": dispatch failed: " + str(error))
    # Queue all due pairs first, then monitor together under one bounded deadline.
    # Dispatch is never reported as successful renewal. A failed producer remains
    # a red coordinator even when the previous proof is still usable by consumers.
    deadline = clock() + wait_seconds
    reasons = {}
    while pending:
        for entry in pending[:]:
            try:
                if health(entry) > now() + RENEWAL_MARGIN:
                    pending.remove(entry)
                    print("Verified renewed proof:", entry["core_sha"], flush=True)
                else:
                    reasons[entry["core_sha"]] = "replacement proof still has 30 days or less remaining"
            except (ValueError, KeyError, subprocess.CalledProcessError) as error:
                reasons[entry["core_sha"]] = str(error)
        if not pending:
            break
        if clock() >= deadline:
            failures.extend(entry["core_sha"] + ": renewal timed out; inspect qualify/record runs: "
                            + reasons.get(entry["core_sha"], "no replacement proof") for entry in pending)
            break
        sleep(min(60, max(0, deadline - clock())))
    if failures:
        raise ValueError("Queued " + str(queued) + " refreshes; failed pairs: " + "; ".join(failures))
    return queued


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--registry", type=Path, default=Path(__file__).with_name("jackson3-promoted-qualifications.json"))
    args = parser.parse_args()
    with tempfile.TemporaryDirectory(prefix="jackson3-refresh-") as directory:
        queued = refresh(json.loads(args.registry.read_text()), health=ProofHealth(directory))
    print("Verified renewal of", queued, "qualifications; healthy proofs did not trigger builds")


if __name__ == "__main__":
    main()
