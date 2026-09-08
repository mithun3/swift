#!/usr/bin/env bash

# ==============================================================================
# Script: generate_benchmark_report.sh
# Description: Processes raw HdrHistogram logs and generates a unified HTML report.
#              Abstracts away execution mode (Docker vs Native) for processing.
# ==============================================================================

set -euo pipefail

if [ "$#" -lt 4 ]; then
    echo "Usage: $0 <target-rate> <message-count> <load-mode> <run-output-dir>"
    exit 1
fi

TARGET_RATE=$1
MESSAGE_COUNT=$2
LOAD_MODE=$3
RUN_OUTPUT_DIR=$4

cd "$(dirname "$0")/.."

# Resolve hlogs
# shellcheck disable=SC2206
HOST_HLOG_DIR="${FX_HOST_HLOG_DIR:-$FX_HLOG_DIR}"
HLOG_FILES=( "${HOST_HLOG_DIR}"/fx-latency*.hlog )
MANIFEST_PATH="${HOST_HLOG_DIR}/run_manifest.json"

echo "==========================================="
echo "    Processing Latency (.hlog to .hgrm)"
echo "==========================================="

if [ "$FX_EXECUTION_MODE" = "docker" ]; then
    # In docker mode, hlog files are mapped to host. 
    # Process them via a temporary container to ensure Java environment matches.
    # We substitute FX_HLOG_DIR (e.g. fx-telemetry) to /tmp/fx-telemetry in container context
    DOCKER_HLOG_FILES=()
    for hlog in "${HLOG_FILES[@]}"; do
        DOCKER_HLOG_FILES+=("${hlog/${HOST_HLOG_DIR}/\/tmp\/fx-telemetry}")
    done
    docker compose run --rm --no-deps benchmark \
        /app/scripts/process_latency.sh "${DOCKER_HLOG_FILES[@]}"
else
    # Native processing
    ./scripts/process_latency.sh "${HLOG_FILES[@]}"
fi

echo "==========================================="
echo "    Generating Run Manifest"
echo "==========================================="

if [ "$FX_EXECUTION_MODE" = "docker" ]; then
    RUNTIME_OS=$(docker info --format '{{.OSType}}')
    RUNTIME_ARCH=$(docker info --format '{{.Architecture}}')
    JVM_OPTIONS=$(docker image inspect fx-pipeline:latest --format '{{range .Config.Env}}{{println .}}{{end}}' | sed -n 's/^JVM_OPTS=//p')
    JDK_VERSION=$(docker run --rm fx-pipeline:latest java -version 2>&1 | head -n 1)
    CPU_COUNT=$(docker info --format '{{.NCPU}}')
    CPUSETS_JSON=$(printf '{"serv-0":"%s","serv-a":"%s","serv-b":"%s","serv-c":"%s","benchmark":"%s","telemetry":"unconstrained"}' \
        "${FX_SERV_0_CPUSET:-}" "${FX_SERV_A_CPUSET:-}" "${FX_SERV_B_CPUSET:-}" \
        "${FX_SERV_C_CPUSET:-}" "${FX_BENCHMARK_CPUSET:-}")
    CPU_PROFILE="${FX_CPU_PROFILE:-host}"
else
    if [ "$(uname -s)" = "Darwin" ]; then
        CPU_COUNT=$(sysctl -n hw.logicalcpu)
        RUNTIME_OS=$(sw_vers -productName)-$(sw_vers -productVersion)
    else
        CPU_COUNT=$(getconf _NPROCESSORS_ONLN)
        RUNTIME_OS=$(uname -s)-$(uname -r)
    fi
    RUNTIME_ARCH=$(uname -m)
    JVM_OPTIONS="${FX_JVM_OPTS_OVERRIDE:-default}"
    JDK_VERSION=$(java -version 2>&1 | head -n 1)
    CPUSETS_JSON="{}"
    CPU_PROFILE="host"
fi

python3 scripts/generate_run_manifest.py \
    --output "$MANIFEST_PATH" \
    --run-id "$FX_RUN_ID" \
    --environment "${ENV_LABEL:-$PROFILE}" \
    --target-rate "$TARGET_RATE" \
    --message-count "$MESSAGE_COUNT" \
    --actual-load-duration "${LOAD_DURATION_SECONDS:-0}" \
    --transport-mode "${LOAD_MODE#--}" \
    --trace-enabled true \
    --queue-path "${FX_QUEUE_DIR}/queue-a" \
    --cpu-count "$CPU_COUNT" \
    --cpu-profile "$CPU_PROFILE" \
    --cpusets "$CPUSETS_JSON" \
    --jvm-options "$JVM_OPTIONS" \
    --jdk-version "$JDK_VERSION" \
    --runtime-os "$RUNTIME_OS" \
    --runtime-arch "$RUNTIME_ARCH" \
    "${HLOG_FILES[@]}"

echo "==========================================="
echo "    Generating HTML Report"
echo "==========================================="

python3 scripts/generate_html_report.py --manifest "$MANIFEST_PATH" "${HLOG_FILES[@]}"

echo "==========================================="
echo "    Archiving Run Artifacts"
echo "==========================================="
mkdir -p "$RUN_OUTPUT_DIR"
for hlog in "${HLOG_FILES[@]}"; do
    cp "$hlog" "$hlog.hgrm" "$RUN_OUTPUT_DIR/"
    if [ -f "$hlog.png" ]; then cp "$hlog.png" "$RUN_OUTPUT_DIR/"; fi
done
cp "${HOST_HLOG_DIR}/latency_report.html" "$MANIFEST_PATH" "$RUN_OUTPUT_DIR/"

echo "Benchmark complete: ${HOST_HLOG_DIR}/latency_report.html"
echo "Archived run: $RUN_OUTPUT_DIR"
