#!/usr/bin/env python3
"""Audit the resolved dependency closure, including immutable NetworkNT releases."""
import argparse
from dataclasses import dataclass
import hashlib
import json
from pathlib import Path
import re


# This published Jackson 3 validator is an approved external prerequisite, not a migration snapshot.
APPROVED_RELEASES = {
    "com.networknt:json-schema-validator:jar:3.0.8": "f853380d630b9434e51602bbecf89cb268513e9c764937c0475f4d79b9b6b6ca",
}
KNOWN_JACKSON2_RELEASES = {
    ("com.networknt", "yaml-rule", "3.0.1"),
    ("com.networknt", "chaos-monkey", "2.4.1"),
}
SCOPES = {"compile", "provided", "runtime", "test", "system", "import"}


@dataclass(frozen=True)
class Artifact:
    group: str
    name: str
    kind: str
    version: str
    classifier: str = ""

    @property
    def coordinates(self):
        suffix = ":" + self.classifier if self.classifier else ""
        return f"{self.group}:{self.name}:{self.kind}{suffix}:{self.version}"

    @property
    def filename(self):
        extension = {"test-jar": "jar", "bundle": "jar", "maven-plugin": "jar"}.get(self.kind, self.kind)
        classifier = self.classifier or ("tests" if self.kind == "test-jar" else "")
        suffix = "-" + classifier if classifier else ""
        return f"{self.name}-{self.version}{suffix}.{extension}"

    def path(self, repository):
        return repository / self.group.replace(".", "/") / self.name / self.version / self.filename


def artifacts(tree):
    result = set()
    for line in tree.splitlines():
        for match in re.finditer(r"(?<!\S)([a-zA-Z0-9_.-]+:[a-zA-Z0-9_.-]+:[^\s]+)", line):
            parts = match.group(1).split(":")
            if parts[-1] in SCOPES:
                parts.pop()
            if len(parts) == 4:
                group, name, kind, version = parts
                result.add(Artifact(group, name, kind, version))
            elif len(parts) == 5:
                group, name, kind, classifier, version = parts
                result.add(Artifact(group, name, kind, version, classifier))
    return sorted(result, key=lambda artifact: artifact.coordinates)


def audit(repository, tree):
    dependencies = artifacts(tree)
    checked = []
    approved = []
    failures = []
    jackson2 = []
    for artifact in dependencies:
        if artifact.group.startswith("com.fasterxml.jackson") and not (
                artifact.group == "com.fasterxml.jackson.core" and artifact.name == "jackson-annotations"):
            jackson2.append(artifact.coordinates)
        if artifact.group != "com.networknt" and not artifact.group.startswith("com.networknt."):
            continue
        checked.append(artifact.coordinates)
        path = artifact.path(repository)
        if (artifact.group, artifact.name, artifact.version) in KNOWN_JACKSON2_RELEASES:
            failures.append(f"{artifact.coordinates}: known Jackson 2 release cannot qualify this migration")
            continue
        expected_digest = APPROVED_RELEASES.get(artifact.coordinates)
        if expected_digest:
            if not path.is_file() or hashlib.sha256(path.read_bytes()).hexdigest() != expected_digest:
                failures.append(f"{artifact.coordinates}: approved release SHA-256 mismatch or missing artifact")
            else:
                approved.append({"coordinates": artifact.coordinates, "sha256": expected_digest})
            continue
        tracking = path.parent / "_remote.repositories"
        if not path.is_file() or not tracking.is_file() or artifact.filename + ">=" not in tracking.read_text().splitlines():
            failures.append(f"{artifact.coordinates}: not installed from recorded source checkouts")
    if not checked:
        failures.append("No resolved NetworkNT artifacts found")
    return {"checked": checked, "approved_releases": approved,
            "jackson2_dependencies": jackson2, "failures": failures}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--repository", type=Path, required=True)
    parser.add_argument("--tree", type=Path, required=True)
    parser.add_argument("--report", type=Path, required=True)
    parser.add_argument("--require-jackson3-only", action="store_true")
    args = parser.parse_args()
    report = audit(args.repository, args.tree.read_text())
    if args.require_jackson3_only and report["jackson2_dependencies"]:
        report["failures"].append("Unexpected Jackson 2 dependencies: " + ", ".join(report["jackson2_dependencies"]))
    args.report.write_text(json.dumps(report, indent=2) + "\n")
    for failure in report["failures"]:
        print(failure)
    if report["failures"]:
        raise SystemExit(1)
    print(f"Verified {len(report['checked'])} NetworkNT dependencies, including releases")
    if report["jackson2_dependencies"]:
        print("Jackson 2 runtime boundaries remain documented draft gates: " + ", ".join(report["jackson2_dependencies"]))


if __name__ == "__main__":
    main()
