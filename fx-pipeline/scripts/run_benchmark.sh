#!/usr/bin/env bash

set -euo pipefail
cd "$(dirname "$0")/.."

if [ "$#" -lt 3 ]; then
    echo "Usage: $0 --profile <profile> <target-rate> <message-count> [--tcp|--direct]"
    echo "Example: $0 --profile local 10000 1000000"
    exit 1
fi

if [ "$1" != "--profile" ]; then
    echo "Error: Must specify --profile <profile> as first arguments."
    exit 1
fi
export PROFILE=$2
TARGET_RATE=$3
MESSAGE_COUNT=$4
shift 4

LOAD_MODE_FLAG="--tcp"
if [ "${1:-}" = "--tcp" ] || [ "${1:-}" = "--direct" ]; then
    LOAD_MODE_FLAG="$1"
fi

if [ ! -f "config/profiles/${PROFILE}.env" ]; then
    echo "Error: Profile config/profiles/${PROFILE}.env not found."
    exit 1
fi
# Load the configuration profile and export its variables
set -a
source "config/profiles/${PROFILE}.env"
set +a

export FX_RUN_ID="${PROFILE}-$(date -u +%Y%m%dT%H%M%SZ)"
RUN_OUTPUT_DIR=${FX_RUN_OUTPUT_DIR:-benchmark-runs/$FX_RUN_ID/$PROFILE}
QUEUE_PATH="${FX_QUEUE_DIR}/queue-a"

source "scripts/runners/${FX_EXECUTION_MODE}_runner.sh"

echo "==========================================="
echo " Starting benchmark"
echo " Profile: $PROFILE"
echo " Execution Mode: $FX_EXECUTION_MODE"
echo " Target Rate: $TARGET_RATE msg/s"
echo " Message Count: $MESSAGE_COUNT"
echo " Run ID: $FX_RUN_ID"
echo "==========================================="

cleanup() {
    local exit_code=$?
    if [ "$exit_code" -ne 0 ]; then
        echo "Benchmark failed or interrupted. Cleaning up..."
        runner_stop_services || true
    fi
}
trap cleanup EXIT

runner_start_services
runner_wait_ready
runner_warmup "$QUEUE_PATH" "$TARGET_RATE" "$LOAD_MODE_FLAG"
runner_measure "$QUEUE_PATH" "$TARGET_RATE" "$MESSAGE_COUNT" "$LOAD_MODE_FLAG"
runner_stop_services

./scripts/generate_benchmark_report.sh "$TARGET_RATE" "$MESSAGE_COUNT" "$LOAD_MODE_FLAG" "$RUN_OUTPUT_DIR"

trap - EXIT
