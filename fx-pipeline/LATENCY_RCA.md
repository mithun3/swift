# Latency Regression RCA

**Status:** IN PROGRESS — evidence hardening underway (Phase 1). No root cause is confirmed
yet. This document is updated as each phase below completes; do not treat any hypothesis in
this file as the confirmed cause until it appears under "Confirmed Root Cause" with supporting
evidence linked.

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

## Confirmed Root Cause

_Pending Phase 2 causal experiments. Do not fill in until falsification rules in the
implementation plan (`/memories/session/plan.md`, Phase 2 Step 7) are satisfied. The
2026-09-12 evidence above narrows things (Docker capacity failure is now proven; local
regression doesn't trivially reproduce on current HEAD) but does not, by itself, satisfy
those rules yet — need 3+ repetitions and the one-variable-at-a-time matrix._

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
