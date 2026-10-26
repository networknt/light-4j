#!/usr/bin/env python3
"""Trusted recorder: candidate checkout and policy checkout are separate."""
import argparse
import json
import os
from pathlib import Path
import subprocess
from jackson3_test_inventory import collect_reports, load_inventory


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--source", type=Path, required=True)
    parser.add_argument("--refs", type=Path, required=True)
    parser.add_argument("--report", type=Path, required=True)
    args = parser.parse_args()
    root = Path(__file__).resolve().parents[1]
    if subprocess.check_output(["git", "-C", str(root), "status", "--porcelain"], text=True).strip():
        raise SystemExit("Cannot qualify a modified core checkout")
    policy_sha = subprocess.check_output(["git", "-C", str(root), "rev-parse", "HEAD"], text=True).strip()
    if os.environ["GITHUB_EVENT_NAME"] != "workflow_dispatch" or policy_sha != os.environ["GITHUB_SHA"]:
        raise SystemExit("Reusable evidence requires the independently pinned dispatch workflow")
    if subprocess.check_output(["git", "-C", str(args.source), "status", "--porcelain"], text=True).strip():
        raise SystemExit("Cannot qualify a modified candidate checkout")
    modules, totals = collect_reports(args.source.resolve(), load_inventory())
    evidence = {
        "schema": 2, "repository": os.environ["GITHUB_REPOSITORY"],
        "source_sha": subprocess.check_output(["git", "-C", str(args.source), "rev-parse", "HEAD"], text=True).strip(),
        "policy_sha": policy_sha,
        "run_id": int(os.environ["GITHUB_RUN_ID"]), "run_attempt": int(os.environ["GITHUB_RUN_ATTEMPT"]),
        "event": os.environ["GITHUB_EVENT_NAME"], "workflow": ".github/workflows/jackson3-source-build.yml",
        "suite": "full-reactor", "tests": totals, "modules": modules,
        "source_refs": json.loads(args.refs.read_text()),
    }
    args.report.write_text(json.dumps(evidence, indent=2) + "\n")


if __name__ == "__main__":
    main()
