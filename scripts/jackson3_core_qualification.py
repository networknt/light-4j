#!/usr/bin/env python3
"""Require retained successful evidence for the actual full-core source checkout."""
import argparse
import io
import json
from pathlib import Path
import re
import subprocess
import zipfile

WORKFLOW = ".github/workflows/jackson3-source-build.yml"
EVIDENCE = "core-source-qualification.json"
ARTIFACT_PREFIX = "core-source-qualification-attempt-"
REPOSITORY = "networknt/light-4j"
SHA = re.compile("[0-9a-f]{40}")


def validate_evidence(evidence, run, sha, source_refs):
    """Run metadata alone cannot establish which ref actions/checkout built."""
    expected = {"schema": 1, "repository": REPOSITORY, "source_sha": sha,
                "run_id": run["id"], "run_attempt": run["run_attempt"],
                "event": run["event"], "workflow": WORKFLOW, "suite": "full-reactor"}
    if any(evidence.get(key) != value for key, value in expected.items()):
        raise ValueError("Qualification evidence does not match the actual source/run/attempt")
    if evidence.get("replay_input") != {}:
        raise ValueError("Replay source overrides cannot qualify consumers")
    refs = evidence.get("source_refs")
    if not isinstance(refs, dict) or set(refs) != {"yaml-rule", "http-client"} or not all(
            isinstance(value, str) and SHA.fullmatch(value) for value in refs.values()):
        raise ValueError("Missing recorded prerequisite source commits")
    if refs != source_refs:
        raise ValueError("Qualified prerequisite commits differ from the consumer source checkouts")
    tests = evidence.get("tests", {})
    if tests.get("tests", 0) <= 0 or tests.get("failures") != 0 or tests.get("errors") != 0:
        raise ValueError("Missing successful reactor test evidence")
    return evidence


class GitHub:
    def api(self, endpoint, paginate=False):
        command = ["gh", "api"]
        if paginate:
            command += ["--paginate", "--slurp"]
        return json.loads(subprocess.check_output(command + ["repos/" + REPOSITORY + "/" + endpoint], text=True))

    def runs(self, sha):
        pages = self.api("actions/workflows/jackson3-source-build.yml/runs?head_sha=" + sha + "&per_page=100", True)
        return [run for page in pages for run in page["workflow_runs"]]

    def artifacts(self, run_id):
        pages = self.api(f"actions/runs/{run_id}/artifacts?per_page=100", True)
        return [artifact for page in pages for artifact in page["artifacts"]]

    def attempt(self, run_id, attempt):
        return self.api(f"actions/runs/{run_id}/attempts/{attempt}")

    def evidence(self, artifact):
        raw = subprocess.check_output(["gh", "api", f"repos/{REPOSITORY}/actions/artifacts/{artifact['id']}/zip"])
        with zipfile.ZipFile(io.BytesIO(raw)) as archive:
            info = archive.getinfo(EVIDENCE)
            if info.file_size > 100_000:
                raise ValueError("Qualification evidence is unexpectedly large")
            return json.loads(archive.read(info))


def qualify(client, sha, source_refs):
    runs = [run for run in client.runs(sha) if run.get("head_sha") == sha
            and run.get("path", "").split("@")[0] == WORKFLOW
            and run.get("event") in {"push", "pull_request"}]
    rejected = []
    for run in sorted(runs, key=lambda item: item["run_number"], reverse=True):
        # Attempt-specific immutable artifacts survive a later pending/cancelled rerun.
        artifacts = [artifact for artifact in client.artifacts(run["id"])
                     if not artifact.get("expired") and re.fullmatch(
                         re.escape(ARTIFACT_PREFIX) + r"[1-9][0-9]*", artifact["name"])]
        for artifact in sorted(artifacts, key=lambda item: int(item["name"][len(ARTIFACT_PREFIX):]), reverse=True):
            number = int(artifact["name"][len(ARTIFACT_PREFIX):])
            try:
                attempt = client.attempt(run["id"], number)
                if (attempt.get("status") != "completed" or attempt.get("conclusion") != "success"
                        or attempt.get("head_sha") != sha or attempt.get("event") != run["event"]
                        or attempt.get("id") != run["id"] or attempt.get("run_attempt") != number):
                    continue
                evidence = validate_evidence(client.evidence(artifact), attempt, sha, source_refs)
                return {"run_id": run["id"], "run_attempt": number, "head_sha": sha,
                        "html_url": attempt["html_url"], "artifact_id": artifact["id"], "evidence": evidence}
            except (ValueError, KeyError, zipfile.BadZipFile, subprocess.CalledProcessError) as error:
                rejected.append(str(error))
    raise ValueError("No retained successful exact-source qualification for " + sha
                     + (": " + "; ".join(rejected) if rejected else ""))


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--source", type=Path, required=True)
    parser.add_argument("--report", type=Path, required=True)
    parser.add_argument("--yaml-source", type=Path, required=True)
    parser.add_argument("--http-source", type=Path, required=True)
    args = parser.parse_args()
    sha = subprocess.check_output(["git", "-C", str(args.source), "rev-parse", "HEAD"], text=True).strip()
    report = {"source_sha": sha}
    try:
        if not SHA.fullmatch(sha):
            raise ValueError("Invalid source commit")
        if subprocess.check_output(["git", "-C", str(args.source), "status", "--porcelain"], text=True).strip():
            raise ValueError("Core source differs from the recorded commit")
        refs = {name: subprocess.check_output(["git", "-C", str(path), "rev-parse", "HEAD"], text=True).strip()
                for name, path in (("yaml-rule", args.yaml_source), ("http-client", args.http_source))}
        report["source_refs"] = refs
        report["qualification"] = qualify(GitHub(), sha, refs)
    except (ValueError, KeyError, subprocess.CalledProcessError) as error:
        report["failure"] = str(error)
        args.report.write_text(json.dumps(report, indent=2) + "\n")
        raise SystemExit(str(error)) from error
    args.report.write_text(json.dumps(report, indent=2) + "\n")
    print("Qualified actual core checkout " + sha + ": " + report["qualification"]["html_url"])


if __name__ == "__main__":
    main()
