# FX Pipeline - Benchmark Reporting

This directory contains the interactive Streamlit dashboard for visualizing and comparing historical FX Pipeline benchmark runs.

It dynamically scans the `../benchmark-runs/` directory for archived HdrHistogram (`.hgrm`) results and metadata, allowing you to overlay latency distributions across different environments (e.g., local vs Docker) to quantify containerization overhead and identify P99 tail latency spikes.

## Key Features

- **KPI Dashboard**: Instant visibility into top-level metrics such as Total Runs, Best P99 Latency, and Latest P99 Latency directly in the Summary tab.
- **Dynamic Unit Formatting**: Select between Nanoseconds (ns), Microseconds (µs), and Milliseconds (ms) from the sidebar for readability.
- **CSV Export**: Instantly download the summary metrics of all your filtered runs into a CSV for offline analysis.
- **Percentile Tracking**: Tracks P50, P90, P99, P99.9, P99.99, and Max latencies for fine-grained performance visibility.
- **A/B Diffing**: Granularly compare the latency characteristics of two pipeline stages side-by-side.

## Prerequisites

You need Python 3.8+ installed on your system.

## Initial Setup

Before running the dashboard for the first time, install the required Python dependencies. It is recommended to use a virtual environment, though it is not strictly required.

```bash
# Optional: Create and activate a virtual environment
python3 -m venv venv
source venv/bin/activate

# Install the required packages
pip3 install -r requirements.txt
```

## Running the Dashboard

To launch the interactive dashboard, ensure you are in the root directory of the `fx-pipeline` project (not inside the `reporting/` folder itself) so that it can correctly locate the `benchmark-runs/` directory.

Run the following command from the `fx-pipeline` root:

```bash
streamlit run reporting/app.py
```

Streamlit will boot up a local web server (typically on `http://localhost:8501`) and automatically open a browser window displaying the interactive charts.

## Adding New Data
The dashboard discovers runs dynamically. Whenever you execute a benchmark via `scripts/run_benchmark.sh`, the results are automatically archived into `benchmark-runs/`. Simply refresh the Streamlit browser window to see the new runs appear in the sidebar selection!
