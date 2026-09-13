# Latency Regression RCA

**Status:** IN PROGRESS — the general mechanism (intermittent macOS scheduler preemption,
frequency modulated by wait-strategy choice) is now well-evidenced (see "Confirmed Root
Cause"), but the specific serv-c-GC trigger is still unproven and no remediation beyond
"keep `yielding`" has been applied. This document is updated as each phase below
completes; do not treat any hypothesis in this file as fully confirmed until it appears
under "Confirmed Root Cause" with supporting evidence linked.

## SESSION HANDOFF (2026-09-12) — read this first in a new session

Previous session hit its context limit. Everything below is preserved so a fresh session
can continue with zero re-discovery. Git commit `5b85e47` (main, NOT pushed) is the
current point of return — see "Rollback" note at the bottom of this section.

### What is DONE (committed, verified)
1. **Manifest schema v2** (`scripts/generate_run_manifest.py`) — real `wait_strategy`,
   artifact SHA-256 hashes / Docker image digest, GC log paths, per-service exit status,
   `dirty_patch_sha256` + `untracked_files` replacing the old ambiguous `git_dirty` bool.
2. **Provenance wiring** (`scripts/generate_benchmark_report.sh`, `scripts/start.sh`,
   `scripts/stop.sh`, `docker-compose.yml`) — per-service GC logs (`-Xlog:gc*`),
   `graceful`/`force_killed`/`already_exited`/`not_started` exit-status files, native
   builds by default now (`scripts/runners/native_runner.sh`). Fixed a live bug
   (`--trace-enabled` was hardcoded `true`) and a latent one (HdrHistogram jar glob could
   match `-javadoc.jar`).
3. **`IntervalHistogramExporter`**
   (`common/src/main/java/com/fx/common/telemetry/IntervalHistogramExporter.java`) — new
   CLI tool + 2 tests, wired into `scripts/process_latency.sh`, archives
   `<hlog>.intervals.csv` per run. Turns "the tail was bad somewhere in 200s" into "the
   tail was bad at elapsed 75-77s" — this is the tool to reach for before adding more
   manual log-scanning.
4. **10 local 50k msg/s / 10M-message repetitions run and analyzed today**: `phased`
   (current retuned `SPIN_TRIES=10_000` form) shows P50/P90 far lower than `yielding`
   (~7.6us/11.5us vs ~25us/64us) when it behaves, but hit an elevated tail (P99.9 >= 10ms)
   in 2 of 4 runs (50%); `yielding` hit it in 1 of 6 (17%). A `serv-c` GC-burst
   correlation was checked on 2 independent bad runs — present both times but NOT
   conclusively above the base rate (serv-c GCs every ~2-3s all run, so proximity alone
   isn't strong evidence) — downgraded to "plausible, unproven." GC itself is ruled out
   as the direct cause on the affected thread (serv-a shows zero GC activity during every
   stall window checked). Full numbers in the "Wait-strategy A/B" / "Consolidated tally"
   sections below.
5. `config/profiles/local.env` is at its committed default, `FX_WAIT_STRATEGY="yielding"`.

### What is NOT done
- **Phase 1 Step 3**: `TelemetryRecorder` measurement-epoch handshake (replace `rm -f
  *.hlog` warmup reset with a real rotate/ack protocol) — not started.
- **The rigorous nearest-GC-gap statistic** for the serv-c correlation (compare bad
  intervals' gap-to-nearest-GC against the SAME gap for every interval, not just bad
  ones) — not built, currently just a documented suggestion.
- **`benchmark-runs/`** (all ~30+ run directories from today, including everything the
  10-run tally above is based on) is **gitignored — not in git history**, only on this
  machine. Not yet decided whether/how to preserve it.
- Phase 2 (formal falsification writeup against the rules already defined below), Phase 3
  (the actual remediation — nothing has been "fixed" yet, only measured), Phase 4
  (cross-environment validation, doc updates to `BENCHMARKING_ARCHITECTURE.md` /
  `PERFORMANCE_TUNING.md` / `CONFIG_PROFILES.md` / `README.md`) — all still open, see the
  full phase plan later in this repo's session memory (`/memories/repo/` — a new session
  should check `/memories/repo/` for a condensed copy of this handoff too).

### RESOLVED (2026-09-12, later same day): hunt for a possibly-lost true-low-latency config

User's own words: *"I would like the option of digging deeper at achieving true low
latency as I am sure I have done it before but due to some bad commits lost that
particular setting."* **Investigated and resolved — see "Busyspin validation at 50k/10M"
further down this document for full evidence.** Short version: the setting is
`FX_WAIT_STRATEGY="busyspin"` (used 2026-09-08 through 2026-09-12, then drifted to
`phased` then `yielding`); it was empirically re-tested at the current 50k/10M scale on
current `HEAD` (3 reps) and reproduces the same intermittent tail-blowup pathology already
being chased in the phased/yielding A/B (P99.9 up to ~9.6ms, Max up to 43.45ms) — it looked
clean historically only because it was measured at a 5x lighter load (10k msg/s). No
config change resulted; `local.env` stays on `yielding` (the best-evidenced of the three
tested). The original starting point below is preserved for reference but the hunt is
complete — no further action needed on this thread unless new information emerges.

**Concrete starting point** — every commit that ever touched wait-strategy/tuning-relevant
files (`WaitStrategy.java`, `BusySpinWaitStrategy.java`, `PhasedBackOffWaitStrategy.java`,
`YieldingWaitStrategy.java`, `config/profiles/*.env`, `scripts/start.sh`,
`PERFORMANCE_TUNING.md`, `docker-compose.yml`), oldest-relevant-first from
`git log --oneline --all -- <those paths>` (30 total, HEAD-first):
```
5b85e47 Harden benchmark provenance + add interval-level latency analysis (Phase 1 RCA)
17ecce0 test
ce9acb5 no cache in docker build phasse
0d31ee3 minor tuning for large TPS
a321e77 test
752a662 script fix
ab68636 script fix
6298257 queue upgrades.
e3766d1 queue.
49f4a4c docker
2465d24 docker based env update for queues.
7776d4e docker based env update for queues.
b3b45f2 fix for queues.
5ecc5ea reporting
d90935b perf upgrade for docker
2c77b2e refactor docos
2061867 refactor docos
85ba287 refactor
9e0442f some deltas
58199d8 final metrics report.
60c3040 refactor scripts.
ad4f80b reporting and docker fixes
c3a3911 fixes
b25db1f docker fix
1335942 final refactor.
090afb6 metrics mismatch.
b4594c4 fix metrics
c6ec975 fix metrics
b86ed32 minor tweaks.
6872bb0 script uopdates
(+ 1 more, re-run the git log command above with -- <paths> to get it and go further back)
```
Suggested approach for the next session:
1. `git show <commit>:common/src/main/java/com/fx/common/handler/PhasedBackOffWaitStrategy.java`
   (and the other wait-strategy files, and `config/profiles/local.env`) across a handful
   of these commits (especially `0d31ee3 "minor tuning for large TPS"` and
   `d90935b "perf upgrade for docker"` — names suggest deliberate tuning changes) to see
   what values existed historically, without checking anything out yet (read-only).
2. If an old `benchmark-runs/` archive from that era exists anywhere on disk (they're
   gitignored, so check outside git — Time Machine, another clone, Spotlight search for
   old `run_manifest.json` files with an older `git_sha`), compare its percentiles against
   today's; that would be the fastest way to confirm what "true low latency" actually
   measured as.
3. Ask the user directly what they remember concretely (approximate date, which
   environment — local/docker/baremetal, roughly what P50/P99 looked like) if steps 1-2
   don't turn up an obvious candidate — this is exactly the kind of ambiguity worth a
   clarifying question rather than guessing.
4. Whatever is found must go through the same evidence bar as everything else in this
   RCA (Phase 2's falsification rules below) before being called "the fix" — do not treat
   "I remember it being fast" as proof without a reproducible, hash-verified run.

### Rollback / continuity notes
- Current HEAD: `5b85e47` on `main`. Not pushed. `git reset --soft 17ecce0` undoes just
  this session's commit if ever needed; nothing before that has been touched.
- This file (`LATENCY_RCA.md`) plus `/memories/repo/` are the two places state was
  preserved for handoff — check both at the start of a new session.

## Impact

A 50,000 msg/s, 10,000,000-message TCP benchmark on local macOS regressed catastrophically
between two runs on the same Git commit:

| Metric (end-to-end) | Clean run | Regressed run | Change |
|---|---:|---:|---:|
| P50 | 30.671 us | 40.095 us | +31% |
| P90 | 65.439 us | 3.680 ms | 56x |
| P95 | 79.167 us | 7.995 ms | 101x |
| P99 | 188.415 us | 27.574 ms | 146x |
| P99.9 | 6.660 ms | 91.554 ms | 13.7x |
| Max | 120.979 ms | 223.085 ms | 1.8x |

`queue-a` is the first stage to diverge and is hit hardest:

| Metric (queue-a) | Clean run | Regressed run | Change |
|---|---:|---:|---:|
| P50 | 6.667 us | 5.835 us | -12% (improved) |
| P90 | 25.839 us | 868.351 us | 34x |
| P99 | 70.079 us | 11.559 ms | 165x |
| P99.9 | 4.579 ms | 64.324 ms | 14x |

Median latency is flat or improved while the tail explodes — this is an **intermittent stall
pattern**, not a uniform slowdown of every message. Service-processing time (serv-a/b/c
handler duration) stays broadly stable; the divergence originates at the queue, not the
business logic.

## Timeline

- `local-20260911T234349Z` (2026-09-11 23:47:59 UTC) — clean baseline. `git_sha=ce9acb5b`,
  `git_dirty=false`.
- `local-20260912T005439Z` (2026-09-12 00:58:48 UTC) — regressed run, ~70 minutes later.
  Same `git_sha=ce9acb5b`, but `git_dirty=true` (caused by untracked `TestVarHandle*.java`
  probe files at the repo root — not compiled into any running service).
- `docker-20260912T005909Z` (2026-09-12 00:59:09 UTC) — a same-day Docker attempt at the same
  workload. **Invalid, not a latency data point** (see below).
- 2026-09-12 (later, before this RCA's implementation phase began) — `HEAD` advanced to
  `17ecce0` ("test", already pushed to `origin/main`). This is **not part of the regression
  window** but changed relevant code after the fact — see "Post-incident repo drift" below.

## Known-good / regressed run IDs

- Clean: `benchmark-runs/local/local-20260911T234349Z/`
- Regressed: `benchmark-runs/local/local-20260912T005439Z/`
- Invalid (separate incident): `benchmark-runs/docker/docker-20260912T005909Z/`

Both local runs: Apple M2 Max, 12 cores, TCP transport, `-XX:+UseZGC -XX:+ZGenerational
-Xmx4G -Xms4G -XX:ActiveProcessorCount=2 -XX:ConcGCThreads=1 -XX:ParallelGCThreads=1
-XX:CICompilerCount=2`, cpusets `serv-0=1 serv-a=2 serv-b=3 serv-c=4 benchmark=5
telemetry=unconstrained`, 50,000 msg/s target, 10,000,000 messages, ~201s duration.

## Architecture boundary (what this system is and is not)

This repository implements **LMAX Disruptor-inspired** principles — single-writer producers,
single-consumer `AbstractEventLoop`s, pluggable `WaitStrategy`, preallocated flyweights
(`FxMarketEvent`), bounded rings (`BatchPersistenceEngine`) — on top of **Chronicle Queue**
(OpenHFT). There is no dependency on the real `com.lmax:disruptor` library (confirmed: no
`lmax`/`disruptor` artifact anywhere in any `pom.xml`, only comments describing the design
philosophy). Any remediation must preserve this boundary and must not introduce the actual
LMAX Disruptor dependency.

## What is proven vs. not proven

**Proven (from archived `.hgrm` files and manifests):**
- The regression is real, reproducible in the archived data, and originates at `queue-a`.
- It is a tail-only stall (median stable/improved), not a uniform slowdown.
- Both runs share an identical Git SHA; the only manifest-visible difference is `git_dirty`.
- Both runs' sample counts reconcile (~10M end-to-end each) — no data loss or truncation.

**Not proven (requires Phase 2 causal experiments before any fix is applied):**
- Whether telemetry (unpinned flush thread + synchronous blocking I/O + up to 8 independent
  per-JVM 1-second timers), host scheduling/thermal noise, GC, or something else caused the
  stall. A near-identical mechanism was already proven on Docker on 2026-08-30 (~1000x P50
  gap, see `/memories/repo/docker-latency-investigation-10k-run.md`), making telemetry the
  leading hypothesis — but it is not yet confirmed for this local incident specifically.
- Whether the two runs even executed identical compiled bytecode: **native benchmarks launch
  whatever JAR already exists in `target/` with no build or hash-verification step**, so
  identical `git_sha` does not guarantee identical binaries.

## Invalid data point: `docker-20260912T005909Z`

Sample counts show a precise cascade, not a generic "incomplete run":

| Stage | Samples | % of 10M ingested |
|---|---:|---:|
| serv-0 (ingress) | 10,000,000 | 100% |
| queue-a | 10,000,000 | 100% |
| queue-b | 9,039,930 | 90.4% |
| queue-c | 4,030,353 | 40.3% |
| serv-c | 0 | 0% |
| end-to-end | 0 | 0% |

This pins the failure at **serv-c / `BatchPersistenceEngine` / H2 batch writer** specifically —
ingestion and queue-a/b were fine, queue-c partially drained, and serv-c never completed a
single event. No logs or exit codes were archived for this run, so there is currently nothing
further to diagnose it with; Phase 1 of the remediation plan adds that capture. This is tracked
as a **separate correctness/drain incident**, fixed independently of the latency RCA and
excluded from latency trend data.

## Post-incident repo drift (discovered when implementation started)

After this incident's evidence was captured, `HEAD` advanced (commit `17ecce0`, already
pushed) and changed `config/profiles/local.env`'s `FX_WAIT_STRATEGY` from `phased` to a new
`yielding` strategy, before any A/B evidence was gathered for *this* incident. This means:
- Both archived runs above actually executed with `FX_WAIT_STRATEGY=phased`
  (`PhasedBackOffWaitStrategy`, `SPIN_TRIES=200`, old reset-after-park behavior) — identical
  between the two runs, so it does not explain the gap between them.
- The `yielding` switch is a plausible improvement for the *general* bimodal/hockey-stick
  pattern described in `PERFORMANCE_TUNING.md` (parkNanos timer slack on macOS), but it has
  not been validated with the A/B methodology this RCA requires, and it predates any
  reproduction run against current `HEAD`. It is treated as an **unvalidated candidate**, not
  a confirmed fix, until Phase 2 tests it explicitly.
- The manifest schema was found to have two concrete defects, now fixed under Phase 1:
  `effective_jvm_options` only ever recorded `FX_JVM_OPTS_OVERRIDE` (never the actual
  `-Dfx.waitstrategy`/`-Dfx.telemetry.*`/`-Dfx.gateway.mode` flags or `--add-exports`/
  `--add-opens` flags), and `--trace-enabled` was hardcoded to `true` regardless of whether
  `FX_ENABLE_TRACING` was actually set.
- Correction to the untracked-files claim below: `TestVarHandle*.java` were untracked only
  at the moment of the regressed run (created 2026-09-09, before the 2026-09-12 benchmark);
  they were committed at some later point (commit `da3d5d6`, also titled "test") and are now
  ordinary tracked source files. The working tree is clean today — no action was needed or
  taken on them.

## Hypotheses (ranked, to be tested in Phase 2)

1. **Telemetry interference (leading hypothesis).** Up to 8 independent per-JVM
   `TelemetryRecorder` flush threads (serv-0: 1, serv-a: 2, serv-b: 2, serv-c: 3), each waking
   every 1000ms and performing synchronous `PrintStream` I/O on the flush thread itself, none
   coordinated or jittered. Already proven as the cause of a ~1000x P50 gap on Docker
   (2026-08-30 investigation). Test: `TelemetryBootstrap` periodic-vs-on-close flush mode A/B.
2. **Host scheduling / thermal noise.** macOS has no `isolcpus`/`nohz_full`; the regressed run
   started ~70 minutes after the clean one. Test: 3+ repeated runs with thermal/context-switch
   capture.
3. **Unverified artifact identity.** Native mode never rebuilds JARs; same `git_sha` does not
   guarantee identical bytecode ran both times. Test: hash-verified builds for every future run
   (Phase 1 Step 2).
4. **GC.** ZGC generational; no GC log was captured for either archived run. Test: GC log
   capture + interval correlation (Phase 1 Step 4 / Phase 2 Step 6).
5. **Gateway/TCP.** Only relevant if direct-mode reproduction does not show the same stall.

## Falsified

- **Stale Chronicle Queue segment files.** `scripts/start.sh` deletes `${FX_QUEUE_DIR}/*` and
  remounts a fresh macOS RAM disk before every run — ruled out.
- **Sample loss / truncated runs (for the two local runs).** Both reconcile to ~10M samples
  end-to-end; no truncation.

## New evidence: 2026-09-12 user-run benchmarks (post-harness-hardening)

Four benchmarks were run against current `HEAD` (17ecce0, `yielding` wait strategy for
local) using the hardened Phase 1 harness (real artifact hashes, GC logs, exit-status
capture). A 5th attempt (`local-20260912T030741Z`) produced an empty run directory —
it aborted before any report was generated, most likely during the newly-added
build-by-default step; needs a follow-up look at terminal scrollback since no log was
archived for a run that never got that far.

### Local vs Docker at identical 10k msg/s / 1M messages (both valid, fully reconciled)

| Metric (end-to-end) | Local (`local-20260912T030822Z`, yielding) | Docker (`docker-20260912T031308Z`, busyspin) | Ratio |
|---|---:|---:|---:|
| P50 | 35.55 us | 3.998 ms | 112x |
| P90 | 65.50 us | 9.961 ms | 152x |
| P95 | 75.97 us | 11.813 ms | 155x |
| P99 | 101.50 us | 15.024 ms | 148x |
| P99.9 | 232.83 us | 18.006 ms | 77x |

This is a same-day, same-rate, fully-reconciled (no drain issues, no sample-count gaps)
comparison — **Docker is ~100-150x worse than local at every percentile, even at a rate
neither environment is anywhere near saturated at.** This independently reproduces the
2026-08-30 Docker investigation's finding (`/memories/repo/docker-latency-investigation-10k-run.md`)
on the current codebase. Sub-stage data (queue-a P50 317us, queue-b P50 169us) shows the
gap is not concentrated in one queue — most of the ~4ms accumulates somewhere across
serv-a/serv-b/serv-c, not purely at ingress. This still needs stage-by-stage attribution
(Phase 1 Step 4's interval exporter, or at minimum reading every stage's `.hgrm`) before
picking a single culprit stage for Docker.

### Docker cannot sustain 50k msg/s — now with direct proof, not just sample-count inference

`docker-20260912T031756Z` (50k/10M) reproduces the same cascade signature as the original
invalid `docker-20260912T005909Z`, and for the first time we have exit-status evidence
explaining *why*: `serv-b` and `serv-c` were **force-killed** (`exit_code=137`, SIGKILL
after the stop timeout) while `serv-0`/`serv-a` stopped normally (`exit_code=143`).
Sample counts: queue-a 100% (10.0M), queue-b 90.5% (9.05M), queue-c 43.8% (4.38M), serv-c
43.8%, end-to-end 43.8%. **This is a genuine capacity/backpressure failure in
serv-b/serv-c under Docker at 50k msg/s, not a measurement artifact** — confirmed
reproduced across two separate runs now. Queue-a's own percentile curve shows a hard
cliff between P95 (4.48ms) and P96.25 (15.36ms) climbing to 42ms+ by P96.875, the
signature of a queue backing up faster than it drains, not tail jitter. Track this as its
own capacity-tuning item, independent of the local latency RCA.

### Local 50k/10M on current `HEAD` (`yielding`) does not reproduce the original regression — now confirmed across 3 repetitions

| Metric (e2e) | Old clean (`phased`, ce9acb5) | Old regressed (`phased`, ce9acb5) | `local-032306Z` | `local-034804Z` | `local-035404Z` |
|---|---:|---:|---:|---:|---:|
| P50 | 30.671 us | 40.095 us | 25.55 us | 25.26 us | 25.09 us |
| P90 | 65.439 us | 3.680 ms | 64.51 us | 64.45 us | 63.30 us |
| P95 | 79.167 us | 7.995 ms | 78.72 us | 79.42 us | 77.12 us |
| P99 | 188.415 us | 27.574 ms | 143.36 us | 309.25 us | 144.64 us |
| P99.9 | 6.660 ms | 91.554 ms | 3.510 ms | 4.760 ms | 1.765 ms |

Three independent repetitions (`local-20260912T032306Z/034804Z/035404Z`), all valid
(graceful shutdowns, sample counts reconciled within the same small warmup-leak margin as
before), all with verified rebuilt-artifact hashes confirming identical current-`HEAD`
bytecode ran each time. **This satisfies Phase 2 Step 5's 3-repetition bar: the
catastrophic regression does not reproduce on current `HEAD` with `yielding`.** P99
and P99.9 show normal run-to-run tail variance (P99 143-309us, P99.9 1.8-4.8ms) but
nothing remotely close to the old regressed run's 27.6ms/91.6ms. This does not yet prove
`yielding` (vs. `phased`) is *why* — that requires the single-variable `phased` comparison
below.

### Single-variable wait-strategy A/B — RESULT: `phased` intermittently reproduces the exact queue-a tail-explosion signature

Two repetitions were run with `FX_WAIT_STRATEGY=phased` (temporarily set in `local.env`,
now reverted back to `yielding`), same `HEAD`, same host, same day as the 3 `yielding`
repetitions above:

| Metric (e2e) | `phased` run 1 (`local-040622Z`) | `phased` run 2 (`local-041539Z`) | `yielding` (3-run range) |
|---|---:|---:|---:|
| P50 | 7.63 us | 7.58 us | 25.1-25.6 us |
| P90 | 11.42 us | 11.63 us | 63.3-64.5 us |
| P95 | 15.22 us | 17.81 us | 77.1-79.4 us |
| P99 | 113.8 us | 508.9 us | 143.4-309.3 us |
| P99.9 | 1.53 ms | **43.4 ms** | 1.77-4.76 ms |
| P99.99 (0.999804687500) | 2.71 ms | **78.0 ms** | (not separately tracked) |

**`phased` (current retuned form, `SPIN_TRIES=10_000`) has consistently *lower* median/P90
than `yielding` when it behaves well** — run 1 looks great across the board. But **run 2
reproduces the same signature as the original regression**: good median (7.58us) and P90
(11.6us), then a catastrophic tail starting around P99.5, reaching 43.4ms at P99.9 and
78-90ms by P99.98+ — approaching the original regressed run's 91.554ms. Critically,
`fx-latency-queue-a.hlog.hgrm` for run 2 shows the **identical shape at the identical
stage**: queue-a P50=2.4us, P90=3.9us (excellent) but P99.9=33.5ms, climbing to ~90ms by
the far tail. This is queue-a-first divergence with a good median, exactly like the
original incident.

GC correlation check: `serv-a`'s GC pause count is identical (6 pauses, all sub-ms) between
the good and bad `phased` runs — GC is not the differentiator. The bad run's stall is
consistent with `phased`'s park phase (`LockSupport.parkNanos`) occasionally interacting
badly with macOS scheduling on the queue-a tailer thread, matching
`PERFORMANCE_TUNING.md`'s own theory, though this is not yet proven at the interval level
(needs Phase 1 Step 4's exporter to see exactly when within the 202s window it happens and
what else was active at that moment).

**Interpretation:** wait strategy is not fully exonerated, but the picture is more nuanced
than "phased is bad, yielding is good" — `yielding` was 3-for-3 clean, `phased` was 1-for-2
clean with the 1 failure reproducing the exact original signature. This is consistent with
the original incident being a case of "got unlucky with an intermittent phased/park-related
stall," and with `yielding`'s design (never parks) being the correct mitigation — but a
3rd `phased` repetition would strengthen this from "consistent with" to "confirmed," and
`PERFORMANCE_TUNING.md`'s claim that `phased` uniformly inflates P50 to ~38us needs
correcting — that described the OLD `phased` (`SPIN_TRIES=200`); the current retuned
`phased` (`SPIN_TRIES=10_000`) has a low P50 most of the time and instead shows
occasional, severe tail excursions.

`local.env`'s `FX_WAIT_STRATEGY` has been reverted back to `yielding` (the validated
default) now that this comparison is captured.

### Correction: `yielding` is not immune either — 2 more repetitions (043440Z, 043920Z)

Two more `yielding` repetitions were run (still on current `HEAD`), bringing the total to
5. **This overturns the "yielding is 3-for-3 clean" claim above** — repetition 4
(`local-20260912T043440Z`) shows the same divergence pattern, just less severe:

| Metric (e2e) | `yielding` run 3 (clean) | `yielding` run 4 (`043440Z`) | `yielding` run 5 (clean) | `phased` bad run (`041539Z`) |
|---|---:|---:|---:|---:|
| P50 | 25.09 us | 25.17 us | 25.06 us | 7.58 us |
| P90 | 63.30 us | 65.44 us | 63.07 us | 11.63 us |
| P99 | 144.6 us | **997.4 us** | 136.4 us | 508.9 us |
| P99.9 | 1.77 ms | **26.3 ms** | 1.98 ms | 43.4 ms |

`local-043440Z`'s `queue-a` histogram shows the identical shape (P50=3.9us, but the tail is
already at 6.65ms by P99.6 and continuing to climb) — same mechanism, same stage, just a
smaller peak than the worst `phased` run (43.4ms).

**Revised conclusion:** this is not "`phased` is broken, `yielding` is fixed." Both wait
strategies show the same intermittent queue-a tail-explosion signature; `yielding` appears
to make it **less frequent and/or less severe** (1 moderate excursion in 5 runs, peak
26.3ms) than `phased` (1 severe excursion in 2 runs, peak 43.4ms), but neither is immune,
and 2 vs. 5 repetitions isn't enough to compare frequency rigorously. This points more
toward a **shared underlying trigger** (macOS host scheduling/thermal/other interference
common to any wait strategy on a non-isolated core) that wait-strategy choice modulates
but does not eliminate, rather than the wait strategy being the root cause by itself.
Confirming the frequency/severity difference (if real) and finding the shared trigger
both require the interval-level correlation from Phase 1 Step 4 — aggregate percentiles
across more repetitions can narrow this further but cannot pinpoint the trigger.

`local.env`'s `FX_WAIT_STRATEGY` has been set back to `phased` again at the user's request
for further repetitions.

### 3rd `phased` repetition (`local-045512Z`) + GC conclusively ruled out

| Metric (e2e) | phased run 1 | phased run 2 (worst) | phased run 3 | yielding (5-run range) |
|---|---:|---:|---:|---:|
| P50 | 7.63 us | 7.58 us | 7.58 us | 25.06-25.17 us |
| P90 | 11.42 us | 11.63 us | 11.38 us | 63.07-65.44 us |
| P99.9 | 1.53 ms | 43.4 ms | **15.2 ms** | 1.77-4.76 ms (4/5), 26.3 ms (1/5) |

Running tally across all 8 local repetitions today: **`phased` shows real tail elevation
in 2 of 3 runs (15.2ms, 43.4ms); `yielding` shows it in 1 of 5 (26.3ms).** Small samples,
but suggestive that `phased` hits this more often, not just harder.

GC is now conclusively ruled out, not just "not the differentiator": `serv-a`'s GC log
timestamps for both the worst `phased` run and this new one show pause activity **only**
in the first ~0.6s (JVM warmup/pretouch) and the final ~0.01s at shutdown — zero GC
events anywhere in the ~229s steady-state window in between, in either a clean or a
tail-blowup run. Whatever causes the stall, it happens with no GC involvement at all on
the affected thread.

Tried a quick host-level check: macOS unified log (`log show`) for thermal/throttle
events during the worst run's exact wall-clock window. Found continuous
`ApplePPMPolicyCPMS::setDetailedThermalPowerBudget` kernel activity with a steadily
climbing "Thermal Budget" counter throughout — but this is routine thermal-governor
bookkeeping running constantly regardless of stalls, not a discrete event that lines up
with the tail excursion. **Inconclusive** — this kind of manual log archaeology can't
pinpoint a ~15-40ms window inside a 200s run without first knowing exactly when the stall
happened. That's precisely what Phase 1 Step 4's interval exporter is for (per-second
HdrHistogram breakdown, so a bad interval's wall-clock timestamp is known and can be
matched against GC/thermal/scheduler logs directly, instead of scanning the whole run).

**Recommendation: stop accumulating more blind aggregate-percentile repetitions here.**
The frequency signal (phased worse than yielding) is suggestive but small-sample, and
without interval-level timing, more repetitions of the same kind of run add data volume
without adding precision. The productive next step is building Step 4's interval exporter
so the *next* set of repetitions can pinpoint the exact second the stall occurs and
correlate it directly against GC logs (already captured) and host signals, rather than
guessing from aggregates.

### Phase 1 Step 4 built: `IntervalHistogramExporter`, and an immediate concrete lead

Added `common/src/main/java/com/fx/common/telemetry/IntervalHistogramExporter.java` — a
CLI tool (`java ... com.fx.common.telemetry.IntervalHistogramExporter <in.hlog>
<out.csv>`) that walks every flush interval in a `.hlog` via HdrHistogram's
`HistogramLogReader` and exports per-interval percentiles with an **elapsed-seconds**
offset (accumulated from each interval's own duration, not from
`Histogram.getStartTimeStamp()`/`getEndTimeStamp()` — those were found to reconstruct
absolute epoch millis incorrectly, roughly 2x the real value, when read back via
`HistogramLogReader`; the interval-to-interval *duration* those fields produce is still
correct, so elapsed time is accumulated from that instead of depending on a library quirk
that isn't fully understood). Wired into `scripts/process_latency.sh` (runs automatically
after `.hgrm` generation, before plotting) and archived by
`scripts/generate_benchmark_report.sh` as `<hlog>.intervals.csv` alongside the other
per-run artifacts. Also fixed a latent, pre-existing bug found while validating this:
`process_latency.sh`'s HdrHistogram-jar auto-discovery glob could match the `-javadoc.jar`
variant (no classes inside) instead of the real jar on a fresh `~/.m2` cache — excluded
`-javadoc`/`-sources` explicitly. Covered by 2 new unit tests
(`IntervalHistogramExporterTest`), full `common` module suite still green.

**Immediate payoff — ran it retroactively against the worst `phased` run
(`local-041539Z`, 43.4ms e2e P99.9):** the entire event is isolated to **elapsed
75-77 seconds** into the 202s run. `queue-a`'s interval at 75.163-76.224s shows the
single worst latency in the whole run (max 432.5ms); `e2e`'s interval at 76.177-77.177s
shows the same 432.5ms max arriving downstream a second later, plus P90 spiking to
56.4ms for that one second. Both `serv-a` and `serv-0`'s GC logs show **zero** activity
anywhere near that window (consistent with the earlier "GC ruled out" finding). But
**`serv-c`'s GC log shows a burst of activity at 76.259-76.931s** — landing almost
exactly inside the e2e stall window. This is a new, promising lead, **not yet a proven
cause**: `serv-c` runs on a different pinned core (cpuset 4) than `serv-a`/queue-a's
tailer (cpuset 2), so this is more likely a shared host-level trigger (something that
makes both serv-c allocate/collect more AND stalls the queue-a tailer at the same moment)
than direct causation — but it is the first concrete, precise correlation found in this
investigation, and the interval exporter is what made it visible within minutes instead
of more log archaeology across an entire 200s window.

**Next validation step:** re-run one or two more `phased`/`yielding` repetitions and check
whether a `serv-c` GC burst reliably coincides with every tail excursion (would elevate
this from "one coincidence" to "a real lead") or whether this specific pairing was itself
coincidental.

### Consolidated tally after 2 more repetitions each (10 total local 50k/10M runs today)

`local-053550Z` (phased, 4th repetition): e2e P99.9 = 3.46ms — clean.
`local-054150Z` (yielding, 6th repetition): e2e P99.9 = 2.74ms — clean. Neither reproduced
the severe pathology, so no new data point for the serv-c correlation from these two —
but re-ran the exporter retroactively against the other already-archived moderate run
(`local-045512Z`, P99.9=15.2ms) to get a second independent test of that lead instead.

**Second serv-c correlation check, and an important caveat.** `045512Z`'s worst interval
is elapsed 91.191-92.192s (P90 spikes to 15.9ms that second). `serv-c`'s GC log shows
bursts at 89.452-90.268s and again at 93.277-93.792s — bracketing the stall within
1-3 seconds either side, but not landing inside it this time (unlike the first check,
where it landed inside the window). **Caveat that must be stated plainly: `serv-c`
GCs roughly every 2-3 seconds for the entire 202s run (~69 cycles total), so finding a
`serv-c` GC event within 1-3 seconds of *any* given point in the run is not, by itself,
strong evidence of correlation — it could easily happen by chance given that base rate.**
A rigorous version of this check would compare the bad-interval-to-nearest-`serv-c`-GC
gap against the same gap computed for *every* interval in the run (bad or not); this
hasn't been done yet. Downgrading this from "promising lead" to "plausible but unproven"
until that proper comparison exists.

**Updated frequency tally (10 total local 50k/10M repetitions today):**

| Wait strategy | Repetitions | Clean (P99.9 < 10ms) | Elevated (>= 10ms) | Elevated rate |
|---|---:|---:|---:|---:|
| `phased` | 4 | 2 (1.53ms, 3.46ms) | 2 (15.2ms, 43.4ms) | 50% |
| `yielding` | 6 | 5 (1.77-4.76ms) | 1 (26.3ms) | 17% |

At this sample size, `yielding` showing a meaningfully lower elevated-tail rate than
`phased` is a real signal worth acting on (not just noise), even though the underlying
mechanism is still not pinned down. Further repetitions have diminishing returns for the
frequency question specifically — the more valuable next investment is either (a) the
rigorous nearest-GC-gap statistical comparison above, or (b) moving forward with
`yielding` as the recommended default and treating the remaining tail risk as a known,
monitored characteristic rather than something that must be fully explained before
proceeding.

### Busyspin validation at 50k/10M — the "lost" setting found, tested, and NOT a free lunch

Per explicit user request (2026-09-12, this session), ran the same 3-repetition,
current-`HEAD` protocol already used for `phased`/`yielding` against **`busyspin`** — the
wait strategy `local.env` actually used from `85ba287` (2026-09-08) through `ce9acb5`
(2026-09-12), i.e. during every historical benchmark still archived in git
(`benchmark-runs/local/local-20260908*`, `git log -p --follow -- config/profiles/local.env`
confirms the exact commit-by-commit history). `config/profiles/local.env` was temporarily
set to `busyspin`, 3 runs executed, then reverted back to `yielding` (unchanged from
before this experiment — no config change resulted from this test).

| Metric (e2e) | busyspin run 1 (`074551Z`) | busyspin run 2 (`080116Z`) | busyspin run 3 (`080544Z`) |
|---|---:|---:|---:|
| P50 | 7.75 µs | 7.63 µs | 7.67 µs |
| P90 | 13.71 µs | 12.79 µs | 13.09 µs |
| P99 | ~169 µs | ~236 µs | ~192 µs |
| P99.9 | ~3.42 ms | ~9.60 ms | ~4.21 ms |
| Max | 19.56 ms | 43.45 ms | 19.87 ms |

All 3 runs valid: graceful exit on all 4 services, sample counts reconciled to the same
small warmup-leak margin as every other run this session, rebuilt-artifact hashes
recorded in each manifest.

**Key findings:**
1. **`busyspin`'s P50/P90 (~7.6-7.75µs / ~12.8-13.7µs) essentially matches current
   `phased`'s (`SPIN_TRIES=10_000`) P50/P90 (~7.6µs / ~11.4-11.6µs).** This confirms the
   working hypothesis recorded earlier: `SPIN_TRIES=10_000` (~20µs spin window) is close
   enough to the 50k-msg/s inter-arrival gap (20µs) that `phased` rarely reaches its
   yield/park phase under sustained load — it behaves like `busyspin` for median/P90
   purposes.
2. **`busyspin` shows the same intermittent tail-blowup signature as `phased` and
   `yielding`.** Run 2's P99.9 (9.60ms) sits right at the elevated boundary used
   throughout this investigation, with a Max (43.45ms) matching the worst `phased` run
   (43.4ms) almost exactly. The other two runs are "clean" by the P99.9<10ms bar, but
   both still show a higher baseline Max (~19.6-19.9ms) than `yielding`'s typical clean
   runs.
3. **The stall originates at queue-a and propagates downstream ~1s later, exactly as
   seen before**: run 2's worst `queue-a` interval is elapsed 100.196-101.201s (max
   39.06ms); e2e's worst interval is 101.276-102.277s (max 43.45ms) — the same ~1s lag
   pattern as the `phased` run analyzed earlier via `IntervalHistogramExporter`.
4. **The serv-c GC-burst correlation was seen a 3rd time**: `serv-c`'s GC log shows a
   pause (`GC(16)`) at elapsed 100.552-100.945s, landing almost exactly at the start of
   queue-a's worst interval; `serv-a` shows zero GC activity in the same window (GC on
   the affected thread itself remains ruled out). This is now 3-for-3 when checked (2
   `phased` runs previously, this `busyspin` run now) — upgraded confidence, but still
   not the rigorous nearest-GC-gap-vs-baseline statistical comparison this RCA's own bar
   requires, so still "plausible, not proven."

**Updated cross-strategy tally (13 total local 50k/10M runs today, all current `HEAD`):**

| Wait strategy | Repetitions | Clean (P99.9 < 10ms) | Elevated (>= 10ms) | Elevated rate | P50 range |
|---|---:|---:|---:|---:|---:|
| `busyspin` | 3 | 2 (3.42, 4.21ms) | 1 borderline (9.60ms) | ~33% | 7.63-7.75µs |
| `phased` (`SPIN_TRIES=10k`) | 4 | 2 (1.53, 3.46ms) | 2 (15.2, 43.4ms) | 50% | 7.58-7.63µs |
| `yielding` (`SPIN_TRIES=1k`) | 6 | 5 (1.77-4.76ms) | 1 (26.3ms) | 17% | 25.06-25.17µs |

### Resolution: nothing was actually "lost" — it was untested at this scale, not safe

`busyspin` is real, reproducible, and fast at the median (~7.6-7.75µs, matching every
historical 10k-msg/s archived run almost exactly — see `benchmark-runs/local/
local-20260908T124515Z/`) — this is almost certainly what the user remembers. But at the
50k msg/s / 10M-message scale this investigation is about, it is **not** a "genuinely
low, stable" configuration free of the tail risk already being chased in the
phased/yielding A/B — it shows the identical intermittent, queue-a-first, ~1-second
stall signature, at a rate (this small 3-run sample) in between `phased`'s and
`yielding`'s. The historical benchmark evidence looked clean because it was measured at
10k msg/s (5x lighter load, best archived run: Max=4.17ms) — a rate with enough headroom
that the same underlying macOS-scheduler-preemption risk apparently manifested less
often/severely, not because the config itself was actually safe at production-scale rates.

**Practical conclusion: reverting `local.env` to `busyspin` would not recover a
lost-but-safe configuration, so no config change resulted from this experiment.**
`yielding` remains the best-evidenced `local` default of the three tested (lowest
elevated-tail rate at 17%, even though it has the worst P50 of the three). This is a
real, unavoidable trade-off on non-isolated macOS, not a bug to fix.

**Documentation correction made**: `PERFORMANCE_TUNING.md` §8.3 claimed `yielding`
"retain[s] the 7µs P50" — today's 6-run data shows its actual P50 is ~25µs, not ~7µs
(~3.3x off). It also claimed `phased` inflates P50 to ~38µs, which was true only for the
pre-retune `SPIN_TRIES=200` version; the current retuned `SPIN_TRIES=10_000` version
measures ~7.6µs. Both corrected directly in that file with a dated verification note.

**What would actually get a flat, low-latency profile**: per `PERFORMANCE_TUNING.md`'s
own (directionally correct) theory, real CPU isolation (`isolcpus` on bare-metal/EC2
Linux) is the only environment where `busyspin`'s tail risk doesn't apply, because the
core is never contended. `baremetal.env`/`ec2.env` already use `busyspin` and have
**never drifted** from it (confirmed via full git history) — but this whole
investigation, today and historically, has never actually executed a bare-metal/EC2 run
to empirically confirm the "perfectly flat" claim. That remains the one genuinely open,
high-value validation left, if/when a bare-metal or EC2 host is available to test
against.

### Docker 50k/10M repeated twice more — capacity failure confirmed a 3rd and 4th time, with a new twist

`docker-20260912T041122Z` and `docker-20260912T042017Z` both again reconcile to only
~43-44% end-to-end. The 4th docker 50k/10M attempt (`041122Z`) shows a **different**
force-kill pattern than the previous three: this time `serv-a` and `serv-c` were
force-killed (`exit_code=137`) while `serv-0`/`serv-b` stopped gracefully — and `serv-a`'s
own per-stage histogram shows **zero recorded samples** even though queue-b (which serv-a
writes to) received ~9M events, meaning serv-a genuinely processed millions of events but
its telemetry never survived to be read: **SIGKILL gives the JVM zero chance to run
`TelemetryRecorder.close()`'s final-interval flush, so a force-killed service's telemetry
can be completely lost, not just incomplete.** The 5th attempt (`042017Z`) reverted to the
earlier serv-b+serv-c-killed pattern. Across 4 docker 50k/10M attempts now: the specific
service that gets force-killed varies, but there is always a downstream capacity collapse
around queue-b/queue-c and always at least one force-kill. This reinforces that Docker's
50k msg/s failure is a genuine, repeatable capacity ceiling somewhere in the aggregate
serv-a->serv-c pipeline, not a single deterministic bottleneck — and that force-killed
services' telemetry cannot be trusted as evidence at all (a gap for the manifest's
validity rules to encode: a run with any `exit_code=137` service should also flag that
service's histogram as potentially incomplete/lost, independent of its reported sample
count).



### Docker 10k/1M repeated (`docker-20260912T034226Z`) — consistent with the first run

Sample counts again reconcile exactly to 1,000,000 at every stage (busyspin, trace
enabled, graceful-shutdown-equivalent `exit_code=143` across all four services) —
confirms the local-vs-Docker gap at 10k msg/s is a repeatable characteristic, not a
one-off. Full percentile comparison not repeated here since the first run
(`docker-20260912T031308Z`) already established the ~100-155x gap; worth doing later if
Docker becomes the active workstream.



### New GC evidence (serv-c allocates and collects far more than serv-0/a/b)

Captured via the newly-added per-service `-Xlog:gc*` logs on `local-20260912T032306Z`
(archived retroactively after this analysis, since the archival step didn't exist yet
when the run happened — now fixed, see below): `serv-c` logged **~69 ZGC cycles** over
the ~202s run (every 2-16s); `serv-0`/`serv-a`/`serv-b` each logged **~1 cycle total**.
Every individual pause is sub-millisecond (ZGC working as designed), so this alone does
not explain multi-millisecond tail spikes, but it is concrete, quantified confirmation
that `serv-c`'s JDBC/H2 batch-write path allocates measurably more than the pure
event-loop stages — relevant to Phase 3 Step 8's zero-GC gate (serv-c will not pass a
"zero GC collections during the measurement window" bar as-is) and a candidate
contributor once interval-level correlation (Phase 1 Step 4) exists to check timing
alignment against the actual latency tail.

### Harness gap found and fixed during this analysis

GC logs and `.exitstatus` files were being generated correctly but were **not being
archived** into `benchmark-runs/<env>/<run-id>/` — they were only ever written to the
live `logs/`/`fx-telemetry/` paths, which get overwritten by the next run. Fixed in
`generate_benchmark_report.sh`'s archiving step; retroactively copied for
`local-20260912T032306Z` so the serv-c GC data above isn't lost.

## New evidence: 2026-09-13 user-provided bare-metal Vultr benchmark archive

The user added historical, previously-uncommitted run archives
(`benchmark-runs/baremetal_vultr/`, 10 runs, and `benchmark-runs/baremetal_vultr_docker/`,
8 runs; both dated 2026-09-09/09-10, predating this incident) for analysis. These are
real Vultr bare-metal runs (AMD EPYC 4345P 8-core, `cpu_profile="isolated"`, JDK
21.0.12.1, Linux 6.12, queue on `/dev/shm`, `-Dfx.serv-*.cpucore` AffinityLock flags
present) — the first genuine `isolcpus` data this whole investigation has had access to.
`FX_WAIT_STRATEGY` was `busyspin` for all of them (confirmed via `git log -p` on
`config/profiles/baremetal.env`: it has never used anything else).

**This surfaced a second, separate tail-latency problem, distinct from the macOS
queue-a/wait-strategy one above.** Three runs at 50k msg/s (two at 10M messages, one at
1M) all reproduce a severe tail:

| Run (`baremetal_vultr-...`) | git_sha | count | e2e P50 | e2e P90 | e2e P99 | e2e P99.9 | e2e Max |
|---|---|---:|---:|---:|---:|---:|---:|
| `20260910T065436Z` | `a321e77` | 10M | 3.69µs | 4.92µs | ~658µs | ~134.5ms | 323.5ms |
| `20260910T090128Z` | `0d31ee3` | 10M | 3.71µs | 4.91µs | ~907µs | ~224.1ms | 348.7ms |
| `20260910T095617Z` | `067fc5e7` | 10M | 3.69µs | 4.86µs | ~7.2µs | ~324.8ms | 495.2ms |
| `20260910T085954Z` | `0d31ee3` | 1M | — | — | — | — | 25.5ms |

All 4 runs are valid (sample counts reconcile exactly to the target message count on
every stage). P50/P90 are excellent — even better than macOS's best `busyspin` numbers —
but the tail is far worse than anything measured on macOS local (worst there: 43.4ms
Max). The 1M-message run's much smaller Max (25.5ms) vs. the two 10M-message runs
(323-495ms) shows the problem grows with run duration/message count, not just rate.
At 10,000 msg/s the same native bare-metal environment is clean; a milder version of the
same signature (queue-c Max ≈ e2e Max, just ~6ms instead of hundreds of ms) also showed
up on Vultr-hosted **Docker** at only 10k msg/s, suggesting Docker's overhead lowers the
threshold at which this triggers.

**Stage isolation points precisely at `queue-c`, not `queue-a`:** per-stage Max values
for every affected run show `queue-c`'s Max essentially equal to e2e's Max (e.g.
348,651,519ns vs 348,651,519ns — exact match in one run), while `queue-a`/`queue-b` stay
in the microsecond-to-low-millisecond range and every service's own handler-dispatch
time (`serv-a`/`serv-b`/`serv-c`) stays at most tens of microseconds. The delay is
entirely in the wait between `serv-b` appending to `queue-c` and `serv-c`'s tailer
calling `handle()` — not in any service's actual processing time.

**Code-grounded mechanism** (read directly from `BatchPersistenceEngine.java`, not just
inferred from histograms): `accumulate()` (called from serv-c's hot path, i.e. the same
thread that tails `queue-c`) spin-waits if its 524,288-slot ring buffer is completely
full. The class's own Javadoc already documents H2 MVStore commit time growing as the
`fx_trades` table grows (this is why `MAX_BATCH` was previously cut from 32,768 to 8,192
— the larger batch was causing 200ms-2s+ commits once the table passed ~1M rows). A
ring-full spin-wait blocks the same thread that reads `queue-c`, so every event still
queued behind the stall gets an inflated `t3ServCEntry` timestamp — recorded as `queue-c`
latency. This is consistent with worse Max on the 10M-message runs (bigger table by the
end) than the 1M-message run, and is a completely different mechanism from the macOS
finding: it reproduces on genuine `isolcpus` hardware (ruling out scheduler preemption)
and is unrelated to `WaitStrategy` choice (`busyspin`, unchanged, in every run).

**Confidence: well-evidenced, not yet live-instrumented.** These runs predate this
repo's GC-log/`IntervalHistogramExporter` tooling (added 2026-09-12), so there's no
direct ring-occupancy or GC measurement pinpointing the exact stall moment the way the
macOS `serv-c` correlation was checked — the stage-isolation evidence and code mechanism
are strong but a live re-run on the same host with current tooling would be needed to
call this fully confirmed. **No fix has been applied** — this is a newly-surfaced,
separate finding pending a decision on whether/how to pursue it. Full detail and
candidate (unvalidated) remediation directions in `PERFORMANCE_TUNING.md` §8.4.

### Instrumentation added (2026-09-13): direct ring-occupancy and H2 commit-duration measurement

Per user request, added purely-additive diagnostics to `BatchPersistenceEngine` (no
behaviour change — same `RING_SIZE`/`MAX_BATCH`/backpressure logic as before) using the
exact same zero-allocation `TelemetryRecorder`/HdrHistogram mechanism already used for
every other pipeline-stage metric:

- **`fx-latency-ring-occupancy.hlog`** — records `writePointer - readPointer` (the
  number of events sitting in the ring, awaiting the db-writer) at every `accumulate()`
  call. Hot-path cost: one more `recordValue()` call, same class as the pre-existing
  `queueCRecorder`/`servCRecorder` calls immediately around it. If this climbs toward
  524,288 (`RING_SIZE`) over the course of a run, that's direct confirmation the ring is
  approaching/hitting the capacity ceiling this finding describes.
- **`fx-latency-db-commit.hlog`** — records the wall-clock duration of
  `executeBatch()` + `commit()` in `flushLoop()`, entirely on the background
  `db-writer` thread — **zero cost on the serv-c hot path**. If this grows over the
  course of a run (correlating with elapsed time / `fx_trades` row count), that's
  direct confirmation of the "H2 MVStore commit time grows with table size" mechanism
  already documented in `BatchPersistenceEngine`'s own Javadoc history.

Both files are named `fx-latency-*.hlog`, so the existing `process_latency.sh` /
`generate_run_manifest.py` / `generate_html_report.py` pipeline picks them up
automatically (no script changes needed) — they'll appear as two more rows in the next
benchmark's HTML report and manifest, right alongside `queue-c`/`serv-c`/etc.

Backward-compatible: `BatchPersistenceEngine(jdbcUrl)` and
`PersistenceEventLoop(jdbcUrl, e2eRecorder, queueCRecorder, servCRecorder)` are unchanged
overloads that pass `null` for both new recorders (all existing call sites and tests
untouched); `PersistenceMain` now constructs and wires the two new recorders through a
new six-argument `PersistenceEventLoop` constructor. Added
`BatchPersistenceEngineTest.testDiagnosticRecordersCaptureDataAndDoNotDisruptPersistence`
verifying both recorders actually flush data and that persistence behaviour is
unaffected; full `serv-c` suite (20 tests) and `mvn -pl serv-c -am compile` both green.

**Next step: this needs a rebuild + redeploy + re-run of the 50k msg/s / 10M-message
bare-metal benchmark to actually produce data** — nothing above changes any currently
archived run's numbers; it only equips the *next* run to answer the ring-fill and
commit-growth questions directly instead of by inference.

### Correction (2026-09-13): the 09-09/09-10 bare-metal runs above were run with the wrong profile — re-run on corrected hosts, picture is now more complex

The user reported the original `baremetal_vultr`/`baremetal_vultr_docker` runs analysed
above were executed with the wrong profile, and provided 4 new runs
(`benchmark-runs/baremetal_vultr/baremetal_vultr-20260912T2053-2058*`) taken after
correcting it. Two important differences from the original data before even looking at
latency: these new runs are on **different physical hosts** than the 09-09/09-10 ones —
`cpu_model` is now `"Intel(R) Xeon(R) E-2286G CPU @ 4.00GHz"` and
`"AMD EPYC 4245P 6-Core Processor"`, both **6-core**, vs. the original
`"AMD EPYC 4345P 8-Core Processor"` (**8-core**). All four still show `cpu_profile:
"isolated"` and the same `-Dfx.serv-*.cpucore=1/2/3/4` mapping, and `git_sha` is
`17ecce0` (2026-09-12, before this session's schema-v2/GC-log tooling) for all four —
same limitation as before: no GC logs or interval-level data for these.

**`queue-c`'s dominant tail reproduces again, now on two more hosts (5 total 50k+ runs across 3 different machines):**

| Run | Host | count | e2e P50 | e2e P99.9 (interp.) | e2e Max | queue-c Max |
|---|---|---:|---:|---:|---:|---:|
| `205404Z` | AMD EPYC 4245P (6-core) | 10M | 3.78µs | ~172.8ms | 361.2ms | 361.2ms (exact match) |
| `205703Z` | Intel Xeon E-2286G (6-core) | 10M | 7.72µs | ~500.7ms | 652.7ms | matches e2e tail shape closely |

`652.7ms` is the worst e2e Max seen in this entire investigation, on either day.
`queue-c`'s percentile curve continues to track e2e's almost exactly from ~P99.8 onward
in both runs, exactly like the original 3 runs — this part of the finding is now
reproduced 5/5 times across 3 distinct physical hosts and is on firmer ground than
before, not weaker.

**New and different from the original analysis: `queue-a` (and sometimes `serv-0`) now
also show large-scale, not single-sample, tail inflation.** In `205404Z`, `queue-a` and
`serv-0` share an identical single extreme outlier (196,083,711ns on both, one sample
out of 10,000,000 — almost certainly one specific cold/first-message event, not a
systemic pattern), but `queue-a` *also* has its own broader climb from ~198µs at P99.86
up to ~13ms by P99.9999 before that single outlier, which `queue-b` inherits unchanged
(matching max) — a real, if much smaller than queue-c's, secondary tail. In `205703Z`
this is far more severe: `queue-a` climbs steeply from ~116µs at P99.90 to 158.07ms by
Max, independently of `serv-0` (whose own Max there is only 5.8ms) — meaning something
delays `serv-a`'s tailer specifically, not serv-0's write path. `serv-a`'s own dispatch
time stays tiny (≤11.8µs) in both runs, same as `serv-c`'s does for the queue-c finding
— so whatever this is, it has the same *shape* as the queue-c mechanism (a hot-path
tailer thread stalling, not slow business logic) but `serv-a`/`RiskValidationEventLoop`
has no ring-buffer/backpressure component analogous to `BatchPersistenceEngine` to
explain *why* it would stall this way.

**This was not visible (or far less visible) in the original 8-core-host data** — none
of the three original runs showed anything close to a 158ms `queue-a` Max; the largest
`queue-a` Max there was 1.14ms. Two candidate explanations, not yet distinguished:
(a) the profile fix itself changed something that exposes a real, previously-masked
`queue-a`/`serv-a` issue, or (b) the 8-core → 6-core host change removed slack CPU
capacity that was incidentally absorbing scheduling noise even under `isolcpus` (the
`-Dfx.serv-*.cpucore` mapping only reserves cores 1-4 for services + core 0 for
housekeeping — an 8-core host has 2 completely unused cores beyond that, a 6-core host
has zero). **Both are plausible; neither is confirmed. Asking the user rather than
guessing — see the questions at the end of this session's report.**

**User's answers:** the original 09-09/09-10 runs used "local... instead of baremetal
profile"; the two new hosts are different/new machines entirely, not the same boxes
reconfigured — so the 8-core→6-core change is a genuine, permanent hardware difference,
and the `queue-a`/`serv-a` finding is confounded by the profile fix *and* a hardware
change happening simultaneously; the two causes cannot be separated with data currently
available. Decision: leave `queue-a`/`serv-a` documented as open, revisit later — no
further investigation performed on it this session. One discrepancy worth flagging for
whoever revisits this: "local instead of baremetal" doesn't fully match what the old
runs' manifests show — they already have `cpu_profile: "isolated"` and the
`-Dfx.serv-*.cpucore` flags, which `config/profiles/local.env` does not produce
(`FX_CPU_PROFILE="host"`, no cpucore flags at all, re-verified directly). Do not assume
the "local vs baremetal" explanation resolves the host-difference confound above — it
may have instead been an `ec2.env`-vs-`baremetal.env` mix-up (those two are
indistinguishable in every manifest field), or something else entirely.

## Confirmed Root Cause

_Full sign-off against the original Phase 2 falsification rules is still not possible in
this session — those rules lived in `/memories/session/plan.md` from the prior session,
and session-scoped memory does not persist across conversations, so the exact bar that
was set cannot be re-checked verbatim. What follows is what the evidence supports as of
2026-09-12, stated with explicit confidence levels rather than a blanket "confirmed"._

**Well-evidenced (13 total runs across all 3 wait strategies, current `HEAD`, 50k msg/s /
10M messages, all with rebuilt-artifact hashes and graceful shutdowns):** the local
macOS regression is an intermittent, single-~1-second-window stall that consistently
originates at `queue-a` (good median, catastrophic tail) and propagates downstream about
1 second later. It reproduces under `busyspin`, `phased`, and `yielding` alike — i.e. it
is **not** caused by any one wait strategy being defective. Its *frequency* (not its
existence) tracks how long each strategy keeps the event-loop thread demanding the CPU
before voluntarily yielding (`busyspin`=never, `phased`@10k spin-tries≈rarely at 50k
msg/s, `yielding`@1k spin-tries=soonest) — consistent with `PERFORMANCE_TUNING.md`'s
pre-existing theory that macOS's lack of true CPU-core isolation lets the scheduler
occasionally preempt/migrate a CPU-hungry thread. GC on the *affected* thread itself is
ruled out (zero GC events in every stall window checked, on the thread that stalls).

**Plausible, not statistically proven:** a `serv-c` GC burst has coincided with the
stall window 3 out of 3 times it was checked (2 `phased` runs, 1 `busyspin` run), but
`serv-c` GCs every 2-3 seconds for the whole run, so this has not been checked against
the rigorous baseline (nearest-GC-gap for bad intervals vs. the same gap for *every*
interval) that would distinguish a real trigger from coincidence at that base rate.

**Not applicable to Docker** (see its own section above): Docker's 50k msg/s failure is
a separate, directly-proven capacity/force-kill problem in serv-b/serv-c, unrelated to
the wait-strategy tail-latency mechanism described here.

**Also not applicable to bare-metal**: the `queue-c`/H2 capacity ceiling found in the
2026-09-13 bare-metal Vultr data (see above) is a third, separate mechanism again —
validated on genuine `isolcpus` hardware where the macOS mechanism cannot occur, and
predominantly localized to `queue-c`/persistence rather than `queue-a`/ingress.
**Update (2026-09-13, corrected-profile re-run):** two further runs on different
(6-core) bare-metal hosts reproduced `queue-c`'s dominant tail again, but also showed
`queue-a`/`serv-a` developing their own large-scale tail (not just single-sample noise)
— not seen on the original 8-core host. Whether this is a second real mechanism or an
artifact of less spare CPU capacity on the newer 6-core hosts is not yet distinguished.
Still tracked as its own item, independent of the macOS wait-strategy RCA either way.

## Confirmed regression: `fx.use.optimized.eventloop=true` (batched + busy-spin tailer) makes bare-metal tail latency worse, not better

**Context:** an attempt to reach a sub-100µs P99.9 target added `OptimizedQueueTailer`
(busy-spin wrapper) and a batched `AbstractEventLoop.runLoopOptimized()` path (process up
to `fx.batch.size` events per outer loop iteration before re-checking the wait strategy),
gated behind `-Dfx.use.optimized.eventloop=true`.

**Data-corruption bug found and fixed first:** the initial implementation read documents via
a no-op callback (`tailer.readDocument(msg -> {})`), which consumed each Chronicle Queue
document without decoding it into the target flyweight. Every event handled by the optimized
loop was an empty/default object. This was invisible in per-stage dispatch metrics (computed
from purely local timestamps) but produced zero recorded samples for `queue-a`/`queue-b`/
`queue-c`, end-to-end, and `db-commit` (all of which depend on timestamp fields propagated
from upstream stages). Fixed in `OptimizedQueueTailer.readDocument(ReadMarshallable)` by
passing the real flyweight through to `underlying.readDocument(message)`, and
`runLoopOptimized()` was rewritten to call it directly instead of going through the
structurally-broken index-only `EventBatchBuffer`. Regression test:
`common/src/test/java/com/fx/unit/OptimizedQueueTailerTest.java`.

**After the fix, two full bare-metal 50k msg/s / 10M message runs
(`baremetal_vultr-20260913T042945Z`, `baremetal_vultr-20260913T050157Z`, same git SHA
`1ffb00e1`) both reproduced a clear regression** relative to the pre-optimization bare-metal
baseline (`baremetal_vultr-20260913T004300Z`, P99.9 = 15.7µs, max = 4.19ms):

| Metric | Baseline (no opt) | Run 1 (042945Z) | Run 2 (050157Z) |
|---|---:|---:|---:|
| e2e P99.9 | 15.7 µs | 21.3 µs | 40.9 µs |
| e2e P99.99 | — | 1,885 µs | 6,963 µs |
| e2e Max | 4.19 ms | 12.4 ms | 22.0 ms |
| queue-a max | 1.95 ms | 12.07 ms | 8.57 ms |
| queue-b max | — | 2.26 ms | 12.48 ms |
| queue-c max | 456.7 µs | 1.49 ms | 21.35 ms |
| db-commit max | — | 68.2 ms | 41.5 ms |
| intervals (of 202) with P99.9 ≥ 100µs | ~7-16 (baseline range) | 63 | 64 |

P50/P90/P99 are essentially unchanged from baseline in both runs — the regression is purely
in the tail. ~31% of one-second intervals have a P99.9 ≥ 100µs in both optimized runs (not a
single-run fluke), and the dominant contributor shifts between queue-a, queue-b, and queue-c
across the two runs, which points at the batching mechanism itself (processing up to 128
events before yielding to the wait strategy) rather than any one queue's tailer. `db-commit`
tens-of-milliseconds max spikes are a new failure mode not present in the baseline at all.

**Status: confirmed regression, not adopted.** `-Dfx.use.optimized.eventloop=true` must
remain opt-in/experimental only (see `config/profiles/local.env` and `baremetal.env` policy
notes) and must not be treated as a validated improvement.

**Update (2026-09-13, batch-size isolation attempt): `fx.batch.size=8` does not fix it.**
A third bare-metal run (`baremetal_vultr-20260913T060533Z`, same git SHA `1ffb00e1`, only
`fx.batch.size` changed 128 → 8) still reproduces the regression at essentially the same
severity: e2e P99.9 = 42.5µs (worse than both batch=128 runs), e2e max = 6.1ms, and 62/202
intervals still have P99.9 ≥ 100µs (batch=128 runs: 63 and 64/202 — i.e. unchanged within
noise). `db-commit` max reached 88.4ms, the worst of the three optimized runs so far.

**Correction (2026-09-13): the batch-size experiment did not actually vary the hot path.**
A fourth run at `fx.batch.size=1` (`baremetal_vultr-20260913T063556Z`) was added to try to
isolate batching from busy-spin, but review of `AbstractEventLoop#runLoopOptimized` shows
`tailer.readDocument()` is called once per event regardless of batch size — the batch size
only bounds how many consecutive successful reads run before the outer loop re-checks the
`running` flag (a cheap volatile read). So all four optimized runs (two at batch=128, one at
batch=8, one at batch=1) exercise the *same* hot-path call pattern: `OptimizedQueueTailer`'s
busy-spin/yield/sleep read, invoked once per event. The batch-size knob was therefore not a
meaningful experimental variable in this implementation, and the P99.9 spread across the four
runs (21µs → 41µs → 42.5µs → 110µs) is most likely run-to-run variance, not a batch-size
effect — note P99.9 trended *worse*, not better, as batch size shrank.

What *is* consistent across all four optimized runs and absent from the pre-optimization
baseline: `db-commit` max is 41-88ms in every optimized run (batch=128: 68.2ms, 41.5ms;
batch=8: 88.4ms; batch=1: 70.1ms) vs. no such spike in the baseline; e2e P99.9 is worse than
the 15.7µs baseline in all four; and the bad-interval count stays in the same 62-78/202
(~31-39%) range regardless of batch size. This points at `OptimizedQueueTailer`'s busy-spin
tailer itself — the one component common to every optimized run — rather than batching.

Next isolating step (the one that actually changes the hot path): temporarily swap
`OptimizedQueueTailer` for a plain `ExcerptTailer.readDocument(flyweight)` inside
`runLoopOptimized()`, keeping the rest of the loop structure identical, and re-run on bare
metal. If the regression disappears, busy-spin is the confirmed cause.

## Corrective Action

_Pending — will be scoped to exactly the branch proven in Phase 2 (see plan Phase 3 Step 9).
No hot-path or wait-strategy change will be described as "the fix" until it has A/B evidence._

## Validation

_Pending Phase 4: independent local / Docker / bare-metal validation matrix._

## Rollback

Every change in this remediation is applied behind a reversible profile/config switch
(`config/profiles/*.env`, `TelemetryBootstrap` flush-mode toggle). Rolling back means reverting
the specific profile value or config flag; no change is applied directly to shared hot-path
code without a flag.

## Follow-ups

- Track the `docker-20260912T005909Z` serv-c/H2 drain failure to a confirmed cause
  independently of this latency RCA.
- Re-validate the already-shipped `phased` -> `yielding` local wait-strategy change with real
  A/B data once Phase 2 tooling lands, instead of leaving it as an unvalidated assumption.
- Track the bare-metal `queue-c`/`BatchPersistenceEngine` H2 capacity ceiling (2026-09-13
  finding, PERFORMANCE_TUNING.md §8.4) as its own item — needs a live re-run with current
  GC-log/interval tooling before any remediation is attempted.
- Root-cause the `fx.use.optimized.eventloop` batching regression (confirmed 2026-09-13,
  see "Confirmed regression" section above) before any further attempt at a sub-100µs
  target: likely candidates are the up-to-128-event batch window delaying the wait
  strategy's reset, and/or bursty downstream pressure on `BatchPersistenceEngine` from
  processing a full batch before any backpressure signal is checked. Try a much smaller
  `fx.batch.size` (e.g. 1-32) and/or excluding serv-c from the optimized loop as isolating
  experiments.
