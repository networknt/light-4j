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
  "observed_ruleset_id": 123,
  "refresh_protocol": 1,
  "forced_renewal": {
    "coordinator_run_id": 100,
    "coordinator_attempt": 1,
    "producer_run_id": 101,
    "producer_attempt": 1,
    "conclusion": "success",
    "artifact_id": 102,
    "artifact_sha256": "<64-hex-characters>",
    "expires_at": "<UTC expiry>"
  },
  "fresh_proof_reuse": {
    "coordinator_run_id": 103,
    "coordinator_attempt": 1,
    "conclusion": "success",
    "dispatched": 0
  }
}
```

The preparer reads the file from `origin/master`, checks tuple/decision and live
protection semantics (the recorded ruleset ID is historical), and verifies that
the registration commit contains the tuple. It re-fetches the recorded successful
run attempts and artifact identity/digest from GitHub, checks coordinator source
commits on master contain the tuple, and reads the exact-attempt coordinator job
logs for forced verification and zero-dispatch reuse. Missing/expired artifacts
or unavailable logs block adoption; renew the acceptance record after rerunning
the checks if needed. This does not affect renewal of already adopted consumers.
The pinned verifier separately checks current retained qualification evidence.
The preparer does not infer independent approval from a
Git commit: ordinary required PR review remains the approval boundary. Failed
verification belongs in a `blocked` record, which cannot authorize adoption.

`observed_ruleset_id` is optional, informational history. It is not validated or
used to authorize promotion; live protection semantics are checked independently.
`refresh_protocol` selects the frozen log protocol in
`../jackson3_refresh_protocol.py`; historical records without it use v1. Wording
changes require a new protocol version while retaining old parsers. Human-facing
diagnostics outside these protocol messages may change freely.

Acceptance is intentionally time-bounded by the original artifact and job-log
retention. Before that deadline (or when the API cannot return the originals),
repeat forced renewal and normal reuse and merge an updated record at the same
tuple path. This is required even for regenerating patches or adding a consumer
to an unchanged tuple. No fallback treats an unavailable API fact as verified.
