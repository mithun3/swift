# FX Pipeline - Benchmark Reporting

This directory contains the interactive Streamlit dashboard for visualizing and comparing historical FX Pipeline benchmark runs.

It dynamically scans the `../benchmark-runs/` directory for archived HdrHistogram (`.hgrm`) results and metadata, allowing you to overlay latency distributions across different environments (e.g., local vs Docker) to quantify containerization overhead and identify P99 tail latency spikes.

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
The dashboard discovers runs dynamically. Whenever you execute a benchmark via `scripts/run_local_benchmark.sh` or `scripts/run_docker_benchmark.sh`, the results are automatically archived into `benchmark-runs/`. Simply refresh the Streamlit browser window to see the new runs appear in the sidebar selection!
