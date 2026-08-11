#!/usr/bin/env python3
"""
generate_html_report.py
Generates a comprehensive, human-readable HTML report from HdrHistogram data.
Extracts percentiles from .hgrm files and embeds .png charts using Base64.
"""

import sys
import os
import base64

def get_percentile(hgrm_file, target_pct):
    """Parses .hgrm to find the latency (in ns) closest to target_pct."""
    closest_pct = -1
    closest_val = 0
    total_count = 0
    try:
        with open(hgrm_file, 'r') as f:
            for line in f:
                line = line.strip()
                if line.startswith('#'):
                    if "Total count" in line:
                        parts = line.split()
                        try:
                            total_count = int(parts[-1])
                        except:
                            pass
                    continue
                if not line or line.startswith('"'):
                    continue
                
                parts = line.split()
                if len(parts) >= 3:
                    try:
                        val = float(parts[0]) # Value in ns
                        pct = float(parts[1]) # Percentile
                        if abs(pct - target_pct) < abs(closest_pct - target_pct):
                            closest_pct = pct
                            closest_val = val
                    except ValueError:
                        pass
    except Exception as e:
        print(f"Error parsing {hgrm_file}: {e}")
        return 0, 0
    return closest_val, total_count

def file_to_base64(filepath):
    """Converts a file to a base64 encoded data URI."""
    try:
        with open(filepath, "rb") as image_file:
            encoded_string = base64.b64encode(image_file.read()).decode("utf-8")
        return f"data:image/png;base64,{encoded_string}"
    except Exception as e:
        print(f"Error reading image {filepath}: {e}")
        return ""

def main():
    if len(sys.argv) < 2:
        print(f"Usage: {sys.argv[0]} <path_to1.hlog> [path_to2.hlog ...]")
        sys.exit(1)

    hlog_files = sys.argv[1:]
    reports_data = []

    for hlog in hlog_files:
        hgrm = hlog + ".hgrm"
        png = hlog + ".png"
        
        if not os.path.exists(hgrm):
            print(f"Warning: Missing data file {hgrm}")
            continue

        name = os.path.basename(hlog)
        
        # Get metrics (latency in ns, we will convert to us)
        p50, total = get_percentile(hgrm, 0.50)
        p90, _ = get_percentile(hgrm, 0.90)
        p99, _ = get_percentile(hgrm, 0.99)
        p999, _ = get_percentile(hgrm, 0.999)
        p9999, _ = get_percentile(hgrm, 0.9999)
        max_val, _ = get_percentile(hgrm, 1.0)
        
        img_b64 = file_to_base64(png) if os.path.exists(png) else ""
        
        reports_data.append({
            "name": name,
            "total": total,
            "p50": p50 / 1000.0,
            "p90": p90 / 1000.0,
            "p99": p99 / 1000.0,
            "p999": p999 / 1000.0,
            "p9999": p9999 / 1000.0,
            "max": max_val / 1000.0,
            "img_b64": img_b64
        })

    if not reports_data:
        print("No valid reports generated.")
        sys.exit(1)

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
            --accent: #007bff;
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
        .container {
            max-width: 1200px;
            margin: auto;
        }
        h1, h2, h3 { color: #212529; }
        .card {
            background: var(--card-bg);
            border: 1px solid var(--border-color);
            border-radius: 8px;
            padding: 20px;
            margin-bottom: 20px;
            box-shadow: 0 4px 6px rgba(0,0,0,0.05);
        }
        .explanation {
            background-color: #e3f2fd;
            border-left: 5px solid #1976d2;
            padding: 15px;
            margin-bottom: 20px;
            border-radius: 4px;
            font-size: 0.95rem;
        }
        table {
            width: 100%;
            border-collapse: collapse;
            margin: 20px 0;
        }
        th, td {
            padding: 12px 15px;
            text-align: right;
            border-bottom: 1px solid var(--border-color);
        }
        th:first-child, td:first-child { text-align: left; }
        th {
            background-color: var(--header-bg);
            font-weight: 600;
        }
        tr:hover { background-color: #f1f3f5; }
        img {
            max-width: 100%;
            height: auto;
            display: block;
            margin: auto;
            border: 1px solid var(--border-color);
            border-radius: 4px;
        }
        .chart-container { margin-top: 2rem; }
    </style>
</head>
<body>
    <div class="container">
        <h1>High-Throughput Latency Benchmark Report</h1>
        
        <div class="card">
            <h2>Understanding the Metrics</h2>
            <div class="explanation">
                <p><strong>Why do we look at Percentiles instead of Averages?</strong></p>
                <p>In low-latency and high-performance applications, the "average" (or mean) latency is highly misleading. It hides the worst-performing requests (the "tail"). If 99 requests take 1&mu;s, and 1 request takes 1,000&mu;s, the average is ~11&mu;s. This fails to show that a user experienced a massive delay. Instead, we use percentiles:</p>
                <ul>
                    <li><strong>P50 (Median):</strong> 50% of the messages were processed faster than this time. Represents the "typical" experience.</li>
                    <li><strong>P90:</strong> 90% of the messages were faster than this. (The top 10% are slower).</li>
                    <li><strong>P99:</strong> 99% of the messages were faster. This is a critical metric for robust system performance.</li>
                    <li><strong>P99.9 and beyond ("The Nines"):</strong> These represent the rarest but worst latencies (the "tail"). In systems processing millions of messages per second, P99.99 means measuring the slowest 1 in 10,000 messages. Consistently low "nines" prove the system is mechanically sympathetic and truly "Garbage Collection (GC) free".</li>
                    <li><strong>Max:</strong> The absolute slowest recorded latency during the run.</li>
                </ul>
                <p><em>Note: All times in the table below are expressed in <strong>microseconds (&mu;s)</strong> (1&mu;s = 1,000 nanoseconds). Lower is better.</em></p>
            </div>
            
            <h3>Summary Table</h3>
            <table>
                <thead>
                    <tr>
                        <th>Run Name</th>
                        <th>Total Samples</th>
                        <th>P50 (&mu;s)</th>
                        <th>P90 (&mu;s)</th>
                        <th>P99 (&mu;s)</th>
                        <th>P99.9 (&mu;s)</th>
                        <th>P99.99 (&mu;s)</th>
                        <th>Max (&mu;s)</th>
                    </tr>
                </thead>
                <tbody>
"""

    for r in reports_data:
        html_content += f"""
                    <tr>
                        <td><strong>{r['name']}</strong></td>
                        <td>{r['total']:,}</td>
                        <td>{r['p50']:,.2f}</td>
                        <td>{r['p90']:,.2f}</td>
                        <td>{r['p99']:,.2f}</td>
                        <td>{r['p999']:,.2f}</td>
                        <td>{r['p9999']:,.2f}</td>
                        <td>{r['max']:,.2f}</td>
                    </tr>
"""
    
    html_content += """
                </tbody>
            </table>
        </div>
        
        <h2>Detailed Distribution Charts</h2>
        <div class="explanation">
            <p>The charts below plot the percentile distribution on a logarithmic scale. A flat, horizontal line that barely rises towards the right edge indicates extreme consistency and highly optimized mechanical sympathy (lack of GC pauses or thread scheduling jitter).</p>
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

    report_path = "latency_report.html"
    with open(report_path, "w", encoding="utf-8") as f:
        f.write(html_content)
        
    print(f"==========================================")
    print(f"  Unified HTML Report generated at: {report_path}")
    print(f"==========================================")

if __name__ == "__main__":
    main()
