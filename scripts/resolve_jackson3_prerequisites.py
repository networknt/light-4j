#!/usr/bin/env python3
"""Resolve omitted prerequisites; explicit inputs must be complete SHAs."""
import argparse
import json
import os
from pathlib import Path
import re
import subprocess


def resolve(overrides, api):
    names = {"yaml-rule", "http-client"}
    if not isinstance(overrides, dict) or set(overrides) - names or any(
            not isinstance(ref, str) or not re.fullmatch("[0-9a-f]{40}", ref)
            for ref in overrides.values()):
        raise ValueError("Prerequisite overrides must map yaml-rule/http-client to full commit SHAs")
    refs = {}
    for repo in sorted(names):
        ref = overrides.get(repo)
        if ref is None:
            branch = api(f"repos/networknt/{repo}")["default_branch"]
            ref = api(f"repos/networknt/{repo}/commits/{branch}")["sha"]
        if not re.fullmatch("[0-9a-f]{40}", ref):
            raise ValueError("GitHub returned an invalid prerequisite SHA")
        refs[repo] = ref
    return refs


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    refs = resolve(json.loads(os.environ.get("SOURCE_REFS", "{}")),
                   lambda endpoint: json.loads(subprocess.check_output(["gh", "api", endpoint], text=True)))
    args.output.write_text(json.dumps(refs, indent=2) + "\n")


if __name__ == "__main__":
    main()
