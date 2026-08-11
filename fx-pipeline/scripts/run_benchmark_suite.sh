#!/usr/bin/env bash

# ==============================================================================
# Script: run_benchmark_suite.sh
# Description: Orchestrates the entire load generation and latency processing pipeline.
#              Executes the load generator, processes the resulting telemetry files,
#              and generates a final HTML latency report.
# Usage: ./run_benchmark_suite.sh <queue-path> <target-rate> <message-count> [hlog-files...]
# Example: ./run_benchmark_suite.sh /tmp/fx-queues/queue-a 500000 5000000 /tmp/fx-latency*.hlog
# ==============================================================================

set -euo pipefail

if [ "$#" -lt 3 ]; then
    echo "Usage: $0 <queue-path> <target-rate> <message-count> [hlog-files...]"
    echo "Example: $0 /tmp/fx-queues/queue-a 500000 5000000 /tmp/fx-latency*.hlog"
    exit 1
fi

QUEUE_PATH=$1
TARGET_RATE=$2
MESSAGE_COUNT=$3

# Shift the first three arguments so that $@ only contains the hlog-files
shift 3

# If no hlog files are explicitly provided, use the default pattern
if [ "$#" -eq 0 ]; then
    HLOG_FILES=( /tmp/fx-latency*.hlog )
else
    HLOG_FILES=( "$@" )
fi

# Ensure we are in the project root directory
cd "$(dirname "$0")/.."

echo "=========================================="
echo "    Benchmark Suite: Step 1/3"
echo "    Running Load Generator"
echo "=========================================="
./scripts/run_load_generator.sh "$QUEUE_PATH" "$TARGET_RATE" "$MESSAGE_COUNT"

echo "=========================================="
echo "    Benchmark Suite: Step 2/3"
echo "    Processing Latency (.hlog to .hgrm and plots)"
echo "=========================================="
./scripts/process_latency.sh "${HLOG_FILES[@]}"

echo "=========================================="
echo "    Benchmark Suite: Step 3/3"
echo "    Generating HTML Report"
echo "=========================================="
if command -v python3 &>/dev/null; then
    python3 scripts/generate_html_report.py "${HLOG_FILES[@]}"
else
    echo "Warning: python3 not found. Skipping HTML report generation."
    echo "To generate the report manually later, run: python3 scripts/generate_html_report.py ${HLOG_FILES[*]}"
fi

echo "=========================================="
echo "    Benchmark Suite Complete!"
echo "=========================================="
