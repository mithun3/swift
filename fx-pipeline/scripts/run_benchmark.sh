#!/usr/bin/env bash

set -euo pipefail
cd "$(dirname "$0")/.."

PROFILE=""
ENV_LABEL=""
TARGET_RATE=""
MESSAGE_COUNT=""
LOAD_MODE_FLAG="--tcp"

while [[ "$#" -gt 0 ]]; do
    case $1 in
        --profile) PROFILE="$2"; shift 2 ;;
        --env-label) ENV_LABEL="$2"; shift 2 ;;
        --tcp|--direct) LOAD_MODE_FLAG="$1"; shift ;;
        *) 
            if [ -z "$TARGET_RATE" ]; then
                TARGET_RATE="$1"
            elif [ -z "$MESSAGE_COUNT" ]; then
                MESSAGE_COUNT="$1"
            else
                echo "Unknown parameter passed: $1"
                exit 1
            fi
            shift
            ;;
    esac
done

if [ -z "$PROFILE" ] || [ -z "$TARGET_RATE" ] || [ -z "$MESSAGE_COUNT" ]; then
    echo "Usage: $0 --profile <profile> [--env-label <label>] <target-rate> <message-count> [--tcp|--direct]"
    echo "Example: $0 --profile local --env-label baremetal_vultr 10000 1000000"
    exit 1
fi

export PROFILE
if [ ! -f "config/profiles/${PROFILE}.env" ]; then
    echo "Error: Profile config/profiles/${PROFILE}.env not found."
    exit 1
fi
# Load the configuration profile and export its variables
set -a
source "config/profiles/${PROFILE}.env"
set +a

export ENV_LABEL="${ENV_LABEL:-${FX_ENV_LABEL:-$PROFILE}}"

export FX_RUN_ID="${ENV_LABEL}-$(date -u +%Y%m%dT%H%M%SZ)"
RUN_OUTPUT_DIR=${FX_RUN_OUTPUT_DIR:-benchmark-runs/$ENV_LABEL/$FX_RUN_ID}
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
