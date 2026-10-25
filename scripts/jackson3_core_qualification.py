#!/usr/bin/env python3
"""Require a successful full-core workflow for the exact checked-out commit."""
import argparse
import json
from pathlib import Path
import re
import subprocess


def qualify(runs, sha):
    matches = [run for run in runs if run.get("head_sha") == sha
               and run.get("path", "").split("@")[0] == ".github/workflows/jackson3-source-build.yml"]
    if not matches:
        raise ValueError("No full-core qualification run for " + sha)
    latest = max(matches, key=lambda run: (run["run_number"], run.get("run_attempt", 1)))
    if latest.get("status") != "completed" or latest.get("conclusion") != "success":
        raise ValueError("Latest full-core qualification is pending or unsuccessful: " + latest["html_url"])
    return {key: latest.get(key) for key in
            ("id", "head_sha", "path", "run_number", "run_attempt", "status", "conclusion", "html_url")}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--source", type=Path, required=True)
    parser.add_argument("--report", type=Path, required=True)
    args = parser.parse_args()
    sha = subprocess.check_output(["git", "-C", str(args.source), "rev-parse", "HEAD"], text=True).strip()
    if not re.fullmatch("[0-9a-f]{40}", sha):
        raise SystemExit("Invalid source commit")
    endpoint = "repos/networknt/light-4j/actions/workflows/jackson3-source-build.yml/runs?head_sha=" + sha + "&per_page=100"
    report = {"source_sha": sha}
    try:
        if subprocess.run(["git", "-C", str(args.source), "diff", "--quiet", "HEAD", "--"], check=False).returncode:
            raise ValueError("Core source differs from the recorded commit")
        response = json.loads(subprocess.check_output(["gh", "api", endpoint], text=True))
        report["qualification"] = qualify(response["workflow_runs"], sha)
    except (ValueError, KeyError, subprocess.CalledProcessError) as error:
        report["failure"] = str(error)
        args.report.write_text(json.dumps(report, indent=2) + "\n")
        raise SystemExit(str(error)) from error
    args.report.write_text(json.dumps(report, indent=2) + "\n")
    print("Qualified exact core source " + sha + ": " + report["qualification"]["html_url"])


if __name__ == "__main__":
    main()
