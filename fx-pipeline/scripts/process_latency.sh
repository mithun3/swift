#!/usr/bin/env bash

# ==============================================================================
# Script: process_latency.sh
# Description: Processes HdrHistogram .hlog files to generate .hgrm percentiles and latency plots.
#              (Note: HTML report generation is handled separately via generate_html_report.py)
# Usage: ./process_latency.sh <path_to.hlog>
# Example: ./process_latency.sh /tmp/fx-latency.hlog
# ==============================================================================

set -euo pipefail

if [ "$#" -lt 1 ]; then
    echo "Usage: $0 <path_to.hlog> [path_to2.hlog ...]"
    echo "Example: $0 /tmp/fx-latency.hlog /tmp/fx-latency-queue-a.hlog"
    exit 1
fi

# Ensure we are in the project root
cd "$(dirname "$0")/.."

# 1. Find HdrHistogram jar (future-proofed resolution)
HDR_JAR=""

# Check local targets first (built by our project)
if [ -d "test/target/dependency" ]; then
    HDR_JAR=$(find . -name "HdrHistogram-*.jar" -not -name "*-javadoc.jar" -not -name "*-sources.jar" -print -quit 2>/dev/null)
fi

# Check ~/.m2
if [ -z "$HDR_JAR" ]; then
    HDR_JAR=$(find ~/.m2/repository/org/hdrhistogram/HdrHistogram -name "HdrHistogram-*.jar" -not -name "*-javadoc.jar" -not -name "*-sources.jar" -print -quit 2>/dev/null || true)
fi

# Fallback to downloading via Maven if it's missing entirely
if [ -z "$HDR_JAR" ]; then
    echo "HdrHistogram jar not found locally. Attempting to download via Maven..."
    mvn dependency:get -Dartifact=org.hdrhistogram:HdrHistogram:2.2.2 -Dtransitive=false
    HDR_JAR=$(find ~/.m2/repository/org/hdrhistogram/HdrHistogram -name "HdrHistogram-*.jar" -not -name "*-javadoc.jar" -not -name "*-sources.jar" -print -quit 2>/dev/null || true)
fi

if [ -z "$HDR_JAR" ]; then
    echo "Error: Could not locate or download HdrHistogram jar."
    exit 1
fi

echo "Using HdrHistogram JAR: $HDR_JAR"

# Tracking arrays for the final summary
PROCESSED_FILES=()
SKIPPED_FILES=()

for HLOG_FILE in "$@"; do
    if [ ! -f "$HLOG_FILE" ]; then
        echo "Warning: File not found: $HLOG_FILE. Skipping."
        SKIPPED_FILES+=("$HLOG_FILE (not found)")
        continue
    fi

    echo "=========================================="
    echo "    Processing $HLOG_FILE"
    echo "=========================================="

    HGRM_FILE="${HLOG_FILE}.hgrm"

    # 2. Process .hlog to .hgrm
    echo "Extracting percentiles to $HGRM_FILE..."
    TMP_PREFIX="${HLOG_FILE}.tmp"
    # Capture output since HistogramLogProcessor exits with 0 even on NullPointerException
    OUTPUT=$(java -cp "$HDR_JAR" org.HdrHistogram.HistogramLogProcessor -i "$HLOG_FILE" -o "$TMP_PREFIX" -outputValueUnitRatio 1 2>&1 || true)
    
    if ! echo "$OUTPUT" | grep -q "Exception"; then
        mv "${TMP_PREFIX}.hgrm" "$HGRM_FILE"
        rm -f "${TMP_PREFIX}"*
        PROCESSED_FILES+=("$HLOG_FILE")
    else
        FILE_SIZE=$(wc -c < "$HLOG_FILE" 2>/dev/null || echo "unknown")
        echo "Warning: Failed to extract percentiles from $HLOG_FILE"
        echo "         File size: ${FILE_SIZE} bytes (0 or very small = no data recorded; may need a longer run)"
        rm -f "${TMP_PREFIX}"*
        SKIPPED_FILES+=("$HLOG_FILE (HistogramLogProcessor failed — ${FILE_SIZE} bytes)")
        continue
    fi

    # 3. Export per-interval percentiles (pinpoints exactly when a spike happened,
    #    instead of only seeing it in the aggregate .hgrm distribution). Non-fatal:
    #    a missing common jar or an unreadable log must not fail the whole benchmark.
    INTERVALS_FILE="${HLOG_FILE}.intervals.csv"
    COMMON_JAR="common/target/common-1.0.0-SNAPSHOT.jar"
    if [ -f "$COMMON_JAR" ]; then
        echo "Exporting per-interval percentiles to $INTERVALS_FILE..."
        java -cp "$HDR_JAR:$COMMON_JAR" com.fx.common.telemetry.IntervalHistogramExporter \
            "$HLOG_FILE" "$INTERVALS_FILE" || echo "Warning: interval export failed for $HLOG_FILE"
    else
        echo "Warning: $COMMON_JAR not found; skipping interval export for $HLOG_FILE"
    fi

    # 4. Generate the plot
    if command -v python3 &>/dev/null; then
        echo "Generating latency plots..."
        python3 scripts/plot_latency.py "$HLOG_FILE"
    else
        echo "Warning: python3 not found. Skipping plot generation."
        echo "To plot manually later, run: python3 scripts/plot_latency.py $HLOG_FILE"
    fi
done

echo "=========================================="
echo "    Processing Complete!"
echo "=========================================="

if [ ${#PROCESSED_FILES[@]} -gt 0 ]; then
    echo "  Successfully processed (${#PROCESSED_FILES[@]}):"
    for f in "${PROCESSED_FILES[@]}"; do echo "    ✓ $f"; done
fi
if [ ${#SKIPPED_FILES[@]} -gt 0 ]; then
    echo "  Skipped (${#SKIPPED_FILES[@]}):"
    for f in "${SKIPPED_FILES[@]}"; do echo "    ✗ $f"; done
fi
