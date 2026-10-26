# Reusable core qualification

## Registration safeguards and pending acceptance

These gates are enforced by the tooling: the coordinator and preparer verify live
effective protection and the underlying active, exact-ref, no-bypass ruleset.
Read-only `policy-protection` CI exercises these API calls with `GITHUB_TOKEN`.
Only explicit smoke mode bypasses protection, as well as the master-ancestry gate;
it cannot register or adopt a consumer tuple.

The preparer defaults to `--stage registration`, which emits no consumer patches.
`--stage adoption` additionally requires the exact tuple to be registered on fetched
master and its accepted record to exist on fetched master, not just locally or on
a PR branch. Each Markdown acceptance record includes one JSON metadata block
using [the documented format](jackson3-acceptance/README.md). The tool checks its
decision, tuple, ruleset ID, registration ancestry, successful distinct coordinator
run IDs, proof artifact/digest and zero dispatches in the reuse run. This is a gate
on reviewed data, not independent authentication of the record or its review.
Normal qualification verification still verifies the retained proof separately.

For **every** promotion, the promotion operator must protect the new fixed policy
branch immediately after creating it and **before qualification or registration**.
Create an active branch ruleset scoped to that exact ref, prohibiting `update`,
`deletion` and `non_fast_forward`, with an empty `bypass_actors` list. Do not move
an existing policy ref or replace its protection to reuse it. Verify the ref's
SHA and effective rules through `GET /repos/networknt/light-4j/rules/branches/{branch}`;
retain the ruleset ID and API verification in the acceptance record below.
If permissions or repository policy prevent protection, stop promotion until a
repository administrator supplies it. Creating a new branch alone is insufficient.
Ruleset 25899868 protects only the current policy; it does not protect future refs.

The promotion PR author owns preparation of the post-merge acceptance record;
a maintainer other than its author reviews it through the ordinary PR process.
Use one follow-up acceptance PR for this record, rather than a separate
PR per run. Store one Markdown record per tuple at
`scripts/jackson3-acceptance/<full-policy-sha>-<full-core-sha>.md`.
Use these headings and required fields consistently:

- `Tuple`: full policy/core SHAs, literal policy ref, and each prerequisite repo/SHA.
- `Registration`: registration PR URL and merged master SHA.
- `Ref protection`: ruleset ID, verification UTC time, actual ref SHA, the three
  effective rule types, and confirmation that the active ruleset has no bypass actors.
- `Forced renewal`: coordinator run URL/ID, dispatched producer run URL/ID,
  conclusions, verified tuple, proof artifact ID, SHA-256 and expiry UTC.
- `Fresh-proof reuse`: subsequent ordinary coordinator run URL/ID, conclusion,
  and log evidence that no producer was dispatched and the fresh proof was reused.
- `Decision`: operator account, verification UTC time, `accepted` or `blocked`,
  remaining limits (including unobserved cron execution), and any rollback PR URL.

An accepted record requires both successful runs and the verified proof; do not
fill pending fields with assumed results. Failed attempts may be recorded as
blocked with links to their preserved logs. The reviewed record must be merged
before consumer adoption. This record documents workflow-token acceptance, not
an independent attestation or deployment qualification.

The registered `jackson3-qualification-policy-f0d2100f972eec765b2b276e7ace38922394f0a6`
branch is protected by active repository ruleset **25899868**, which prohibits
updates, deletion and non-fast-forward updates with no bypass actors. Effective
branch rules were checked through GitHub's API. Administrators can still change
repository rules; the trust boundary includes that administration. SHA checks
remain required before dispatch and when accepting evidence.

The obsolete `jackson3-qualification-policy-27aed6a44` branch must remain until
the seven consumer workflows stop referencing the earlier policy. After their
promotion, verify the remote consumer heads no longer reference that policy,
check no registry entry uses the obsolete ref, and delete only that obsolete
branch. Do not move or delete the registered policy branch.

Post-merge workflow-token acceptance is **pending**, not established by this PR.
Before consumer adoption, retain the forced coordinator run ID, its dispatched
producer run ID, the verified tuple and proof artifact digest, and the subsequent
ordinary coordinator run ID showing fresh-proof reuse with no producer dispatch.
Commit these results in a reviewed follow-up acceptance record linked to the
registration PR; consumer promotion remains blocked until both checks pass and
that record exists. Manual runs exercise the workflow token and event router;
they do not demonstrate that GitHub's cron scheduler has fired.

If either post-merge check fails, keep the consumers on their existing pins.
Preserve failed run IDs and logs. Prepare a reviewed rollback PR removing only
this tuple from `scripts/jackson3-promoted-qualifications.json` (leaving the policy
ref, protection and prior proof intact), then rerun the ordinary coordinator to
confirm the removed tuple is not dispatched. Alternatively, repair the cause in
a reviewed PR and repeat both acceptance runs before adoption. A failed
registration must not be reported as successful renewal.

The refresh workflow uses one unconditional coordinator step with explicit
schedule/manual routing. Its shell is executed by unit tests for scheduled input
absence, scheduled false input, both manual boolean values and invalid events.

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

The inventory stores a reviewed executed-test baseline for 83 modules: 1,283 total
for the native-fix corpus. The original 1,276-test corpus was measured at
`27aed6a44a91d40a4a590be563b3d6e2fbc3415b` in
[40266655138](https://github.com/networknt/light-4j/actions/runs/40266655138).
The merged runtime candidate `97b2359b92ed3e1d05cac04f2da8760aa954e49d` ran
1,372 tests with 89 skips and no failures/errors in the successful ordinary push
[40356825612](https://github.com/networknt/light-4j/actions/runs/40356825612).
The committed [report snapshot](jackson3-test-baseline-evidence.json) retains every
module's observed counters, baseline/floor comparison, prerequisite SHAs and run
and artifact provenance after the remote report artifact expires. Its ZIP SHA-256
matches GitHub's artifact digest, and every parsed XML report was byte-compared
with that archive. The regression test compares policy baselines to these observed
counters; it does not duplicate aggregate totals or module-count magic numbers.

This snapshot is a reviewed ordinary-CI measurement, not reusable qualification
evidence or an independent attestation of test honesty. The read-only preview does
not authenticate arbitrary supplied reports; the recorded GitHub metadata/digest
establish which reviewed artifact supplied these counters. Fresh policy-pinned
qualification and its normal validation remain required before registration.
The reviewed changes are:


| Module | Previous baseline | Native-fix baseline | Executed floor |
| --- | ---: | ---: | ---: |
| client-config | 4 | 6 | 5 |
| jackson-defaults | 3 | 6 | 5 |
| token-exchange | 60 | 62 | 58 |

The tolerance still allows a loss of `max(1, ceil(5% * baseline))` per module,
with a floor of one executed test in each required module. Examples: 100 -> 95,
3 -> 2, 1 -> 1. Effective floors now total 1,177. Both baseline and effective floor
are stored, and tooling validates the formula. The disabled request-transformer
suite remains the explicit zero-execution exception. Raising these baselines is a
reviewed policy change; it does not move any consumer pin automatically.

Small consolidations or environment-dependent skips within the allowance need no
policy change. Adopting a different candidate still requires its own qualification;
that is separate from changing the policy pin. Drops below the allowance or
intentional module changes require review. Missing reports, empty modules and a
large suite reduced to one test remain failures. Reports are discovered recursively;
missing or unexpected modules cannot be hidden by a positive reactor total. This
is a count/coverage signal, not a claim about assertion quality.

Test additions also require protection. Both the recorder and verifier reject a
candidate whose observed executed count, after the same tolerance, exceeds the
reviewed baseline. Promotion repeats this guard before writing any patches.
For example, growth from 100 to 200 requires reviewing representative counts and
raising the baseline; a reviewed baseline of 200 then rejects a fall back to 100.
Counts from 95 through 105 around a baseline of 100 do not force a policy update.
This protects growth beyond the tolerance, not each individual added test.
No run automatically ratchets a baseline, and renewal never changes an inventory.
Review baseline changes alongside the new candidate, qualify the reviewed policy,
and promote it deliberately. Existing immutable candidates keep their existing
policy and do not need repinning merely because another candidate gained tests.

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

The inventory now covers the merged native-fix candidate
`97b2359b92ed3e1d05cac04f2da8760aa954e49d`: client-config 6, jackson-defaults 6,
and token-exchange 62 executed tests. The earlier 4/3 growth-guard obstruction has
been addressed by this reviewed baseline proposal. After this policy change is
reviewed and merged, use its actual master SHA as P; do not repin consumers to the
open policy branch. The following order applies to this first promotion and to
future candidate updates (the stack and ordinary-CI measurement steps are already
complete for this candidate).

Promotion has this required order:

1. Review/merge the policy content and the OAuth/token stack (#2822/#2823).
   Fetch master and select an immutable common candidate **C** containing all
   required runtime fixes. Do not choose C before the stack is assembled.
2. Run ordinary full-core CI for C and retain its `source-build-reports` artifact,
   exact candidate/run identity, recorded prerequisite SHAs and successful result.
   Ordinary CI remains available even when reusable evidence recording would fail
   the growth guard. Inspect only C's reports under the extracted `consumer/`
   directory; do not mix prerequisite-module reports into the inventory.
3. Measure all modules from those reports, review representative executed counts
   and their tests/skips, and prepare an inventory-only baseline update for every
   material increase. Review/merge that update on master as policy **P**. Recompute
   each changed `minimum_executed` with `minimum_executed(baseline_executed)`;
   do not lower other baselines automatically or bypass the growth guard. Check
   the reviewed inventory against C's complete reports. Select P only after this
   baseline review, rather than treating the pre-stack #2821 inventory as final.
4. Create the fixed policy ref at the actual reviewed master SHA P. Qualify C at P
   with the same recorded prerequisite SHAs. Require both qualify and record to
   pass, then verify/download the retained proof. An expected growth rejection
   before step 3 is a missing baseline review, not a reason to rerun unchanged P.
5. Prepare/review the registration and seven consumer patches using P and C.
   Land registration first, run the coordinator from master and require a verified
   healthy-proof result, then adopt the consumer patches. No open-branch repin.

P and C are separate commits: the inventory-only policy update may follow the
runtime candidate without changing which runtime source is qualified. A later
push, branch deletion or squash cannot silently select either SHA.

For a read-only baseline-growth preview from retained ordinary CI reports:

```sh
python3 scripts/preview_jackson3_test_baselines.py \
  --reports-root /path/to/extracted/source-build-reports/consumer \
  --output /path/to/baseline-preview.json
```

The tested script prints JSON when `--output` is omitted. It reports every observed
module's tests, skips and executed count, current baseline/floor, and review issues.
New modules, missing modules and below-floor counts are diagnostic fields rather
than exceptions that hide the observed counts. Empty report directories likewise
show missing coverage, never accepted evidence. Unsafe/malformed XML and invalid
counters still fail through the same bounded parser used by qualification.

The preview neither edits an inventory nor qualifies the candidate. Check the
reported gaps, authenticate the run/candidate and review representative counts
before changing the policy. The recorder and verifier still reject coverage/floor
differences; this read-only measurement path does not waive those checks. It avoids
the circular requirement to obtain accepted reusable evidence before measuring a
candidate that needs an inventory update. The reviewed policy must still produce
fresh reusable proof afterward.

Create a stable policy ref `jackson3-qualification-policy-POLICY_FULL_SHA` pointing
to the exact reviewed policy SHA. Then dispatch qualification once for that
candidate at that fixed ref. A dispatch from a later moving master head does not qualify
an older policy pin. Check the run and download its proof before applying the
consumer follow-up.

From the reviewed master tooling checkout, prepare refresh registration first:

```sh
python3 scripts/prepare_jackson3_consumer_promotion.py \
  --stage registration \
  --policy-sha REVIEWED_POLICY_MASTER_SHA --core-sha QUALIFIED_CORE_MASTER_SHA \
  --master-checkout /path/to/fetched/light-4j \
  --worktrees /path/to/migration/worktrees --output /path/to/new/patch-directory
```

This stage creates only the registry patch, proof report and instructions. After
registration merges, run forced renewal and then normal refresh, review and merge
the acceptance record, and fetch master. Repeat the command with `--stage adoption`
and a new output directory to generate the seven consumer patches. The tool checks
live protection again and runs the proof verifier from a clean local clone of the
selected policy commit, even when the promotion tooling is newer than that policy.

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

Qualification proof artifacts explicitly retain for 90 days. The weekly
`jackson3-qualification-refresh.yml` coordinator is a health check, not a weekly
reactor rebuild. It reads registered immutable tuples from
`scripts/jackson3-promoted-qualifications.json` on master. Each tuple must remain
on master and its fixed policy ref must still point to the registered SHA.

The coordinator checks retained proof with a clean checkout of **that tuple's
policy**, including its inventory, rather than imposing the current master's
inventory on old pins. A proof with more than 30 days remaining needs no build.
Missing/rejected proof or 30 days or less remaining triggers qualification of the
same core, policy and prerequisite SHAs. All due tuples are queued before waiting.
Dispatch requests `return_run_details` and must return the created run ID. While
that run is queued or executing, the coordinator performs only one status lookup
per minute. It checks the run's policy/event/workflow identity and reports failed,
cancelled or timed-out runs immediately. API/rate-limit errors are failures, not
silent retries. Only a successful completed run triggers one final full check with
the tuple's pinned verifier; its proof must belong to that dispatched run and have
more than 30 days left.

The 80-minute absolute budget starts before registry/ref checks and initial proof
inspection, so it includes setup, dispatch, runner queueing, polling and final
verification. API/dispatch subprocesses are capped at 30 seconds; full verification
is capped at 180 seconds, each reduced to the remaining overall budget. A final
check finishing after the deadline cannot turn the coordinator green. The job's
95-minute timeout leaves outer-runner/checkout and reporting margin. A successful
dispatch alone never makes it green. Producer failures therefore leave a failed
coordinator check even when the bot-triggered producer sends no notification.
Repository maintainers must monitor failed coordinator checks; this does not
promise email delivery. Earlier successful proof remains usable during recovery.
A healthy tuple normally rebuilds about every 60–67 days, rather than weekly.

The promotion preparer emits `light-4j-refresh.patch` in addition to the seven
consumer patches, preserving existing registry entries. It derives the new tuple
from verified qualification evidence and refuses to overwrite a pair with different
prerequisites. Land that registration patch on master and run the coordinator from
master; require its verified healthy-proof result before adopting the consumer patches.
The registry commit does not alter either literal consumer pin and therefore does
not require a new policy/candidate adoption cycle. Review the registry at every promotion and retire superseded entries only after
checking that no consumer relies on them. Old pins are not silently retired;
registered supported tuples retain an occasional renewal cost. Keep Actions and the schedule enabled, check failed refresh
runs, and re-enable scheduling if GitHub disables it for repository inactivity.
This is automated renewal with a retention grace period, not permanent proof
storage or a guarantee against an indefinitely disabled/failing CI service.

### Verify registered dispatch with the workflow token

After the registration follow-up is reviewed and merged on master, dispatch the
coordinator explicitly with its manual `force_renewal` input:

```sh
gh workflow run jackson3-qualification-refresh.yml --repo networknt/light-4j \
  --ref master -f force_renewal=true
```

This uses the workflow's `GITHUB_TOKEN` with `actions: write`, rather than the
operator's credentials inside the job. The production CLI `--force-renewal`
bypasses only the proof-age shortcut. It still requires canonical registered
policy refs, exact SHAs and master ancestry; it cannot be combined with smoke
mode. It dispatches the registered tuples even when their proof is fresh, then
requires successful run status and a verified exact-run replacement proof.
The input defaults to false and is not enabled by the weekly schedule.

Wait for both the producer qualification and coordinator job to pass before
consumer adoption. Then run the coordinator normally to verify that fresh proof
is reused without dispatching another reactor. This checks the registered-master
workflow token path, which the pre-merge operator-credential smoke cannot prove.
Keep the core/policy pins unchanged throughout this registration verification.

### Manual coordinator smoke before promotion

The empty registry is intentional; it should not be populated with an unapproved
branch just to test dispatch. Instead, an operator can run the **same coordinator**
against a separate one-tuple JSON file using `--smoke-tuple`:

```json
{
  "schema": 1,
  "qualifications": [{
    "policy_sha": "REPLACE_WITH_PROPOSED_POLICY_FULL_SHA",
    "core_sha": "REPLACE_WITH_CANDIDATE_FULL_SHA",
    "policy_ref": "fix-jackson3-mapper-policy",
    "source_refs": {
      "yaml-rule": "REPLACE_WITH_RECORDED_YAML_RULE_FULL_SHA",
      "http-client": "REPLACE_WITH_RECORDED_HTTP_CLIENT_FULL_SHA"
    }
  }]
}
```

```sh
python3 scripts/refresh_jackson3_qualifications.py \
  --smoke-tuple /path/to/throwaway-smoke-tuple.json > /path/to/smoke.log 2>&1
```

This explicitly labeled manual test accepts a proposed branch/tag after checking
that it still resolves to the exact policy SHA. It skips the master-ancestry and
proof-age prechecks so one dispatch really occurs, then uses the production
dispatch, returned run ID, status/metadata polling, timeout handling and final
pinned-policy proof verification. It permits exactly one tuple and cannot read the
promotion registry via this flag. It never writes a registry or consumer pins.
Check for the returned run ID, completed `Verified renewed proof` and `SMOKE PASSED`
messages and a zero process exit status. Retain the log and run URL. If no run ID
returns, inspect Actions before trying again; the first request may have created
a run. The smoke is not scheduled, so an unsuccessful experiment does not create
weekly duplicate dispatches.

A passing smoke proves the real API path, not master approval, registration or
consumer adoption. Normal scheduled execution has no smoke flag and keeps all
master/canonical-ref/age checks. The actual registered-master lifecycle must still
be verified after review and registration.

The registration proposal records the reviewed master policy
`f0d2100f972eec765b2b276e7ace38922394f0a6` and runtime candidate
`97b2359b92ed3e1d05cac04f2da8760aa954e49d`, with the prerequisite commits
selected by their policy-pinned qualification. The policy ref remains immutable;
registration and renewal do not change either policy or runtime identity.

The seven consumer PRs still carry their earlier policy until the registration
follow-up is reviewed/merged and the registered-master workflow-token verification
passes. Their earlier green checks are not evidence of this promotion. After that
verification, apply the prepared patches with these literal policy/core pins.
No new open-branch pin is substituted, and registration by itself does not adopt
anything in a consumer. The actual registered lifecycle remains a post-merge gate.
