#!/usr/bin/env bash

# ==============================================================================
# Script: stop.sh
# Description: Gracefully stops all FX Pipeline services by sending SIGTERM (kill -15).
# Usage: ./scripts/stop.sh
# ==============================================================================

set -e

# Ensure we are in the project root directory
cd "$(dirname "$0")/.."

PID_FILE="logs/services.pid"

if [ ! -f "$PID_FILE" ]; then
    echo "No $PID_FILE found. Are the services running?"
    exit 0
fi

echo "=========================================="
echo "    Stopping FX Pipeline Services"
echo "=========================================="

# Read PIDs into an array
mapfile -t PIDS < "$PID_FILE"

if [ ${#PIDS[@]} -eq 0 ]; then
    echo "No PIDs found in $PID_FILE."
    rm -f "$PID_FILE"
    exit 0
fi

for PID in "${PIDS[@]}"; do
    if ps -p "$PID" > /dev/null 2>&1; then
        echo "Sending SIGTERM (kill -15) to PID $PID..."
        kill -15 "$PID"
    else
        echo "PID $PID is not running."
    fi
done

echo "Waiting for services to shut down..."

# Wait for processes to exit gracefully
for PID in "${PIDS[@]}"; do
    while ps -p "$PID" > /dev/null 2>&1; do
        sleep 0.5
    done
    echo "PID $PID has stopped."
done

echo "Cleaning up $PID_FILE..."
rm -f "$PID_FILE"

echo "=========================================="
echo " All services stopped cleanly."
echo "=========================================="
