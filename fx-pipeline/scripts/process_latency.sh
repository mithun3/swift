#!/usr/bin/env bash

# ==============================================================================
# Script: process_latency.sh
# Description: Processes HdrHistogram .hlog files to generate .hgrm percentiles and latency plots.
# Usage: ./process_latency.sh <path_to.hlog>
# Example: ./process_latency.sh /tmp/fx-latency.hlog
# ==============================================================================

set -e

if [ "$#" -ne 1 ]; then
    echo "Usage: $0 <path_to.hlog>"
    echo "Example: $0 /tmp/fx-latency.hlog"
    exit 1
fi

HLOG_FILE="$1"
HGRM_FILE="${HLOG_FILE}.hgrm"

if [ ! -f "$HLOG_FILE" ]; then
    echo "Error: File not found: $HLOG_FILE"
    exit 1
fi

# Ensure we are in the project root
cd "$(dirname "$0")/.."

echo "=========================================="
echo "    Processing HdrHistogram Log"
echo "    File: $HLOG_FILE"
echo "=========================================="

# 1. Find HdrHistogram jar (future-proofed resolution)
HDR_JAR=""

# Check local targets first (built by our project)
if [ -d "test/target/dependency" ]; then
    HDR_JAR=$(find . -name "HdrHistogram-*.jar" -print -quit 2>/dev/null)
fi

# Check ~/.m2
if [ -z "$HDR_JAR" ]; then
    HDR_JAR=$(find ~/.m2/repository/org/hdrhistogram/HdrHistogram -name "HdrHistogram-*.jar" -print -quit 2>/dev/null || true)
fi

# Fallback to downloading via Maven if it's missing entirely
if [ -z "$HDR_JAR" ]; then
    echo "HdrHistogram jar not found locally. Attempting to download via Maven..."
    mvn dependency:get -Dartifact=org.hdrhistogram:HdrHistogram:2.2.2 -Dtransitive=false
    HDR_JAR=$(find ~/.m2/repository/org/hdrhistogram/HdrHistogram -name "HdrHistogram-*.jar" -print -quit 2>/dev/null || true)
fi

if [ -z "$HDR_JAR" ]; then
    echo "Error: Could not locate or download HdrHistogram jar."
    exit 1
fi

echo "Using HdrHistogram JAR: $HDR_JAR"

# 2. Process .hlog to .hgrm
echo "Extracting percentiles to $HGRM_FILE..."
TMP_PREFIX="${HLOG_FILE}.tmp"
java -cp "$HDR_JAR" org.HdrHistogram.HistogramLogProcessor -i "$HLOG_FILE" -o "$TMP_PREFIX"
mv "${TMP_PREFIX}.hgrm" "$HGRM_FILE"
rm -f "$TMP_PREFIX"

# 3. Generate the plot
if command -v python3 &>/dev/null; then
    echo "Generating latency plots..."
    python3 scripts/plot_latency.py "$HLOG_FILE"
else
    echo "Warning: python3 not found. Skipping plot generation."
    echo "To plot manually later, run: python3 scripts/plot_latency.py $HLOG_FILE"
fi

echo "=========================================="
echo "    Processing Complete!"
echo "=========================================="
