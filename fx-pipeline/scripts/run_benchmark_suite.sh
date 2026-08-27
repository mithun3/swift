#!/usr/bin/env bash

# ==============================================================================
# Script: run_benchmark_suite.sh
# Description: Orchestrates the entire load generation and latency processing pipeline.
#              Executes the load generator, processes the resulting telemetry files,
#              and generates a final HTML latency report.
# Usage: ./run_benchmark_suite.sh <queue-path> <target-rate> <message-count> [--tcp|--direct] [hlog-files...]
# Modes:
#   --tcp    (default) Routes load through serv-0 via TCP. All 6 services record latency.
#            Requires the full pipeline (including serv-0) to already be running.
#   --direct Writes directly to queue-a, bypassing serv-0. Downstream latency only.
# Example (TCP, default): ./run_benchmark_suite.sh /tmp/fx-queues/queue-a 500000 5000000
# Example (direct):       ./run_benchmark_suite.sh /tmp/fx-queues/queue-a 500000 5000000 --direct
# Example (TCP + hlogs):  ./run_benchmark_suite.sh /tmp/fx-queues/queue-a 500000 5000000 --tcp /tmp/fx-latency*.hlog
# ==============================================================================

set -euo pipefail

if [ "$#" -lt 3 ]; then
    echo "Usage: $0 <queue-path> <target-rate> <message-count> [--tcp|--direct] [hlog-files...]"
    echo "Example (TCP, default): $0 /tmp/fx-queues/queue-a 500000 5000000"
    echo "Example (direct):       $0 /tmp/fx-queues/queue-a 500000 5000000 --direct"
    echo "Example (TCP + hlogs):  $0 /tmp/fx-queues/queue-a 500000 5000000 --tcp /tmp/fx-latency*.hlog"
    exit 1
fi

QUEUE_PATH=$1
TARGET_RATE=$2
MESSAGE_COUNT=$3

# Shift the first three arguments
shift 3

# Parse optional mode flag. Must come before any hlog file arguments.
# Default to --tcp so that serv-0 gateway telemetry is captured by default.
LOAD_MODE_FLAG="--tcp"
if [ "${1:-}" = "--tcp" ] || [ "${1:-}" = "--direct" ]; then
    LOAD_MODE_FLAG="$1"
    shift
elif [ -n "${1:-}" ] && [[ "${1:-}" == --* ]]; then
    echo "Error: Unknown flag '${1}'. Use --tcp or --direct."
    exit 1
fi

# NOTE: If no hlog files are explicitly provided, we defer the default glob
# expansion to AFTER services are stopped (see Step 1.5), so that all hlog
# files — including fx-latency-serv-0.hlog written by the gateway JVM — are
# present on disk before we resolve the file list.
if [ "$#" -eq 0 ]; then
    HLOG_FILES_ARG="default"
else
    HLOG_FILES=( "$@" )
    HLOG_FILES_ARG="explicit"
fi

# Ensure we are in the project root directory
cd "$(dirname "$0")/.."

echo "==========================================="
echo "    Benchmark Suite: Step 1/3"
echo "    Running Load Generator"
echo "    Mode: $LOAD_MODE_FLAG"
echo "==========================================="
./scripts/run_load_generator.sh "$QUEUE_PATH" "$TARGET_RATE" "$MESSAGE_COUNT" "$LOAD_MODE_FLAG"

echo "  Waiting 5 seconds for pipeline to drain queues before stopping..."
sleep 5

# ── Step 1.5: Stop all services before processing hlogs ──────────────────────
# CRITICAL ORDERING: All TelemetryRecorder instances hold a BufferedPrintStream
# that is only guaranteed to be fully flushed when TelemetryRecorder.close() is
# called (inside each service's JVM shutdown hook). We must stop the pipeline
# here — after the load run finishes — so that:
#   1. shutdown hooks fire and flush the final partial histogram interval to disk
#   2. the PrintStream is explicitly closed, ensuring all bytes reach the OS
# Without this step, fx-latency-serv-0.hlog and fx-latency-serv-c.hlog will
# be missing or empty when process_latency.sh runs.
echo "==========================================="
echo "    Benchmark Suite: Step 1.5/3"
echo "    Stopping Services (flushing telemetry)"
echo "==========================================="
if [ -f "logs/services.pid" ]; then
    ./scripts/stop.sh
else
    echo "  (No services.pid found — services may already be stopped. Continuing.)"
fi

# Resolve the hlog file list NOW — after stop.sh has fired all shutdown hooks.
# This guarantees fx-latency-serv-0.hlog (written by the gateway JVM's shutdown
# hook) exists on disk before we try to process it.
if [ "$HLOG_FILES_ARG" = "default" ]; then
    # shellcheck disable=SC2206
    HLOG_FILES=( /tmp/fx-latency*.hlog )
    echo "  Auto-detected hlog files:"
    for f in "${HLOG_FILES[@]}"; do echo "    $f"; done
fi

echo "==========================================="
echo "    Benchmark Suite: Step 2/3"
echo "    Processing Latency (.hlog to .hgrm and plots)"
echo "==========================================="
./scripts/process_latency.sh "${HLOG_FILES[@]}"

echo "==========================================="
echo "    Benchmark Suite: Step 3/3"
echo "    Generating HTML Report"
echo "==========================================="
if command -v python3 &>/dev/null; then
    python3 scripts/generate_html_report.py "${HLOG_FILES[@]}"
else
    echo "Warning: python3 not found. Skipping HTML report generation."
    echo "To generate the report manually later, run: python3 scripts/generate_html_report.py ${HLOG_FILES[*]}"
fi

echo "=========================================="
echo "    Benchmark Suite Complete!"
echo "=========================================="
