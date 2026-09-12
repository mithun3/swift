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
        DOCKER_HLOG_FILES+=("${hlog/$HOST_HLOG_DIR//tmp/fx-telemetry}")
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

# Portable sha256 (macOS ships shasum, most Linux distros ship sha256sum).
sha256_of() {
    if [ ! -f "$1" ]; then
        echo "missing"
        return
    fi
    if command -v sha256sum >/dev/null 2>&1; then
        sha256sum "$1" | awk '{print $1}'
    else
        shasum -a 256 "$1" | awk '{print $1}'
    fi
}

# Reads a small status/exit-code file written by start.sh/stop.sh, defaulting to
# "unknown" so a missing file (older run, or a service that was never started)
# never breaks manifest generation.
read_status_file() {
    if [ -f "$1" ]; then
        cat "$1"
    else
        echo "unknown"
    fi
}

if [ "$(uname -s)" = "Darwin" ]; then
    HOST_CPU_MODEL=$(sysctl -n machdep.cpu.brand_string || echo "Unknown")
else
    HOST_CPU_MODEL=$(awk -F': ' '/model name/ {print $2; exit}' /proc/cpuinfo || echo "Unknown")
fi

# The actual wait strategy is a JVM -D flag baked into start.sh/docker-compose.yml's command
# line, not part of FX_JVM_OPTS_OVERRIDE — record the resolved value explicitly so it never
# has to be inferred from FX_JVM_OPTS_OVERRIDE (which does not contain it).
WAIT_STRATEGY="${FX_WAIT_STRATEGY:-phased}"

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
    # The telemetry container is unconditionally part of ALL_SERVICES in
    # docker_runner.sh today, so tracing is genuinely always on for docker runs
    # (unlike native, which gates it behind FX_ENABLE_TRACING).
    TRACE_ENABLED="true"
    DOCKER_IMAGE_DIGEST=$(docker image inspect fx-pipeline:latest --format '{{.Id}}' 2>/dev/null || echo "unknown")
    ARTIFACT_HASHES_JSON="{}"
    GC_LOG_PATHS_JSON=$(printf '{"serv-0":"%s","serv-a":"%s","serv-b":"%s","serv-c":"%s"}' \
        "./fx-telemetry/serv-0-gc.log" "./fx-telemetry/serv-a-gc.log" \
        "./fx-telemetry/serv-b-gc.log" "./fx-telemetry/serv-c-gc.log")
    EXIT_STATUS_JSON=$(printf '{"serv-0":"%s","serv-a":"%s","serv-b":"%s","serv-c":"%s"}' \
        "$(docker inspect fx-serv-0 --format 'exit_code={{.State.ExitCode}}' 2>/dev/null || echo unknown)" \
        "$(docker inspect fx-serv-a --format 'exit_code={{.State.ExitCode}}' 2>/dev/null || echo unknown)" \
        "$(docker inspect fx-serv-b --format 'exit_code={{.State.ExitCode}}' 2>/dev/null || echo unknown)" \
        "$(docker inspect fx-serv-c --format 'exit_code={{.State.ExitCode}}' 2>/dev/null || echo unknown)")
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
    # Record the actual taskset cpusets from the loaded profile (non-empty only).
    # An empty cpuset string means "unconstrained" (no taskset applied to that service).
    if [ -n "${FX_SERV_0_CPUSET:-}${FX_SERV_A_CPUSET:-}${FX_SERV_B_CPUSET:-}${FX_SERV_C_CPUSET:-}${FX_BENCHMARK_CPUSET:-}" ]; then
        CPUSETS_JSON=$(printf '{"serv-0":"%s","serv-a":"%s","serv-b":"%s","serv-c":"%s","benchmark":"%s","telemetry":"unconstrained"}' \
            "${FX_SERV_0_CPUSET:-}" "${FX_SERV_A_CPUSET:-}" "${FX_SERV_B_CPUSET:-}" \
            "${FX_SERV_C_CPUSET:-}" "${FX_BENCHMARK_CPUSET:-}")
    else
        CPUSETS_JSON="{}"
    fi
    CPU_PROFILE="${FX_CPU_PROFILE:-host}"
    TRACE_ENABLED="${FX_ENABLE_TRACING:-false}"
    DOCKER_IMAGE_DIGEST=""
    # Native has no image tag to pin identity to; hash the jars that actually ran instead.
    ARTIFACT_HASHES_JSON=$(printf '{"common":"%s","serv-0":"%s","serv-a":"%s","serv-b":"%s","serv-c":"%s"}' \
        "$(sha256_of common/target/common-1.0.0-SNAPSHOT.jar)" \
        "$(sha256_of serv-0/target/serv-0-1.0.0-SNAPSHOT.jar)" \
        "$(sha256_of serv-a/target/serv-a-1.0.0-SNAPSHOT.jar)" \
        "$(sha256_of serv-b/target/serv-b-1.0.0-SNAPSHOT.jar)" \
        "$(sha256_of serv-c/target/serv-c-1.0.0-SNAPSHOT.jar)")
    GC_LOG_PATHS_JSON=$(printf '{"serv-0":"%s","serv-a":"%s","serv-b":"%s","serv-c":"%s"}' \
        "logs/serv-0-gc.log" "logs/serv-a-gc.log" "logs/serv-b-gc.log" "logs/serv-c-gc.log")
    EXIT_STATUS_JSON=$(printf '{"serv-0":"%s","serv-a":"%s","serv-b":"%s","serv-c":"%s"}' \
        "$(read_status_file logs/serv-0.exitstatus)" "$(read_status_file logs/serv-a.exitstatus)" \
        "$(read_status_file logs/serv-b.exitstatus)" "$(read_status_file logs/serv-c.exitstatus)")
fi

python3 scripts/generate_run_manifest.py \
    --output "$MANIFEST_PATH" \
    --run-id "$FX_RUN_ID" \
    --environment "${ENV_LABEL:-$PROFILE}" \
    --target-rate "$TARGET_RATE" \
    --message-count "$MESSAGE_COUNT" \
    --actual-load-duration "${LOAD_DURATION_SECONDS:-0}" \
    --transport-mode "${LOAD_MODE#--}" \
    --trace-enabled "$TRACE_ENABLED" \
    --wait-strategy "$WAIT_STRATEGY" \
    --docker-image-digest "$DOCKER_IMAGE_DIGEST" \
    --artifact-hashes "$ARTIFACT_HASHES_JSON" \
    --gc-log-paths "$GC_LOG_PATHS_JSON" \
    --exit-status "$EXIT_STATUS_JSON" \
    --queue-path "${FX_QUEUE_DIR}/queue-a" \
    --cpu-count "$CPU_COUNT" \
    --cpu-profile "$CPU_PROFILE" \
    --cpu-model "$HOST_CPU_MODEL" \
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
    if [ -f "$hlog.intervals.csv" ]; then cp "$hlog.intervals.csv" "$RUN_OUTPUT_DIR/"; fi
done
cp "${HOST_HLOG_DIR}/latency_report.html" "$MANIFEST_PATH" "$RUN_OUTPUT_DIR/"

# GC logs and exit-status files are overwritten at the start of the next run —
# archive them now or this run's evidence is gone before anyone can look at it.
if [ "$FX_EXECUTION_MODE" = "docker" ]; then
    cp ./fx-telemetry/*-gc.log "$RUN_OUTPUT_DIR/" 2>/dev/null || true
else
    cp logs/*-gc.log logs/*.exitstatus "$RUN_OUTPUT_DIR/" 2>/dev/null || true
fi

echo "Benchmark complete: ${HOST_HLOG_DIR}/latency_report.html"
echo "Archived run: $RUN_OUTPUT_DIR"
