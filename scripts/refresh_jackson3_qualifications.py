#!/usr/bin/env python3
"""Re-dispatch registered immutable qualifications; never promote or move a pin."""
import argparse
import json
from pathlib import Path
import re
import subprocess

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


def refresh(registry, github=api, submit=dispatch):
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
    queued = 0
    for entry in eligible:
        try:
            submit({"ref": entry["policy_ref"], "inputs": {
                "source_sha": entry["core_sha"],
                "source_refs": json.dumps(entry["source_refs"], sort_keys=True, separators=(",", ":"))}})
            queued += 1
        except subprocess.CalledProcessError as error:
            failures.append(entry["core_sha"] + ": dispatch failed: " + str(error))
    if failures:
        raise ValueError("Queued " + str(queued) + " refreshes; failed pairs: " + "; ".join(failures))
    return queued


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--registry", type=Path, default=Path(__file__).with_name("jackson3-promoted-qualifications.json"))
    args = parser.parse_args()
    queued = refresh(json.loads(args.registry.read_text()))
    print("Queued", queued, "qualification refreshes; dispatch success is not qualification success")


if __name__ == "__main__":
    main()
