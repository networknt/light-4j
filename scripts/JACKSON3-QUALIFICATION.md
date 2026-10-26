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

The policy inventory covers 83 test modules. Per-module executed-test floors retain the verified baseline (1,276 executed tests total; 67 modules have floors greater than one). Baseline source: `27aed6a44a91d40a4a590be563b3d6e2fbc3415b`, qualification [40266655138](https://github.com/networknt/light-4j/actions/runs/40266655138), retained schema-2 module counters. Each must report its baseline executed count or more,
except the explicitly listed, already-disabled request-transformer suite. Its
skips are recorded and never counted as executed tests. Reports are discovered
recursively, and missing or unexpected modules fail qualification; adding a
module requires a reviewed inventory update. Intentional corpus reductions require a reviewed floor change; new modules require a reviewed inventory change. The retained baseline is a count floor, not a claim about assertion quality.

This is trusted CI orchestration, not a proof that arbitrary candidate test code
is honest. Writers who can alter an approved policy pin or its consumer workflow
are inside the trust boundary. Candidate Maven code still executes in the build
job; it cannot modify the fresh recorder job. No independent security attestation
or isolation from malicious test-report generation is claimed.

Policy promotion and candidate qualification are distinct: a green PR check does
not approve a policy pin, and merging does not automatically move existing pins.


## Rejection actions

An inventory mismatch is not repaired by rerunning the same policy. Restore missing
candidate reports/modules, or review intentional module changes, update the
inventory, promote a reviewed master policy and update consumer pins. An executed
count below its floor requires restoring tests/configuration or a reviewed corpus
reduction and floor update. Rejected evidence identifies these actions without a
blanket rerun command. Only an absent run or expired evidence calls for a new
dispatch without a source/policy change; failed runs must be inspected first.

## Untrusted report input

The fresh recorder streams XML counters through Expat, with handlers that reject
DOCTYPE, entity declarations, external entity references and skipped entities
before expansion. This is encoding-aware (including UTF-16), not a byte-pattern
filter. No candidate-produced XML is parsed with ElementTree. Limits are 16 MiB
per report, 256 MiB total, 10,000 report files, depth 64, and 10,000 suite elements
per report. It retains only suite counters, not the full XML tree. Existing
Surefire reports were checked against these limits. This does not change the
separate limit on trust in candidate-generated report contents.

## Promote after review and merge

Do not repin consumers to another open #2821 head. Review/merge the policy content
first. Fetch the resulting master commits and choose explicit policy and candidate
SHAs on master (they may be the same commit). The common candidate for all seven
consumers must also include the reviewed OAuth/token native fixes needed by the
gateway; finish the relevant stack merges before choosing that common candidate.

Then dispatch qualification once for that candidate at a ref pointing to the exact
reviewed policy SHA. A dispatch from a later moving master head does not qualify
an older policy pin. Check the run and download its proof before applying the
consumer follow-up.

Prepare the seven reviewable workflow patches with:

```sh
python3 scripts/prepare_jackson3_consumer_promotion.py \
  --policy-sha REVIEWED_POLICY_MASTER_SHA --core-sha QUALIFIED_CORE_MASTER_SHA \
  --master-checkout /path/to/fetched/light-4j \
  --worktrees /path/to/migration/worktrees --output /path/to/new/patch-directory
```

The preparer refuses SHAs not on the fetched origin/master and policies missing
the shared resolver. It writes patches only; it never edits consumer checkouts.
Review/apply those patches as the follow-up in the seven consumer PRs. Each patch
sets literal `JACKSON3_QUALIFIER_REVISION` and `JACKSON3_CORE_REVISION` values and
replaces the copied shell resolver with `resolve_jackson3_consumer_sources.py`
from the policy checkout. Core resolution performs no branch/PR lookup; only an
explicit full-SHA override may select another qualified candidate. Remaining
prerequisite migration branches, where still needed, use the one shared tested
resolver. Subsequent pushes to the open core branch, branch deletion or a squash
cannot move these literal pins. A squash is handled by selecting its actual
reviewed master commit during promotion, not by guessing its SHA in advance.
