#!/usr/bin/env python3
"""Shared consumer resolver. Core is an explicit SHA, never a moving branch."""
import argparse
import json
import os
from pathlib import Path
import re
import subprocess
import sys

from jackson3_core_qualification import SHA


def api(endpoint, paginate=False):
    command = ["gh", "api"]
    if paginate:
        command += ["--paginate", "--slurp"]
    return json.loads(subprocess.check_output(command + [endpoint], text=True))


def validate_inputs(core_sha, repositories, overrides):
    if not SHA.fullmatch(core_sha):
        raise ValueError("Core requires an explicit full commit SHA")
    if not isinstance(repositories, dict) or any(
            not re.fullmatch("[a-z0-9-]+", name) or name in {"light-4j", "yaml-rule", "http-client"}
            or (branch is not None and (not isinstance(branch, str) or not re.fullmatch("[A-Za-z0-9_/-]+", branch)))
            for name, branch in repositories.items()):
        raise ValueError("Invalid remaining prerequisite plan")
    allowed = set(repositories) | {"light-4j", "yaml-rule", "http-client"}
    if not isinstance(overrides, dict) or set(overrides) - allowed or any(
            not isinstance(value, str) or not SHA.fullmatch(value) for value in overrides.values()):
        raise ValueError("Source overrides must be allowed repositories with full SHAs")


def resolve_remaining(repo, branch, override, github=api):
    if override is not None:
        return override
    prefix = "repos/networknt/" + repo
    if branch:
        pages = github(prefix + "/pulls?head=networknt:" + branch + "&state=all&per_page=100", True)
        pulls = [item for page in pages for item in page]
        opened = [item for item in pulls if item["state"] == "open"]
        if len(opened) > 1:
            raise ValueError("Ambiguous prerequisite branch: " + repo)
        if opened:
            ref = opened[0]["head"]["sha"]
            if not SHA.fullmatch(ref):
                raise ValueError("Invalid prerequisite PR head")
            return ref
        if pulls and not any(item.get("merged_at") for item in pulls):
            raise ValueError("Prerequisite branch closed without merging: " + repo)
    default = github(prefix)["default_branch"]
    ref = github(prefix + "/commits/" + default)["sha"]
    if not SHA.fullmatch(ref):
        raise ValueError("Invalid prerequisite default head")
    return ref


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--core-sha", required=True)
    parser.add_argument("--repositories", default="{}")
    parser.add_argument("--source-refs", default=os.environ.get("SOURCE_REFS", "{}"))
    parser.add_argument("--report", type=Path, required=True)
    parser.add_argument("--github-output", type=Path, required=True)
    args = parser.parse_args()
    repositories, overrides = json.loads(args.repositories), json.loads(args.source_refs)
    validate_inputs(args.core_sha, repositories, overrides)
    core = overrides.get("light-4j", args.core_sha)
    core_overrides = {name: overrides[name] for name in ("yaml-rule", "http-client") if name in overrides}
    # The independently pinned verifier checks its own clean policy checkout and run metadata.
    subprocess.run([sys.executable, str(Path(__file__).with_name("jackson3_core_qualification.py")),
                    "--source-sha", core, "--overrides", json.dumps(core_overrides),
                    "--report", str(args.report), "--github-output", str(args.github_output)], check=True)
    refs = {"light-4j": core}
    for name, branch in repositories.items():
        refs[name] = resolve_remaining(name, branch, overrides.get(name))
    with args.github_output.open("a") as output:
        for name, ref in refs.items():
            output.write(name.replace("-", "_") + "=" + ref + "\n")


if __name__ == "__main__":
    main()
