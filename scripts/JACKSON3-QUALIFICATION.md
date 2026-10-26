# Reusable core qualification

Consumers must pin an independently reviewed light-4j **policy commit**, separately
from the candidate core commit. Do not obtain that pin from candidate files,
branch heads, an artifact, or a repository variable that moves automatically.
Changing the pin requires review of the workflow, recorder, verifier, bootstrap
script, and test-module inventory at the new commit.

The existing `jackson3-source-build.yml` has two paths:

* PR/push builds (and dispatch without `source_sha`) are ordinary candidate CI.
  They do not publish reusable evidence.
* Dispatch with a full `source_sha` runs the policy at the dispatch ref and builds
  that candidate in a separate checkout. A fresh recording job checks out the
  policy again, validates required module reports, and publishes schema-2 evidence.

Run qualification from a branch/tag pointing at the reviewed policy commit:

```sh
gh workflow run jackson3-source-build.yml --repo networknt/light-4j \
  --ref POLICY_REF -f source_sha=CANDIDATE_FULL_SHA
```

`source_refs` may optionally pin yaml-rule/http-client to full SHAs. Otherwise
the trusted run resolves those defaults once and records them. Consumers select
successful retained evidence **before** checking out those two repositories;
they use its recorded SHAs instead of resolving moving heads independently.
Explicit consumer overrides must match the selected qualification. Conflicts
require another qualification with the requested prerequisite SHAs.

The consumer checks GitHub API metadata: repository, workflow path, dispatch
event, the independently pinned policy `head_sha`, successful run/attempt, and
the artifact associated with that attempt. Evidence must also match the candidate
SHA, policy SHA, required module coverage, and summed test counters. A later
failed or pending attempt does not revoke earlier successful retained evidence.
Missing/expired evidence reports the candidate and policy requiring a new run.

The policy inventory covers 83 test modules. Each must report executed tests,
except the explicitly listed, already-disabled request-transformer suite. Its
skips are recorded and never counted as executed tests. Reports are discovered
recursively, and missing or unexpected modules fail qualification; adding a
module requires a reviewed inventory update. This is module coverage, not a
promise that a test count can never change.

This is trusted CI orchestration, not a proof that arbitrary candidate test code
is honest. Writers who can alter an approved policy pin or its consumer workflow
are inside the trust boundary. Candidate Maven code still executes in the build
job; it cannot modify the fresh recorder job. No independent security attestation
or isolation from malicious test-report generation is claimed.

Policy promotion and candidate qualification are distinct: a green PR check does
not approve a policy pin, and merging does not automatically move existing pins.
