#!/usr/bin/env python3
"""
generate_html_report.py

Generates a comprehensive, human-readable HTML report from HdrHistogram data.
Extracts percentiles from .hgrm files and embeds .png charts via Base64.

Unit display: all latency values are shown with adaptive units so that
sub-microsecond precision is never lost in the display layer:
  < 1,000 µs  → microseconds (µs)
  < 1,000 ms  → milliseconds (ms)
  >= 1,000 ms → seconds (s)
"""

import sys
import os
import base64

# ── Nanosecond boundary constants for adaptive unit selection ─────────────────
_NS_PER_US       = 1_000          # 1 µs  = 1,000 ns
_NS_PER_MS       = 1_000_000      # 1 ms  = 1,000,000 ns
_NS_PER_S        = 1_000_000_000  # 1 s   = 1,000,000,000 ns
_US_BOUNDARY_NS  = 1_000 * _NS_PER_US   # 1,000 µs — upper bound for µs display
_MS_BOUNDARY_NS  = 1_000 * _NS_PER_MS   # 1,000 ms — upper bound for ms display


def format_latency_ns(value_ns: float) -> str:
    """
    Formats a nanosecond duration with the most appropriate SI unit.

    Thresholds (user-defined adaptive rule):
      value_ns < 1,000,000       (<  1,000 µs)  → display in microseconds (µs)
      value_ns < 1,000,000,000   (< 1,000 ms)   → display in milliseconds (ms)
      value_ns >= 1,000,000,000  (>= 1 s)        → display in seconds (s)

    Args:
        value_ns: Latency in nanoseconds as recorded by HdrHistogram.

    Returns:
        Formatted string, e.g. "733.479 ms", "1.000 µs", or "5.302 s".
    """
    if value_ns < _US_BOUNDARY_NS:
        return f"{value_ns / _NS_PER_US:.3f} µs"
    if value_ns < _MS_BOUNDARY_NS:
        return f"{value_ns / _NS_PER_MS:.3f} ms"
    return f"{value_ns / _NS_PER_S:.3f} s"


def _severity_class(value_ns: float) -> str:
    """
    Maps a nanosecond latency to a CSS severity class for visual heat-mapping.

    Thresholds:
      sev-green:  < 100 µs   — excellent; GC-free hot path
      sev-yellow: < 1 ms     — acceptable for low-latency systems
      sev-orange: < 100 ms   — degraded; investigate scheduling or GC
      sev-red:    >= 100 ms  — critical; pipeline stall, GC pause, or backlog
    """
    if value_ns < 100_000:              # < 100 µs
        return "sev-green"
    if value_ns < _NS_PER_MS:           # < 1 ms
        return "sev-yellow"
    if value_ns < 100 * _NS_PER_MS:    # < 100 ms
        return "sev-orange"
    return "sev-red"


def _format_cell(value_ns: float) -> str:
    """Returns an HTML <span> with severity colour and adaptive unit label."""
    css = _severity_class(value_ns)
    return f'<span class="{css}">{format_latency_ns(value_ns)}</span>'


def get_percentile(hgrm_file: str, target_pct: float):
    """
    Parses an HdrHistogram .hgrm file and returns the value (in ns) whose
    recorded percentile is closest to target_pct.

    Args:
        hgrm_file:  Path to the .hgrm text file produced by HdrHistogram.
        target_pct: Target percentile in [0.0, 1.0] (e.g. 0.99 for P99).

    Returns:
        Tuple (value_ns: float, total_count: int).
        value_ns is 0.0 if the file is missing or empty.
    """
    closest_pct = -1.0
    closest_val = 0.0
    total_count = 0
    try:
        with open(hgrm_file, 'r') as f:
            for line in f:
                line = line.strip()
                if line.startswith('#'):
                    if "Total count" in line:
                        parts = line.split()
                        try:
                            total_count = int(parts[-1].rstrip(']'))
                        except (ValueError, IndexError):
                            pass
                    continue
                if not line or line.startswith('"'):
                    continue

                parts = line.split()
                if len(parts) >= 3:
                    try:
                        val = float(parts[0])  # value in nanoseconds
                        pct = float(parts[1])  # percentile in [0, 1]
                        if abs(pct - target_pct) < abs(closest_pct - target_pct):
                            closest_pct = pct
                            closest_val = val
                    except ValueError:
                        pass
    except OSError as e:
        print(f"Error parsing {hgrm_file}: {e}", file=sys.stderr)
        return 0.0, 0
    return closest_val, total_count


def file_to_base64(filepath: str) -> str:
    """Converts a binary file to a base64-encoded data URI for inline HTML embedding."""
    try:
        with open(filepath, "rb") as f:
            return f"data:image/png;base64,{base64.b64encode(f.read()).decode('utf-8')}"
    except OSError as e:
        print(f"Error reading image {filepath}: {e}", file=sys.stderr)
        return ""


def main():
    if len(sys.argv) < 2:
        print(f"Usage: {sys.argv[0]} <path_to1.hlog> [path_to2.hlog ...]",
              file=sys.stderr)
        sys.exit(1)

    hlog_files = sys.argv[1:]
    reports_data = []

    for hlog in hlog_files:
        hgrm = hlog + ".hgrm"
        png  = hlog + ".png"

        if not os.path.exists(hgrm):
            print(f"Warning: Missing data file {hgrm}", file=sys.stderr)
            continue

        name = os.path.basename(hlog)

        # All percentile values are stored as raw nanoseconds.
        # format_latency_ns() handles display-time unit conversion.
        p50,  total = get_percentile(hgrm, 0.50)
        p90,  _     = get_percentile(hgrm, 0.90)
        p99,  _     = get_percentile(hgrm, 0.99)
        p999, _     = get_percentile(hgrm, 0.999)
        p9999, _    = get_percentile(hgrm, 0.9999)
        max_val, _  = get_percentile(hgrm, 1.0)

        img_b64 = file_to_base64(png) if os.path.exists(png) else ""

        reports_data.append({
            "name":    name,
            "total":   total,
            "p50":     p50,
            "p90":     p90,
            "p99":     p99,
            "p999":    p999,
            "p9999":   p9999,
            "max":     max_val,
            "img_b64": img_b64,
        })

    if not reports_data:
        print("No valid reports generated.", file=sys.stderr)
        sys.exit(1)

    # ── HTML header + styles ──────────────────────────────────────────────────
    html_content = """<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Latency Benchmark Report</title>
    <style>
        :root {
            --bg-color: #f8f9fa;
            --text-color: #333;
            --card-bg: #fff;
            --border-color: #dee2e6;
            --header-bg: #e9ecef;
            --font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
        }
        body {
            font-family: var(--font-family);
            background-color: var(--bg-color);
            color: var(--text-color);
            margin: 0;
            padding: 2rem;
            line-height: 1.6;
        }
        .container { max-width: 1200px; margin: auto; }
        h1, h2, h3 { color: #212529; }
        .card {
            background: var(--card-bg);
            border: 1px solid var(--border-color);
            border-radius: 8px;
            padding: 20px;
            margin-bottom: 20px;
            box-shadow: 0 4px 6px rgba(0,0,0,0.05);
        }
        /* ── Callout blocks ── */
        .info  { background:#e3f2fd; border-left:5px solid #1976d2; padding:15px; margin-bottom:20px; border-radius:4px; font-size:.95rem; }
        .warn  { background:#fff3e0; border-left:5px solid #e65100; padding:15px; margin-bottom:16px; border-radius:4px; font-size:.95rem; }
        .fix   { background:#e8f5e9; border-left:5px solid #2e7d32; padding:15px; margin-bottom:16px; border-radius:4px; font-size:.95rem; }
        /* ── Table ── */
        table  { width:100%; border-collapse:collapse; margin:20px 0; }
        th, td { padding:12px 15px; text-align:right; border-bottom:1px solid var(--border-color); }
        th:first-child, td:first-child { text-align:left; }
        th     { background-color:var(--header-bg); font-weight:600; }
        tr:hover { background-color:#f1f3f5; }
        /* ── Severity heat-map ── */
        .sev-green  { color:#1b5e20; font-weight:600; }
        .sev-yellow { color:#f57f17; font-weight:600; }
        .sev-orange { color:#e65100; font-weight:700; }
        .sev-red    { color:#b71c1c; font-weight:700; background-color:#ffebee; border-radius:4px; padding:2px 6px; }
        /* ── Charts ── */
        img { max-width:100%; height:auto; display:block; margin:auto; border:1px solid var(--border-color); border-radius:4px; }
        .chart-container { margin-top:2rem; }
        pre { background:#f5f5f5; border:1px solid #ddd; border-radius:4px; padding:12px; font-size:.85rem; overflow-x:auto; }
        code { font-family: monospace; }
    </style>
</head>
<body>
<div class="container">
    <h1>High-Throughput Latency Benchmark Report</h1>

    <!-- ── Section 1: Metric explanation ─────────────────────────────────── -->
    <div class="card">
        <h2>Understanding the Metrics</h2>
        <div class="info">
            <p><strong>Why Percentiles instead of Averages?</strong></p>
            <p>In low-latency systems the average is highly misleading — it hides worst-case tail events.
            If 99 requests take 1&nbsp;&mu;s and 1 takes 1,000&nbsp;&mu;s the average is ~11&nbsp;&mu;s,
            masking the real outlier. Percentiles expose the full distribution:</p>
            <ul>
                <li><strong>P50 (Median):</strong> The "typical" experience — 50% of events were faster.</li>
                <li><strong>P90:</strong> 90% of events were faster; the top 10% are slower.</li>
                <li><strong>P99:</strong> Critical SLA boundary — 1 in 100 events exceeded this.</li>
                <li><strong>P99.9 / P99.99 ("The Nines"):</strong> Rarest worst tail. In a GC-free system with
                    true CPU isolation these should remain flat — no "hockey stick" rise.</li>
                <li><strong>Max:</strong> Single worst latency recorded during the entire run.</li>
            </ul>
            <p><em>Latency values are shown with <strong>adaptive units</strong>:
            &mu;s when &lt;&nbsp;1,000&nbsp;&mu;s &mdash;
            ms when &lt;&nbsp;1,000&nbsp;ms &mdash;
            s otherwise. Lower is always better.</em></p>
        </div>

        <h2>Pipeline Stage Definitions</h2>
        <div class="info">
            <p>Each histogram file measures a distinct segment of the FX pipeline.
            All timestamps are captured via <code>System.nanoTime()</code> and stamped directly
            onto the zero-allocation <code>FxMarketEvent</code> flyweight:</p>
            <ul>
                <li><strong>fx-latency-serv-0</strong> — Gateway internal processing:
                    FIX decode + correlation&nbsp;ID + queue-a write.
                    <em>T<sub>write</sub>&nbsp;&minus;&nbsp;T<sub>0</sub></em></li>
                <li><strong>fx-latency-queue-a</strong> — Queue-a wait:
                    time the event spent sitting in queue-a waiting for serv-a to consume it.
                    <em>T<sub>1entry</sub>&nbsp;&minus;&nbsp;T<sub>0</sub></em></li>
                <li><strong>fx-latency-serv-a</strong> — Risk validation processing:
                    credit check + status mutation.
                    <em>T<sub>1exit</sub>&nbsp;&minus;&nbsp;T<sub>1entry</sub></em></li>
                <li><strong>fx-latency-queue-b</strong> — Queue-b wait:
                    serv-a exit to serv-b entry.
                    <em>T<sub>2entry</sub>&nbsp;&minus;&nbsp;T<sub>1exit</sub></em></li>
                <li><strong>fx-latency-serv-b</strong> — Pricing engine processing:
                    spread application.
                    <em>T<sub>2exit</sub>&nbsp;&minus;&nbsp;T<sub>2entry</sub></em></li>
                <li><strong>fx-latency-queue-c</strong> — Queue-c wait:
                    serv-b exit to serv-c entry.
                    <em>T<sub>3entry</sub>&nbsp;&minus;&nbsp;T<sub>2exit</sub></em></li>
                <li><strong>fx-latency-serv-c</strong> — Persistence processing:
                    ring-buffer enqueue + async JDBC batch.
                    <em>T<sub>3exit</sub>&nbsp;&minus;&nbsp;T<sub>3entry</sub></em></li>
                <li><strong>fx-latency</strong> — End-to-end:
                    FIX ingress to persistence entry.
                    <em>T<sub>3entry</sub>&nbsp;&minus;&nbsp;T<sub>0</sub></em></li>
            </ul>
            <p>
                <strong>Severity key:</strong>&nbsp;
                <span class="sev-green">&#9632;&nbsp;&lt;100&nbsp;&mu;s — excellent</span>&nbsp;&nbsp;
                <span class="sev-yellow">&#9632;&nbsp;&lt;1&nbsp;ms — acceptable</span>&nbsp;&nbsp;
                <span class="sev-orange">&#9632;&nbsp;&lt;100&nbsp;ms — degraded</span>&nbsp;&nbsp;
                <span class="sev-red">&#9632;&nbsp;&ge;100&nbsp;ms — critical</span>
            </p>
        </div>
"""

    # ── Summary table ─────────────────────────────────────────────────────────
    html_content += """
        <h3>Summary Table</h3>
        <table>
            <thead>
                <tr>
                    <th>Run Name</th>
                    <th>Total Samples</th>
                    <th>P50</th>
                    <th>P90</th>
                    <th>P99</th>
                    <th>P99.9</th>
                    <th>P99.99</th>
                    <th>Max</th>
                </tr>
            </thead>
            <tbody>
"""

    for r in reports_data:
        html_content += f"""
                <tr>
                    <td><strong>{r['name']}</strong></td>
                    <td>{r['total']:,}</td>
                    <td>{_format_cell(r['p50'])}</td>
                    <td>{_format_cell(r['p90'])}</td>
                    <td>{_format_cell(r['p99'])}</td>
                    <td>{_format_cell(r['p999'])}</td>
                    <td>{_format_cell(r['p9999'])}</td>
                    <td>{_format_cell(r['max'])}</td>
                </tr>
"""

    html_content += """
            </tbody>
        </table>
    </div>

    <!-- ── Section 2: Root-Cause Analysis ───────────────────────────────── -->
    <div class="card">
        <h2>Root-Cause Analysis (RCA)</h2>

        <div class="warn">
            <h3>&#9888; Issue 1 &mdash; Queue-A: Systematic Producer-Consumer Backlog</h3>
            <p><strong>Observed:</strong>
            <code>fx-latency-queue-a</code> P50&nbsp;&asymp;&nbsp;733&nbsp;ms,
            Max&nbsp;&asymp;&nbsp;1,128&nbsp;ms.
            The distribution is flat from P50 to Max (only ~400&nbsp;ms spread), which is
            <em>not</em> a rare spike &mdash; it is a <strong>steady-state queue backlog</strong>.</p>

            <p><strong>Primary root cause &mdash; Producer-Consumer Rate Mismatch:</strong>
            The <code>LoadGenerator</code> injects events at a configured target rate
            (e.g.&nbsp;5M&nbsp;events/sec). The downstream pipeline consumes more slowly.
            Each event waits in queue-a for all preceding events to drain first.
            A 733&nbsp;ms P50 means the queue held roughly 730&nbsp;ms worth of unconsumed backlog
            at any given moment. The ~778,000 events that never reached serv-c
            (2,000,000 sent &minus; 1,221,641 persisted) confirm the unconsumed tail.</p>

            <p><strong>Contributing factors:</strong></p>
            <ul>
                <li><strong>macOS / Docker CPU scheduling:</strong>
                    <code>AffinityLock.acquireLock()</code> is advisory on macOS &mdash; the OS
                    scheduler can preempt serv-a for tens to hundreds of milliseconds, stalling
                    queue-a consumption and compounding the backlog.</li>
                <li><strong>Chronicle Queue mmap page faults:</strong>
                    When the 64&nbsp;MB queue store file rolls to a new segment the first access
                    triggers a kernel page fault (&gt;100&nbsp;ms on HDDs; 10&ndash;50&nbsp;ms on SSDs).
                    Pre-warming the queue at startup eliminates this one-time spike.</li>
                <li><strong>JIT compilation during warm-up:</strong>
                    C2 JIT compilation of hot methods triggers JVM safepoints of 200&ndash;500&nbsp;ms
                    in the first few seconds, contaminating early histogram buckets.</li>
            </ul>
        </div>

        <div class="warn">
            <h3>&#9888; Issue 2 &mdash; Queue-C: Bimodal Latency (P50&nbsp;=&nbsp;25&nbsp;ms, P99&nbsp;=&nbsp;5&nbsp;s)</h3>
            <p><strong>Observed:</strong>
            Half of events reach serv-c within ~25&nbsp;ms; the 99th percentile
            leaps to nearly 5&nbsp;seconds. This bimodal distribution is the fingerprint of
            periodic hot-path blocking.</p>

            <p><strong>Root cause &mdash; Ring Buffer Saturation in BatchPersistenceEngine:</strong>
            <code>BatchPersistenceEngine.accumulate()</code> spin-waits when the 65,536-slot
            async ring buffer is full:</p>
            <pre><code>while (w - r &gt;= RING_SIZE) Thread.onSpinWait();</code></pre>
            <p>When H2 <code>executeBatch()&nbsp;+&nbsp;commit()</code> in the <code>db-writer</code>
            background thread becomes slow (disk I/O, transaction overhead, JVM GC in that thread),
            the ring fills. The serv-c event-loop thread then stalls inside
            <code>accumulate()</code>. While stalled, serv-c cannot drain queue-c, causing
            a cascade of multi-second queue-c backlog.</p>
            <p>At 1M&nbsp;events/sec, a 65,536-slot ring covers only ~65&nbsp;ms of burst
            absorption. Any H2 commit taking longer than ~65&nbsp;ms will cause this stall.</p>

            <p><strong>Contributing factors:</strong></p>
            <ul>
                <li><strong>H2 transaction overhead:</strong>
                    In-memory H2 with <code>autoCommit=false</code> and per-batch
                    <code>commit()</code> has significant MVCC overhead at high insert rates.</li>
                <li><strong>Small MAX_BATCH:</strong>
                    4,096 rows per JDBC batch means frequent <code>commit()</code> calls,
                    each with non-trivial latency that compounds under high throughput.</li>
            </ul>
        </div>

        <div class="warn">
            <h3>&#9888; Issue 3 &mdash; serv-C: Tail Latency (Max&nbsp;&asymp;&nbsp;586&nbsp;ms)</h3>
            <p><strong>Root cause &mdash; Hot-path spin-wait inside <code>accumulate()</code>:</strong>
            The serv-c <code>handle()</code> method is directly blocked by ring buffer
            back-pressure for up to 586&nbsp;ms. This is simultaneously the cause of
            Issue&nbsp;2: while serv-c is spinning, it cannot read the next event from
            queue-c, so queue-c depth and wait time explode.</p>
        </div>
    </div>

    <!-- ── Section 3: Fix Plan ───────────────────────────────────────────── -->
    <div class="card">
        <h2>Fix Plan</h2>

        <div class="fix">
            <h3>&#10003; Fix 1 &mdash; Calibrate Load-Generator Rate (Immediate, Zero Code Change)</h3>
            <p>Reduce the target injection rate to match the pipeline's sustainable throughput.
            Run a short calibration pass at a low rate, measure actual consumed events/sec,
            then set target rate to 80% of that figure to leave headroom.</p>
            <pre><code># Calibration pass (direct mode, 500k msgs/sec, 2 million messages):
./scripts/run_benchmark_suite.sh /tmp/fx-queues/queue-a 500000 2000000 --direct
# Observe samples in fx-latency.hlog vs. fx-latency-serv-0.hlog to determine throughput.
# Set target &le; 80% of the pipeline&apos;s sustainable rate for your hardware.</code></pre>
        </div>

        <div class="fix">
            <h3>&#10003; Fix 2 &mdash; Enlarge BatchPersistenceEngine Ring Buffer</h3>
            <p>Increase <code>RING_SIZE</code> from 65,536 to 524,288 (512&nbsp;K slots) and
            raise <code>MAX_BATCH</code> from 4,096 to 32,768 in
            <code>BatchPersistenceEngine.java</code>.
            This provides ~500&nbsp;ms of burst absorption at 1M&nbsp;events/sec, preventing the
            serv-c spin-wait stall during H2 commit spikes.</p>
            <p><em>Memory cost: 524,288 &times; ~128 bytes per <code>BatchRow</code>
            &asymp; 64&nbsp;MB, pre-allocated at startup &mdash; zero GC after construction.</em></p>
        </div>

        <div class="fix">
            <h3>&#10003; Fix 3 &mdash; Tune H2 JDBC URL for Throughput</h3>
            <p>Add <code>LOG=0;UNDO_LOG=0;CACHE_SIZE=262144</code> to the JDBC URL
            to minimise transactional overhead. These settings are appropriate for
            benchmark / audit-log workloads where rollback is not required.</p>
            <pre><code>jdbc:h2:mem:fxdb;DB_CLOSE_DELAY=-1;MODE=MySQL;LOG=0;UNDO_LOG=0;CACHE_SIZE=262144</code></pre>
        </div>

        <div class="fix">
            <h3>&#10003; Fix 4 &mdash; Pre-Warm Chronicle Queue on Startup</h3>
            <p>Write a single dummy event to queue-a during service startup (before the
            benchmark begins) to pre-fault all 64&nbsp;MB mmap pages. This eliminates the
            first-access latency spike from the kernel page-fault handler.
            For production: store queues on <code>tmpfs</code> / RAM disk and add
            <code>-XX:+AlwaysPreTouch</code> to JVM flags.</p>
        </div>

        <div class="fix">
            <h3>&#10003; Fix 5 &mdash; Linux CPU Isolation for Production Benchmarks</h3>
            <p>macOS CPU affinity is advisory. For deterministic, reproducible results
            run on Linux with kernel isolation as documented in
            <code>BENCHMARK_TUNING.md</code>:
            <code>isolcpus</code>, <code>nohz_full</code>, <code>rcu_nocbs</code>,
            and <code>idle=poll</code>. Without isolation, the OS scheduler can preempt
            any event-loop thread for tens to hundreds of milliseconds.</p>
        </div>
    </div>

    <!-- ── Section 4: Distribution Charts ──────────────────────────────── -->
    <h2>Detailed Distribution Charts</h2>
    <div class="info">
        <p>Charts below plot percentile vs. latency on a logarithmic X-axis.
        A flat, horizontal line indicates excellent consistency (no GC pauses, no scheduling jitter).
        A steep "hockey stick" rising at P99+ indicates tail events caused by GC,
        mmap page faults, or OS preemption.</p>
    </div>
"""

    for r in reports_data:
        if r['img_b64']:
            html_content += f"""
    <div class="card chart-container">
        <h3>{r['name']}</h3>
        <img src="{r['img_b64']}" alt="Latency Distribution for {r['name']}">
    </div>
"""

    html_content += """
</div>
</body>
</html>
"""

    output_dir = os.path.dirname(os.path.abspath(hlog_files[0]))
    report_path = os.path.join(output_dir, "latency_report.html")

    with open(report_path, "w", encoding="utf-8") as f:
        f.write(html_content)

    print("==========================================")
    print(f"  Unified HTML Report generated at: {report_path}")
    print("==========================================")


if __name__ == "__main__":
    main()
