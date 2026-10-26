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

The inventory stores a verified executed-test baseline for 83 modules: 1,276 total,
measured at `27aed6a44a91d40a4a590be563b3d6e2fbc3415b` in qualification
[40266655138](https://github.com/networknt/light-4j/actions/runs/40266655138).
The proposed tolerance allows a loss of `max(1, ceil(5% * baseline))` per module,
with a floor of one executed test in each required module. Examples: 100 -> 95,
3 -> 2, 1 -> 1. The resulting floors total 1,171, not 1,276. Both the baseline
and effective floor are stored, and tooling validates the formula. The existing
disabled request-transformer suite remains the explicit zero-execution exception.
This tolerance is proposed for review, not silently adopted by the seven consumers.

Small consolidations or environment-dependent skips within the allowance need no
policy change. Adopting a different candidate still requires its own qualification;
that is separate from changing the policy pin. Drops below the allowance or
intentional module changes require review. Missing reports, empty modules and a
large suite reduced to one test remain failures. Reports are discovered recursively;
missing or unexpected modules cannot be hidden by a positive reactor total. This
is a count/coverage signal, not a claim about assertion quality.

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

Create a stable policy ref `jackson3-qualification-policy-POLICY_FULL_SHA` pointing
to the exact reviewed policy SHA. Then dispatch qualification once for that
candidate at that fixed ref. A dispatch from a later moving master head does not qualify
an older policy pin. Check the run and download its proof before applying the
consumer follow-up.

From a clean checkout of the selected policy commit, prepare the seven workflow
patches plus refresh registration with:

```sh
python3 scripts/prepare_jackson3_consumer_promotion.py \
  --policy-sha REVIEWED_POLICY_MASTER_SHA --core-sha QUALIFIED_CORE_MASTER_SHA \
  --master-checkout /path/to/fetched/light-4j \
  --worktrees /path/to/migration/worktrees --output /path/to/new/patch-directory
```

The preparer refuses SHAs not on the fetched origin/master, policies missing the
shared resolver/refresh workflow, or unverifiable qualification evidence. It writes
patches and a retained qualification report only; it never edits consumer checkouts.
Its shell recognizer accepts flat literal calls with differing whitespace/quotes,
rejects unsupported dynamic/compound forms, and compares the complete plan against
the literal checkout repository inventory. It cannot silently discard an unknown
call or omit a checked-out prerequisite.
Review/apply those patches as the follow-up in the seven consumer PRs. Each patch
sets literal `JACKSON3_QUALIFIER_REVISION` and `JACKSON3_CORE_REVISION` values and
replaces the copied shell resolver with `resolve_jackson3_consumer_sources.py`
from the policy checkout. Core resolution performs no branch/PR lookup; only an
explicit full-SHA override may select another qualified candidate. Remaining
prerequisite migration branches, where still needed, use the one shared tested
resolver. Subsequent pushes to the open core branch, branch deletion or a squash
cannot move these literal pins. A squash is handled by selecting its actual
reviewed master commit during promotion, not by guessing its SHA in advance.


## Retention and scheduled renewal

Qualification proof artifacts explicitly retain for 90 days. The proposed weekly
`jackson3-qualification-refresh.yml` coordinator reads only registered immutable
tuples from `scripts/jackson3-promoted-qualifications.json` on master. It verifies
that both policy and candidate are master ancestors and that the fixed policy ref
still points to the registered SHA, then dispatches the existing qualification
workflow with the same core, policy and yaml-rule/http-client SHAs. It does not
resolve moving heads or change pins. One broken registered ref does not prevent
other valid pairs from being queued; errors still fail the coordinator visibly.
Dispatch success is not qualification success: inspect the resulting qualify and
record jobs. Earlier successful retained proofs remain usable after a failed
refresh, giving the 90-day retention window as recovery time.

The promotion preparer emits `light-4j-refresh.patch` in addition to the seven
consumer patches, preserving existing registry entries. It derives the new tuple
from verified qualification evidence and refuses to overwrite a pair with different
prerequisites. Land that registration patch on master and run the coordinator from
master; verify its resulting qualification before adopting the consumer patches.
The registry commit does not alter either literal consumer pin and therefore does
not require a new policy/candidate adoption cycle. Retire an entry only after no
consumer relies on it. Keep Actions and the schedule enabled, check failed refresh
runs, and re-enable scheduling if GitHub disables it for repository inactivity.
This is automated renewal with a retention grace period, not permanent proof
storage or a guarantee against an indefinitely disabled/failing CI service.

The registry is deliberately empty before promotion. Current green checks in the
seven consumer PRs still use their earlier policy and do **not** qualify this
head's tolerance, parser or rejection rules. Until actual reviewed master commits
exist and the promotion/registration follow-up is applied, both adoption and its
scheduled lifecycle remain pending. No other open-branch pin is substituted.
