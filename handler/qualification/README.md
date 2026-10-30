# Continuation allocation evidence for issue #2820

Build the focused tests first, then run from the repository root:

```sh
mvn -pl handler -am test -Dtest=HandlerContinuationTest -Dsurefire.failIfNoSpecifiedTests=false
python3 handler/qualification/run-allocation.py
```

`allocation.json` records raw samples, warmed per-resume medians, JDK details and
source hashes. The standalone probe reuses one captured runnable and increments a
volatile counter so the callback cannot disappear. It verifies caller MDC restoration.
It does not measure capture/registration, transport, logging, monitor contention,
or end-to-end gateway throughput. JIT and machine variation affect the figures.

A resume still snapshots the caller MDC and restores it on exit, including when
entry already matches the request map: the callback can change MDC. That restoration
is required for shared I/O/worker threads. Nested calls for the same policy reuse
the scope. Proxy request state caches its captured handler and runnable so callbacks
do not recreate those adapters. One frame is allocated per outer resume.

These local figures quantify remaining allocation; they do not close a gateway
performance qualification gate or establish a zero-allocation path.
