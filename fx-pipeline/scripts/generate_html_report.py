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
import html
import json

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


def parse_arguments(argv: list[str]):
    """Returns an optional manifest path and the positional histogram logs."""
    manifest_path = None
    hlog_files = []
    index = 0
    while index < len(argv):
        argument = argv[index]
        if argument == "--manifest":
            index += 1
            if index >= len(argv):
                raise ValueError("--manifest requires a JSON file path")
            manifest_path = argv[index]
        elif argument.startswith("--"):
            raise ValueError(f"Unknown option: {argument}")
        else:
            hlog_files.append(argument)
        index += 1
    return manifest_path, hlog_files


def load_manifest(manifest_path: str | None) -> dict:
    """Loads run metadata from JSON, or returns an empty manifest."""
    if manifest_path is None:
        return {}
    try:
        with open(manifest_path, "r", encoding="utf-8") as manifest_file:
            manifest = json.load(manifest_file)
    except (OSError, json.JSONDecodeError) as error:
        raise ValueError(f"Unable to load manifest {manifest_path}: {error}") from error
    if not isinstance(manifest, dict):
        raise ValueError("Run manifest must contain a JSON object")
    return manifest


def build_manifest_card(manifest: dict) -> str:
    """Renders benchmark provenance as an HTML-safe table."""
    if not manifest:
        return ""

    rows = []
    for key, value in manifest.items():
        if isinstance(value, (dict, list)):
            display_value = json.dumps(value, sort_keys=True)
        elif isinstance(value, bool):
            display_value = str(value).lower()
        elif isinstance(value, int):
            display_value = f"{value:,}"
        else:
            display_value = str(value)
        label = key.replace("_", " ").title()
        rows.append(
            f"<tr><th>{html.escape(label)}</th>"
            f"<td>{html.escape(display_value)}</td></tr>"
        )

    return """
    <div class="card">
        <h2>Run Configuration</h2>
        <table class="metadata">
            <tbody>
                %s
            </tbody>
        </table>
    </div>
""" % "\n                ".join(rows)


# Known per-stage .hlog filename suffixes, as produced by GatewayMain/PersistenceMain.
# Whichever report doesn't match any of these is assumed to be the end-to-end file.
_STAGE_SUFFIXES = (
    "-serv-0.hlog", "-queue-a.hlog", "-serv-a.hlog",
    "-queue-b.hlog", "-serv-b.hlog", "-queue-c.hlog", "-serv-c.hlog",
)


def _find_by_suffix(reports: list, suffix: str):
    """Returns the first report whose name ends with suffix, or None."""
    for r in reports:
        if r["name"].endswith(suffix):
            return r
    return None


def _find_e2e(reports: list):
    """Returns the end-to-end report — the one file that isn't a per-stage suffix."""
    for r in reports:
        if not any(r["name"].endswith(s) for s in _STAGE_SUFFIXES):
            return r
    return None


def build_reconciliation_banner(reports_data: list) -> str:
    """
    Compares serv-0's ingress sample count against the terminal stage's completed
    sample count and returns an HTML callout describing the result.

    A gap here means events were still in flight in queue-a/queue-b/queue-c when the
    benchmark was stopped — see AbstractEventLoop's drain-then-stop shutdown phase and
    stop.sh's staged shutdown order. Returns an empty string if serv-0 or a terminal
    stage report is missing from the supplied .hlog files (nothing to reconcile).
    """
    serv0 = _find_by_suffix(reports_data, "-serv-0.hlog")
    terminal = _find_by_suffix(reports_data, "-serv-c.hlog") or _find_e2e(reports_data)

    if serv0 is None or terminal is None:
        return ""

    ingress_count = serv0["total"]
    completed_count = terminal["total"]
    missing = ingress_count - completed_count

    if missing > 0:
        pct = (missing / ingress_count * 100) if ingress_count else 0.0
        return f"""
        <div class="warn">
            <h3>&#9888; {missing:,} event(s) unaccounted for ({pct:.1f}% of ingress)</h3>
            <p><strong>serv-0</strong> (<code>{serv0['name']}</code>) ingested
            <strong>{ingress_count:,}</strong> event(s), but only
            <strong>{completed_count:,}</strong> event(s) completed the full pipeline
            (<code>{terminal['name']}</code>). The difference means events were still in
            flight in queue-a/queue-b/queue-c when the benchmark was stopped.</p>
            <p>Check each service's stdout log for a
            <code>"Drain timeout (...) reached"</code> warning
            (see <code>AbstractEventLoop.drainRemainingBacklog</code>) to confirm which
            stage(s) did not fully drain within
            <code>-Dfx.eventloop.drainTimeoutMillis</code>.</p>
        </div>
"""
    if missing < 0:
        return f"""
        <div class="warn">
            <h3>&#9888; Terminal stage count ({completed_count:,}) exceeds serv-0 ingress count ({ingress_count:,})</h3>
            <p>This is unexpected and indicates either a telemetry double-recording bug or
            stale <code>.hlog</code> files from a previous run mixed into this report's
            file list — clear <code>/tmp/fx-latency*.hlog*</code> before the next run.</p>
        </div>
"""
    return f"""
        <div class="fix">
            <h3>&#10003; All {ingress_count:,} ingested event(s) accounted for</h3>
            <p>serv-0's ingress count matches the terminal stage's completed count exactly —
            the pipeline fully drained before the benchmark run was stopped.</p>
        </div>
"""


def main():
    try:
        manifest_path, hlog_files = parse_arguments(sys.argv[1:])
        manifest = load_manifest(manifest_path)
    except ValueError as error:
        print(f"Error: {error}", file=sys.stderr)
        sys.exit(2)

    if not hlog_files:
        print(f"Usage: {sys.argv[0]} [--manifest run.json] <path_to1.hlog> [path_to2.hlog ...]",
              file=sys.stderr)
        sys.exit(1)
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

    reconciliation_banner = build_reconciliation_banner(reports_data)
    manifest_card = build_manifest_card(manifest)

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
""" + manifest_card + """

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
"""
    html_content += reconciliation_banner
    html_content += """
    </div>

    <!-- ── Section 2: Distribution Charts ──────────────────────────────── -->
    <h2>Detailed Distribution Charts</h2>
    <div class="info">
        <p>Charts below plot percentile vs. latency on a logarithmic X-axis.
        A flat, horizontal line indicates excellent consistency (no GC pauses, no scheduling jitter).
        A steep "hockey stick" rising at P99+ indicates tail events caused by GC,
        mmap page faults, or OS preemption.</p>
        <p>See <code>PERFORMANCE_TUNING.md</code> in the project root — it is kept in sync with the current
        architecture constraints.</p>
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
