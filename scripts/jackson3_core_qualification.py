#!/usr/bin/env python3
"""Require retained successful evidence for the actual full-core source checkout."""
import argparse
import io
import json
from pathlib import Path
import re
import subprocess
import zipfile
import os

from jackson3_test_inventory import InventoryMismatch, TestFloorError, load_inventory, validate_modules

WORKFLOW = ".github/workflows/jackson3-source-build.yml"
EVIDENCE = "core-source-qualification.json"
ARTIFACT_PREFIX = "core-source-qualification-attempt-"
REPOSITORY = "networknt/light-4j"
SHA = re.compile("[0-9a-f]{40}")


class PrerequisiteConflict(ValueError):
    pass


def rejection_action(error):
    if isinstance(error, InventoryMismatch):
        return ("Restore missing candidate reports/modules, or review intentional module changes and update the "
                "policy inventory. Promote a reviewed master policy commit and update consumer pins; "
                "rerunning the unchanged policy does not repair inventory differences.")
    if isinstance(error, TestFloorError):
        return ("Restore the expected executed tests in the candidate. An intentional test-corpus reduction "
                "requires a reviewed floor update, policy promotion and consumer pin updates; "
                "rerunning the unchanged candidate/policy does not repair the test floor.")
    if isinstance(error, PrerequisiteConflict):
        return "Qualify this candidate with the explicitly requested prerequisite SHAs, or remove the conflicting override."
    return "Inspect and repair the rejected evidence or its producer; a rerun alone is not an evidence repair."


def validate_evidence(evidence, run, sha, source_refs, trusted_revision):
    """Run metadata alone cannot establish which ref actions/checkout built."""
    expected = {"schema": 2, "repository": REPOSITORY, "source_sha": sha,
                "run_id": run["id"], "run_attempt": run["run_attempt"], "policy_sha": trusted_revision,
                "event": run["event"], "workflow": WORKFLOW, "suite": "full-reactor"}
    if any(evidence.get(key) != value for key, value in expected.items()):
        raise ValueError("Qualification evidence does not match the actual source/run/attempt")
    refs = evidence.get("source_refs")
    if not isinstance(refs, dict) or set(refs) != {"yaml-rule", "http-client"} or not all(
            isinstance(value, str) and SHA.fullmatch(value) for value in refs.values()):
        raise ValueError("Missing recorded prerequisite source commits")
    if any(refs.get(name) != value for name, value in source_refs.items()):
        raise PrerequisiteConflict("Explicit prerequisite commits conflict with qualified evidence")
    totals = validate_modules(evidence.get("modules", {}), load_inventory())
    if evidence.get("tests") != totals:
        raise ValueError("Reactor totals differ from required module reports")
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


def qualify(client, sha, source_refs, trusted_revision):
    if not SHA.fullmatch(trusted_revision):
        raise ValueError("An independently approved full-SHA qualification policy pin is required")
    runs = [run for run in client.runs(trusted_revision) if run.get("head_sha") == trusted_revision
            and run.get("path", "").split("@")[0] == WORKFLOW
            and run.get("event") == "workflow_dispatch"
            and run.get("head_repository", {}).get("full_name") == REPOSITORY]
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
                        or attempt.get("head_sha") != trusted_revision or attempt.get("event") != run["event"]
                        or attempt.get("id") != run["id"] or attempt.get("run_attempt") != number):
                    continue
                raw = client.evidence(artifact)
                if raw.get("source_sha") != sha:
                    continue
                evidence = validate_evidence(raw, attempt, sha, source_refs, trusted_revision)
                return {"run_id": run["id"], "run_attempt": number, "head_sha": sha,
                        "html_url": attempt["html_url"], "artifact_id": artifact["id"], "evidence": evidence}
            except (ValueError, KeyError, zipfile.BadZipFile, subprocess.CalledProcessError) as error:
                rejected.append(str(error) + ". " + rejection_action(error))
    if rejected:
        raise ValueError("Retained qualification evidence rejected for " + sha + ": " + "; ".join(rejected))
    raise ValueError("No retained successful trusted qualification for " + sha
                     + ": evidence is missing, expired, or has no successful attempt. Inspect failed runs first: "
                     "candidate/report failures need repair; inventory or floor changes need reviewed policy/pin updates. "
                     "Only for an absent run or expired evidence, rerun jackson3-source-build.yml at approved policy "
                     + trusted_revision + " with source_sha=" + sha)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    candidate = parser.add_mutually_exclusive_group(required=True)
    candidate.add_argument("--source", type=Path)
    candidate.add_argument("--source-sha")
    parser.add_argument("--report", type=Path, required=True)
    parser.add_argument("--github-output", type=Path)
    parser.add_argument("--yaml-source", type=Path)
    parser.add_argument("--http-source", type=Path)
    parser.add_argument("--overrides", default="{}")
    parser.add_argument("--trusted-revision", default=os.environ.get("JACKSON3_QUALIFIER_REVISION", ""))
    args = parser.parse_args()
    sha = args.source_sha or subprocess.check_output(
        ["git", "-C", str(args.source), "rev-parse", "HEAD"], text=True).strip()
    report = {"source_sha": sha}
    try:
        if not SHA.fullmatch(args.trusted_revision):
            raise ValueError("An independently approved full-SHA qualification policy pin is required")
        policy_root = Path(__file__).resolve().parents[1]
        policy_sha = subprocess.check_output(["git", "-C", str(policy_root), "rev-parse", "HEAD"], text=True).strip()
        if policy_sha != args.trusted_revision or subprocess.check_output(
                ["git", "-C", str(policy_root), "status", "--porcelain"], text=True).strip():
            raise ValueError("Verifier checkout does not match the clean independently pinned policy")
        if not SHA.fullmatch(sha):
            raise ValueError("Invalid source commit")
        refs = json.loads(args.overrides)
        if not isinstance(refs, dict) or set(refs) - {"yaml-rule", "http-client"} or any(
                not isinstance(value, str) or not SHA.fullmatch(value) for value in refs.values()):
            raise ValueError("Invalid prerequisite overrides")
        for name, path in (("light-4j", args.source), ("yaml-rule", args.yaml_source),
                           ("http-client", args.http_source)):
            if path is not None:
                if subprocess.check_output(["git", "-C", str(path), "status", "--porcelain"], text=True).strip():
                    raise ValueError("Modified source checkout: " + name)
                if name != "light-4j":
                    actual = subprocess.check_output(["git", "-C", str(path), "rev-parse", "HEAD"], text=True).strip()
                    if name in refs and refs[name] != actual:
                        raise ValueError("Explicit override differs from checkout: " + name)
                    refs[name] = actual
        report["qualification"] = qualify(GitHub(), sha, refs, args.trusted_revision)
        report["source_refs"] = report["qualification"]["evidence"]["source_refs"]
    except (ValueError, KeyError, subprocess.CalledProcessError) as error:
        report["failure"] = str(error)
        args.report.write_text(json.dumps(report, indent=2) + "\n")
        raise SystemExit(str(error)) from error
    args.report.write_text(json.dumps(report, indent=2) + "\n")
    if args.github_output:
        with args.github_output.open("a") as output:
            for name, value in report["source_refs"].items():
                output.write(name.replace("-", "_") + "=" + value + "\n")
    print("Qualified actual core checkout " + sha + ": " + report["qualification"]["html_url"])


if __name__ == "__main__":
    main()
