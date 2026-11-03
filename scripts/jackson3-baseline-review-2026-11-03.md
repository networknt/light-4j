# Jackson 3.1.8 test-corpus baseline review

This proposal raises test coverage floors using the retained candidate reports from
[run 41688412346](https://github.com/networknt/light-4j/actions/runs/41688412346),
for candidate `e77455b400f4319c41eb19aaa06c780200dee48e` and policy
`f0d2100f972eec765b2b276e7ace38922394f0a6`.
The full `qualify` job succeeded; the `record` job rejected stale baselines.
The overall run failed and did **not** produce reusable qualification evidence.

All 83 report modules are present: 1,571 tests, 89 skipped, 1,482 executed,
zero failures/errors. Artifact `candidate-reports-attempt-1` (ID 13257171875)
has ZIP SHA-256
`b1df44cda69dbddc0b8246dd078fff862e2649945c3a6779bf0de8b815131b23`.
The downloaded ZIP matches GitHub's digest and every extracted XML report byte.
`scripts/jackson3-test-baseline-evidence.json` retains the complete observed
snapshot and distinguishes the successful qualify job from the failed recorder.

| Module | Old executed baseline | Observed executed | Old floor | New floor |
|---|---:|---:|---:|---:|
| client | 69 | 222 | 65 | 210 |
| config | 123 | 132 | 116 | 125 |
| decryptor | 7 | 19 | 6 | 18 |
| ingress-proxy | 39 | 42 | 37 | 39 |
| security | 69 | 70 | 65 | 66 |
| server | 29 | 32 | 27 | 30 |
| server-config | 7 | 23 | 6 | 21 |
| service | 26 | 27 | 24 | 25 |
| token-exchange | 62 | 63 | 58 | 59 |

The five growth-guard failures are client, config, decryptor, server and
server-config. Four smaller increases are recorded as well, so the policy matches
the complete observed corpus rather than combining counts from different runs.
No baseline or floor is lowered. The existing five-percent tolerance, disabled
module reasons, module coverage checks and baseline-growth rejection are unchanged.

Representative source/report review:

- Client: connection lifecycle, I/O ownership, HTTP/2 cancellation and pool
  retirement suites account for the larger corpus; 222 executed tests.
- Config: `ConfigMixedRepresentationTest` (20), `DecryptConstructorTest` (5),
  password handling (4) and feature-profile regression remain executed; 132 total.
- Decryptor: `AESSaltSecurityTest` (9) and `ManualDecryptorLifecycleTest` (3)
  supplement existing decryptor tests; 19 executed.
- Server config: `ServerOptionRegistryTest` contributes 16 tests; 23 executed.
- Server: TLS password and YAML/config/status lifecycle tests; 32 executed.
- The smaller ingress-proxy, security, service and token-exchange increases are
  retained in the complete module snapshot, including unchanged skip counters.

Validation: 111 qualification/promotion Python tests pass. The bounded production
report parser accepts all 306 XML files, module validation passes, and
`require_current_baselines` passes with this proposed inventory.

## Required promotion order

Follow [JACKSON3-QUALIFICATION.md](JACKSON3-QUALIFICATION.md#promote-after-review-and-merge).
Review and merge the runtime fixture repair (#2842) and this policy update first.
Select the actual resulting master policy SHA; do not point consumers at this
open branch. Create/protect its new immutable policy ref, qualify the selected
core with the recorded prerequisite SHAs, then complete registration and reviewed
acceptance before updating consumer policy pins. The existing protected policy
ref must not move. This proposal does not weaken or bypass the failed gate.
