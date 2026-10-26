# Workflow-token acceptance records

The promotion PR author prepares `<full-policy-sha>-<full-core-sha>.md`; an
independent maintainer reviews it. Keep the six sections specified in
`../JACKSON3-QUALIFICATION.md`, with links to logs and run/proof metadata.
Include exactly one fenced `json` block with this schema (substitute actual
values; this example is not an accepted tuple):

```json
{
  "schema": 1,
  "decision": "accepted",
  "tuple": {
    "policy_sha": "<full-policy-sha>",
    "core_sha": "<full-core-sha>",
    "policy_ref": "jackson3-qualification-policy-<full-policy-sha>",
    "source_refs": {"yaml-rule": "<full-sha>", "http-client": "<full-sha>"}
  },
  "registration_sha": "<merged-registration-sha>",
  "ruleset_id": 123,
  "forced_renewal": {
    "coordinator_run_id": 100,
    "producer_run_id": 101,
    "conclusion": "success",
    "artifact_id": 102,
    "artifact_sha256": "<64-hex-characters>",
    "expires_at": "<UTC expiry>"
  },
  "fresh_proof_reuse": {
    "coordinator_run_id": 103,
    "conclusion": "success",
    "dispatched": 0
  }
}
```

The preparer reads the file from `origin/master`, checks tuple/decision, live
ruleset identity and registration ancestry, and validates the successful run IDs,
artifact digest and reuse count. It does not infer independent approval from a
Git commit: ordinary required PR review remains the approval boundary. Failed
verification belongs in a `blocked` record, which cannot authorize adoption.
