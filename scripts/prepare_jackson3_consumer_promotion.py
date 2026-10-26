#!/usr/bin/env python3
"""Prepare reviewable patches only after policy and candidate are on origin/master."""
import argparse
import difflib
import json
from pathlib import Path
import re
import shlex
import subprocess
import textwrap
import sys
import tempfile

from refresh_jackson3_qualifications import policy_ref, validate_registry
from jackson3_test_inventory import load_inventory, require_current_baselines

REPOSITORIES = ("light-aws-lambda", "light-lambda-native", "yaml-rule-plugin", "light-codegen",
                "http-sidecar", "light-gateway", "light-spa-4j")
WORKFLOW = ".github/workflows/jackson3-source-build.yml"


def resolution_step(text):
    matches = list(re.finditer(r"(?m)^( *)- name: ['\"]?Resolve prerequisite commits['\"]?[ \t]*$", text))
    if len(matches) != 1:
        raise ValueError("Expected one prerequisite resolution step")
    match = matches[0]
    indent = match[1]
    following = re.search(r"(?m)^" + re.escape(indent) + r"- \S", text[match.end():])
    end = match.end() + following.start() if following else len(text)
    step = text[match.start():end]
    blocks = list(re.finditer(r"(?m)^( +)run: *\|[+-]?[ \t]*$", step))
    if len(blocks) != 1:
        raise ValueError("Prerequisite resolver requires one literal shell block")
    block = blocks[0]
    run_indent = len(block[1])
    body = []
    for line in step[block.end():].splitlines():
        if line.strip() and len(line) - len(line.lstrip(" ")) <= run_indent:
            break
        body.append(line)
    return match.start(), end, indent, body


def resolve_calls(lines):
    """Accept flat literal calls only. Never silently omit an unfamiliar form."""
    calls = []
    in_function = False
    function_indent = ""
    definitions = 0
    for line in lines:
        stripped = line.strip()
        if not stripped or stripped.startswith("#"):
            continue
        if re.fullmatch(r"(?:resolve\s*\(\s*\)|function\s+resolve(?:\s*\(\s*\))?)\s*\{\s*(?:#.*)?", stripped):
            if in_function or definitions:
                raise ValueError("Unsupported duplicate/nested resolve definition")
            definitions += 1
            in_function = True
            function_indent = line[:len(line) - len(line.lstrip(" \t"))]
            continue
        if (in_function and line[:len(line) - len(line.lstrip(" \t"))] == function_indent
                and re.fullmatch(r"}\s*(?:#.*)?", stripped)):
            in_function = False
            continue
        if not in_function and re.match(r"(?:function\b|[A-Za-z_]\w*\s*\(\s*\))", stripped):
            raise ValueError("Unfamiliar shell functions require manual promotion review")
        if not in_function and re.match(r"(?:if|elif|else|fi|for|while|until|case|esac|select|do|done)\b|[({]", stripped):
            raise ValueError("Conditional/grouped resolution requires manual promotion review")
        if not re.search(r"\bresolve\b", stripped):
            continue
        try:
            lexer = shlex.shlex(stripped, posix=True, punctuation_chars=";&|(){}")
            lexer.whitespace_split = True
            tokens = list(lexer)
        except ValueError as error:
            raise ValueError("Unsupported resolve quoting/continuation; refusing to drop a call") from error
        if not tokens:
            continue
        if (in_function or tokens[0] != "resolve" or len(tokens) not in (2, 3)
                or not re.fullmatch("[a-z0-9-]+", tokens[1])
                or (len(tokens) == 3 and not re.fullmatch("[A-Za-z0-9_./-]+", tokens[2]))):
            raise ValueError("Unsupported resolve syntax; refusing to drop a prerequisite")
        calls.append((tokens[1], tokens[2] if len(tokens) == 3 else ""))
    if in_function or definitions != 1:
        raise ValueError("Expected one complete legacy resolve function")
    if len({repo for repo, branch in calls}) != len(calls):
        raise ValueError("Duplicate prerequisite calls require manual promotion review")
    return calls


def promoted_workflow(text, policy_sha, core_sha):
    if not all(re.fullmatch("[0-9a-f]{40}", value) for value in (policy_sha, core_sha)):
        raise ValueError("Promotion requires full policy and candidate SHAs")
    start, end, indent, body = resolution_step(text)
    calls = resolve_calls(body)
    if sum(repo == "light-4j" for repo, branch in calls) != 1:
        raise ValueError("Expected one legacy core resolver to replace")
    declared = set()
    for value in re.findall(r"(?m)^\s+repository: *(.+?) *$", text):
        values = shlex.split(value, comments=True)
        if len(values) != 1 or not re.fullmatch("networknt/[a-z0-9-]+", values[0]):
            raise ValueError("Promotion requires a literal checkout repository inventory")
        declared.add(values[0].split("/", 1)[1])
    planned = {repo for repo, branch in calls} | {"yaml-rule", "http-client"}
    if planned != declared:
        raise ValueError("Resolver/checkouts differ; refusing prerequisite loss: missing="
                         + str(sorted(declared - planned)) + "; extra=" + str(sorted(planned - declared)))
    remaining = {repo: branch or None for repo, branch in calls if repo not in {"light-4j", "yaml-rule", "http-client"}}
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
    replacement = textwrap.indent(textwrap.dedent(replacement), indent)
    text = text[:start] + replacement + text[end:]
    text, count = re.subn(r"(?m)^( +JACKSON3_QUALIFIER_REVISION: )[0-9a-f]{40}[ \t]*$",
                         lambda match: match[1] + policy_sha + "\n"
                         + " " * (len(match[1]) - len(match[1].lstrip())) + "JACKSON3_CORE_REVISION: " + core_sha, text)
    if count != 1:
        raise ValueError("Expected one policy pin")
    return text.replace("# Proposed immutable policy pin; promotion requires review of this commit.",
                        "# Reviewed master policy and separately qualified candidate; both pins are immutable.")


def require_on_master(checkout, sha):
    if not re.fullmatch("[0-9a-f]{40}", sha):
        raise ValueError("Promotion requires full commit SHAs")
    result = subprocess.run(["git", "-C", str(checkout), "merge-base", "--is-ancestor", sha, "origin/master"],
                            capture_output=True)
    if result.returncode:
        raise ValueError("Commit is not on the fetched origin/master: " + sha + "; merge/review first, then fetch")


def registered_qualification(registry, policy_sha, core_sha, evidence):
    entries = validate_registry(registry)
    if evidence.get("policy_sha") != policy_sha or evidence.get("source_sha") != core_sha:
        raise ValueError("Promotion evidence does not match the selected policy/core")
    entry = {"policy_sha": policy_sha, "core_sha": core_sha, "policy_ref": policy_ref(policy_sha),
             "source_refs": evidence.get("source_refs")}
    validate_registry({"schema": 1, "qualifications": [entry]})
    for existing in entries:
        if (existing["policy_sha"], existing["core_sha"]) == (policy_sha, core_sha):
            if existing != entry:
                raise ValueError("Registered pair has different prerequisites; review the registry instead of replacing it")
            return registry
    return {"schema": 1, "qualifications": entries + [entry]}


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
    for required in ("scripts/resolve_jackson3_consumer_sources.py", "scripts/refresh_jackson3_qualifications.py",
                     "scripts/jackson3-promoted-qualifications.json", ".github/workflows/jackson3-qualification-refresh.yml"):
        subprocess.run(["git", "-C", str(args.master_checkout), "cat-file", "-e", args.policy_sha + ":" + required], check=True)
    patches = {}
    for repo in REPOSITORIES:
        before = (args.worktrees / repo / WORKFLOW).read_text()
        after = promoted_workflow(before, args.policy_sha, args.core_sha)
        patches[repo] = "".join(difflib.unified_diff(before.splitlines(True), after.splitlines(True),
                                                  fromfile="a/" + WORKFLOW, tofile="b/" + WORKFLOW))
    # Run from the clean selected policy checkout, not a newer moving master checkout.
    with tempfile.TemporaryDirectory() as directory:
        report = Path(directory) / "qualification.json"
        subprocess.run([sys.executable, str(Path(__file__).with_name("jackson3_core_qualification.py")),
                        "--source-sha", args.core_sha, "--trusted-revision", args.policy_sha,
                        "--report", str(report)], check=True)
        qualification = json.loads(report.read_text())
        evidence = qualification["qualification"]["evidence"]
    require_current_baselines(evidence["modules"], load_inventory())
    registry_path = "scripts/jackson3-promoted-qualifications.json"
    before_registry = subprocess.check_output(["git", "-C", str(args.master_checkout), "show",
                                              "origin/master:" + registry_path], text=True)
    registry = registered_qualification(json.loads(before_registry), args.policy_sha, args.core_sha, evidence)
    after_registry = json.dumps(registry, indent=2) + "\n"
    patches["light-4j-refresh"] = "".join(difflib.unified_diff(before_registry.splitlines(True), after_registry.splitlines(True),
                                                            fromfile="a/" + registry_path, tofile="b/" + registry_path))
    args.output.mkdir(parents=True, exist_ok=False)
    for repo, patch in patches.items():
        (args.output / (repo + ".patch")).write_text(patch)
    (args.output / "qualification.json").write_text(json.dumps(qualification, indent=2) + "\n")
    (args.output / "PROMOTION.md").write_text(
        "Create/verify the fixed policy ref first: " + policy_ref(args.policy_sha) + " -> " + args.policy_sha + "\n\n"
        "Land light-4j-refresh.patch on master before consumer adoption. It registers the immutable core/policy/prerequisite tuple.\n"
        "Run jackson3-qualification-refresh.yml from master and check the resulting qualify/record run, not just dispatch success.\n"
        "Then review/apply the seven consumer patches. The registry commit does not move either consumer pin.\n")
    print("Prepared seven consumer patches and the refresh-registration patch; no worktrees were edited")


if __name__ == "__main__":
    main()
