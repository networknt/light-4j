#!/usr/bin/env python3
"""Prepare reviewable patches only after policy and candidate are on origin/master."""
import argparse
import difflib
import json
from pathlib import Path
import re
import subprocess

REPOSITORIES = ("light-aws-lambda", "light-lambda-native", "yaml-rule-plugin", "light-codegen",
                "http-sidecar", "light-gateway", "light-spa-4j")
WORKFLOW = ".github/workflows/jackson3-source-build.yml"


def promoted_workflow(text, policy_sha, core_sha):
    if not all(re.fullmatch("[0-9a-f]{40}", value) for value in (policy_sha, core_sha)):
        raise ValueError("Promotion requires full policy and candidate SHAs")
    calls = re.findall(r"^          resolve ([a-z0-9-]+)(?: ([A-Za-z0-9_/-]+))?$", text, re.M)
    if sum(repo == "light-4j" for repo, branch in calls) != 1:
        raise ValueError("Expected one legacy core resolver to replace")
    remaining = {repo: branch or None for repo, branch in calls if repo not in {"light-4j", "yaml-rule", "http-client"}}
    text, count = re.subn(r"(  JACKSON3_QUALIFIER_REVISION: )[0-9a-f]{40}",
                         lambda match: match[1] + policy_sha + "\n  JACKSON3_CORE_REVISION: " + core_sha, text)
    if count != 1:
        raise ValueError("Expected one policy pin")
    text = text.replace("# Proposed immutable policy pin; promotion requires review of this commit.",
                        "# Reviewed master policy and separately qualified candidate; both pins are immutable.")
    start = text.index("      - name: Resolve prerequisite commits\n")
    end = text.index("      - uses:", start)
    plan = json.dumps(remaining, separators=(",", ":"))
    replacement = '''      - name: Select qualified core and resolve remaining prerequisites
        id: prerequisites
        env:
          GH_TOKEN: ${{ github.token }}
          SOURCE_REFS: ${{ inputs.source_refs || '{}' }}
        shell: bash
        run: |
          python3 qualification-policy/scripts/resolve_jackson3_consumer_sources.py \\
            --core-sha "$JACKSON3_CORE_REVISION" \\
            --repositories 'PLAN' --source-refs "$SOURCE_REFS" \\
            --report core-qualification.json --github-output "$GITHUB_OUTPUT"
'''.replace("PLAN", plan)
    return text[:start] + replacement + text[end:]


def require_on_master(checkout, sha):
    if not re.fullmatch("[0-9a-f]{40}", sha):
        raise ValueError("Promotion requires full commit SHAs")
    result = subprocess.run(["git", "-C", str(checkout), "merge-base", "--is-ancestor", sha, "origin/master"],
                            capture_output=True)
    if result.returncode:
        raise ValueError("Commit is not on the fetched origin/master: " + sha + "; merge/review first, then fetch")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--policy-sha", required=True)
    parser.add_argument("--core-sha", required=True)
    parser.add_argument("--master-checkout", type=Path, required=True)
    parser.add_argument("--worktrees", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    require_on_master(args.master_checkout, args.policy_sha)
    require_on_master(args.master_checkout, args.core_sha)
    subprocess.run(["git", "-C", str(args.master_checkout), "cat-file", "-e",
                    args.policy_sha + ":scripts/resolve_jackson3_consumer_sources.py"], check=True)
    patches = {}
    for repo in REPOSITORIES:
        before = (args.worktrees / repo / WORKFLOW).read_text()
        after = promoted_workflow(before, args.policy_sha, args.core_sha)
        patches[repo] = "".join(difflib.unified_diff(before.splitlines(True), after.splitlines(True),
                                                  fromfile="a/" + WORKFLOW, tofile="b/" + WORKFLOW))
    args.output.mkdir(parents=True, exist_ok=False)
    for repo, patch in patches.items():
        (args.output / (repo + ".patch")).write_text(patch)
    print("Prepared seven patches; consumer worktrees were not edited")


if __name__ == "__main__":
    main()
