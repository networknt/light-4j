"""Registration protection and merged acceptance gates; no mutations."""
import json
from pathlib import Path
import re
import subprocess


REQUIRED_RULES = {"update", "deletion", "non_fast_forward"}


def require_protected_ref(entry, github):
    branch = entry["policy_ref"]
    if github("commits/" + branch).get("sha") != entry["policy_sha"]:
        raise ValueError("Pinned policy ref moved")
    effective = github("rules/branches/" + branch)
    if not isinstance(effective, list):
        raise ValueError("Invalid effective policy-ref rules")
    candidates = {rule.get("ruleset_id") for rule in effective
                  if rule.get("type") in REQUIRED_RULES and type(rule.get("ruleset_id")) is int}
    for ruleset_id in sorted(candidates):
        ruleset = github("rulesets/" + str(ruleset_id))
        refs = ruleset.get("conditions", {}).get("ref_name", {})
        types = {rule.get("type") for rule in ruleset.get("rules", [])}
        effective_types = {rule.get("type") for rule in effective if rule.get("ruleset_id") == ruleset_id}
        if (ruleset.get("target") == "branch" and ruleset.get("enforcement") == "active"
                and ruleset.get("bypass_actors") == []
                and refs.get("include") == ["refs/heads/" + branch] and refs.get("exclude") == []
                and REQUIRED_RULES <= types and REQUIRED_RULES <= effective_types):
            return ruleset_id
    raise ValueError("Policy ref needs an active exact-ref ruleset with update/deletion/non_fast_forward and no bypass actors")


def acceptance_path(entry):
    return "scripts/jackson3-acceptance/" + entry["policy_sha"] + "-" + entry["core_sha"] + ".md"


def validate_acceptance(text, entry):
    blocks = re.findall(r"(?ms)^```json\s*\n(.*?)^```\s*$", text)
    if len(blocks) != 1:
        raise ValueError("Acceptance record requires one JSON metadata block")
    data = json.loads(blocks[0])
    if (not isinstance(data, dict) or type(data.get("schema")) is not int or data["schema"] != 1
            or data.get("decision") != "accepted" or data.get("tuple") != entry):
        raise ValueError("Acceptance decision/tuple does not match registered promotion")
    if not re.fullmatch("[0-9a-f]{40}", str(data.get("registration_sha", ""))):
        raise ValueError("Acceptance needs the merged registration SHA")
    forced, reuse = data.get("forced_renewal", {}), data.get("fresh_proof_reuse", {})
    if not isinstance(forced, dict) or not isinstance(reuse, dict):
        raise ValueError("Acceptance needs forced-renewal and fresh-proof-reuse objects")
    for value in (data.get("ruleset_id"), forced.get("coordinator_run_id"), forced.get("producer_run_id"),
                  forced.get("artifact_id"), reuse.get("coordinator_run_id")):
        if type(value) is not int or value <= 0:
            raise ValueError("Acceptance needs positive ruleset, run and artifact IDs")
    if (forced.get("conclusion") != "success" or reuse.get("conclusion") != "success"
            or type(reuse.get("dispatched")) is not int or reuse["dispatched"] != 0
            or reuse["coordinator_run_id"] == forced["coordinator_run_id"]
            or not re.fullmatch("[0-9a-f]{64}", str(forced.get("artifact_sha256", "")))):
        raise ValueError("Acceptance needs successful distinct runs, proof digest and zero reuse dispatches")
    return data


def require_accepted_record(checkout, entry, ruleset_id):
    """Read fetched master, never accept an uncommitted or PR-only local record."""
    path = acceptance_path(entry)
    result = subprocess.run(["git", "-C", str(checkout), "show", "origin/master:" + path],
                            capture_output=True, text=True, check=False)
    if result.returncode:
        raise ValueError("Merge reviewed acceptance record before consumer adoption: " + path)
    data = validate_acceptance(result.stdout, entry)
    if data["ruleset_id"] != ruleset_id:
        raise ValueError("Accepted ruleset differs from live policy protection; review acceptance again")
    result = subprocess.run(["git", "-C", str(checkout), "merge-base", "--is-ancestor",
                             data["registration_sha"], "origin/master"], capture_output=True)
    if result.returncode:
        raise ValueError("Accepted registration SHA is not on fetched master")
    return data


if __name__ == "__main__":
    from refresh_jackson3_qualifications import api, validate_registry
    registry = json.loads(Path(__file__).with_name("jackson3-promoted-qualifications.json").read_text())
    for entry in validate_registry(registry):
        ruleset_id = require_protected_ref(entry, api)
        print("Verified protected policy:", entry["policy_sha"], "ruleset", ruleset_id)
