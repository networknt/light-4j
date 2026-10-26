"""Registration protection and merged acceptance gates; no mutations."""
import json
import argparse
from pathlib import Path
import re
import subprocess


REQUIRED_RULES = {"update", "deletion", "non_fast_forward"}


def require_protected_ref(entry, github, require_bypass_visibility=True):
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
        bypass_known = "bypass_actors" in ruleset
        bypass_ok = ruleset.get("bypass_actors") == [] or (not bypass_known and not require_bypass_visibility)
        if (ruleset.get("target") == "branch" and ruleset.get("enforcement") == "active"
                and bypass_ok
                and refs.get("include") == ["refs/heads/" + branch] and refs.get("exclude") == []
                and REQUIRED_RULES <= types and REQUIRED_RULES <= effective_types):
            if not bypass_known:
                print("Verified effective protection; bypass list unavailable to read-only token:", branch, flush=True)
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
                  forced.get("artifact_id"), reuse.get("coordinator_run_id"),
                  forced.get("coordinator_attempt"), forced.get("producer_attempt"), reuse.get("coordinator_attempt")):
        if type(value) is not int or value <= 0:
            raise ValueError("Acceptance needs positive ruleset, run and artifact IDs")
    if (forced.get("conclusion") != "success" or reuse.get("conclusion") != "success"
            or type(reuse.get("dispatched")) is not int or reuse["dispatched"] != 0
            or reuse["coordinator_run_id"] == forced["coordinator_run_id"]
            or not re.fullmatch("[0-9a-f]{64}", str(forced.get("artifact_sha256", "")))):
        raise ValueError("Acceptance needs successful distinct runs, proof digest and zero reuse dispatches")
    return data


def require_registry_at(checkout, revision, entry):
    result = subprocess.run(["git", "-C", str(checkout), "show",
                             revision + ":scripts/jackson3-promoted-qualifications.json"],
                            capture_output=True, text=True)
    if result.returncode or entry not in json.loads(result.stdout).get("qualifications", []):
        raise ValueError("Acceptance registration commit does not contain the tuple: " + revision)


def require_master_ancestor(checkout, revision):
    result = subprocess.run(["git", "-C", str(checkout), "merge-base", "--is-ancestor",
                             revision, "origin/master"], capture_output=True)
    if result.returncode:
        raise ValueError("Accepted commit is not on fetched master: " + revision)


def job_log(job_id):
    return subprocess.check_output(["gh", "api", "--allow-escape-sequences",
        "repos/networknt/light-4j/actions/jobs/" + str(job_id) + "/logs"], text=True, timeout=30)


def verify_acceptance_facts(checkout, entry, data, github, read_log):
    forced, reuse = data["forced_renewal"], data["fresh_proof_reuse"]

    def has_message(log, message):
        # Job logs may prefix timestamps; match the complete message suffix so
        # run 11 cannot accidentally match run 110.
        return any(line.rstrip().endswith(message) for line in log.splitlines())

    def run(run_id, attempt, workflow):
        value = github("actions/runs/" + str(run_id) + "/attempts/" + str(attempt))
        if (value.get("id") != run_id or value.get("run_attempt") != attempt
                or value.get("status") != "completed" or value.get("conclusion") != "success"
                or value.get("event") != "workflow_dispatch"
                or value.get("path", "").split("@")[0] != ".github/workflows/" + workflow):
            raise ValueError("Acceptance run metadata does not match: " + str(run_id))
        return value

    def coordinator(run_id, attempt):
        value = run(run_id, attempt, "jackson3-qualification-refresh.yml")
        if value.get("head_branch") != "master":
            raise ValueError("Acceptance coordinator did not run on master")
        require_master_ancestor(checkout, value["head_sha"])
        require_registry_at(checkout, value["head_sha"], entry)
        jobs = github("actions/runs/" + str(run_id) + "/attempts/" + str(attempt) + "/jobs?per_page=100")["jobs"]
        refresh = [job for job in jobs if job.get("name") == "refresh" and job.get("conclusion") == "success"]
        if len(refresh) != 1:
            raise ValueError("Acceptance needs one successful refresh job")
        return read_log(refresh[0]["id"])

    producer = run(forced["producer_run_id"], forced["producer_attempt"], "jackson3-source-build.yml")
    if (producer.get("head_sha") != entry["policy_sha"]
            or producer.get("actor", {}).get("login") != "github-actions[bot]"):
        raise ValueError("Acceptance producer is not the bot-dispatched pinned policy")
    artifact = github("actions/artifacts/" + str(forced["artifact_id"]))
    if (artifact.get("id") != forced["artifact_id"] or artifact.get("expired") is not False
            or artifact.get("workflow_run", {}).get("id") != forced["producer_run_id"]
            or artifact.get("name") != "core-source-qualification-attempt-" + str(forced["producer_attempt"])
            or artifact.get("digest") != "sha256:" + forced["artifact_sha256"]):
        raise ValueError("Acceptance artifact identity, digest or retention does not match GitHub")
    forced_log = coordinator(forced["coordinator_run_id"], forced["coordinator_attempt"])
    expected = "Verified renewed proof: " + entry["core_sha"] + " run " + str(forced["producer_run_id"])
    if (not has_message(forced_log, expected)
            or not re.search(r"(?m)Verified forced registered renewal of [1-9][0-9]* qualifications\s*$", forced_log)
            or not has_message(forced_log, "Dispatched qualification run: " + str(forced["producer_run_id"]))):
        raise ValueError("Acceptance forced-renewal log does not verify the recorded producer")
    reuse_log = coordinator(reuse["coordinator_run_id"], reuse["coordinator_attempt"])
    if (not has_message(reuse_log, "Healthy retained proof; no reactor dispatched: " + entry["core_sha"])
            or not has_message(reuse_log, "Verified renewal of 0 qualifications; healthy proofs did not trigger builds")
            or "Dispatched qualification run:" in reuse_log):
        raise ValueError("Acceptance reuse log does not prove zero dispatches")


def require_accepted_record(checkout, entry, github=None, read_log=None):
    """Read fetched master, never accept an uncommitted or PR-only local record."""
    path = acceptance_path(entry)
    result = subprocess.run(["git", "-C", str(checkout), "show", "origin/master:" + path],
                            capture_output=True, text=True, check=False)
    if result.returncode:
        raise ValueError("Merge reviewed acceptance record before consumer adoption: " + path)
    data = validate_acceptance(result.stdout, entry)
    require_master_ancestor(checkout, data["registration_sha"])
    require_registry_at(checkout, data["registration_sha"], entry)
    if github is None:
        from refresh_jackson3_qualifications import api
        github = api
    verify_acceptance_facts(checkout, entry, data, github, read_log or job_log)
    return data


if __name__ == "__main__":
    from refresh_jackson3_qualifications import api, validate_registry
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--read-only", action="store_true",
                        help="Check visible effective protection; GitHub hides bypass actors from read-only tokens")
    args = parser.parse_args()
    registry = json.loads(Path(__file__).with_name("jackson3-promoted-qualifications.json").read_text())
    for entry in validate_registry(registry):
        ruleset_id = require_protected_ref(entry, api, require_bypass_visibility=not args.read_only)
        print("Verified protected policy:", entry["policy_sha"], "ruleset", ruleset_id)
