#!/usr/bin/env python3
"""Run only after the full reactor command succeeds; record its actual checkout."""
import json
import os
from pathlib import Path
import subprocess
import xml.etree.ElementTree as ET


def main():
    root = Path(__file__).resolve().parents[1]
    if subprocess.check_output(["git", "-C", str(root), "status", "--porcelain"], text=True).strip():
        raise SystemExit("Cannot qualify a modified core checkout")
    totals = {key: 0 for key in ("tests", "failures", "errors", "skipped")}
    for path in root.glob("*/target/surefire-reports/TEST-*.xml"):
        report = ET.parse(path).getroot()
        for key in totals:
            totals[key] += int(report.get(key, 0))
    if not totals["tests"] or totals["failures"] or totals["errors"]:
        raise SystemExit("Cannot record unsuccessful or empty full-reactor qualification")
    evidence = {
        "schema": 1, "repository": os.environ["GITHUB_REPOSITORY"],
        "source_sha": subprocess.check_output(["git", "-C", str(root), "rev-parse", "HEAD"], text=True).strip(),
        "run_id": int(os.environ["GITHUB_RUN_ID"]), "run_attempt": int(os.environ["GITHUB_RUN_ATTEMPT"]),
        "event": os.environ["GITHUB_EVENT_NAME"], "workflow": ".github/workflows/jackson3-source-build.yml",
        "suite": "full-reactor", "tests": totals,
        "replay_input": json.loads(os.environ.get("SOURCE_REFS", "{}")),
        "source_refs": json.loads(Path("source-refs.json").read_text()),
    }
    Path("core-source-qualification.json").write_text(json.dumps(evidence, indent=2) + "\n")


if __name__ == "__main__":
    main()
