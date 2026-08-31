#!/usr/bin/env bash

set -euo pipefail

cd "$(dirname "$0")/.."

TARGET_RATE=${1:-150000}
MESSAGE_COUNT=${2:-100000}
STARTUP_TIMEOUT_SECONDS=${FX_STARTUP_TIMEOUT_SECONDS:-30}
STOP_TIMEOUT_SECONDS=${FX_STOP_TIMEOUT_SECONDS:-40}
SKIP_BUILD=${FX_SKIP_BUILD:-false}
TRACE_ENABLED=${FX_TRACE_ENABLED:-true}
CPU_PROFILE=${FX_CPU_PROFILE:-auto}
RUN_ID=${FX_RUN_ID:-docker-$(date -u +%Y%m%dT%H%M%SZ)}
MANIFEST_HOST_PATH=fx-telemetry/run_manifest.json
RUN_OUTPUT_DIR=${FX_RUN_OUTPUT_DIR:-benchmark-runs/$RUN_ID/docker}

if [ "$CPU_PROFILE" = "auto" ]; then
    if [ "$(uname -s)" = "Darwin" ]; then
        CPU_PROFILE=desktop
    else
        CPU_PROFILE=isolated
    fi
fi

case "$CPU_PROFILE" in
    desktop)
        # serv-c gets 3 vCPUs: event-loop (3), ZGC/JIT (8), db-writer (9).
        # The db-writer thread competes with ZGC/JIT on the shared cpuset; giving
        # serv-c a third vCPU ensures the event loop is never preempted by a
        # long H2 executeBatch()/commit() during the measured benchmark window.
        : "${FX_SERV_0_CPUSET:=0,5}"
        : "${FX_SERV_A_CPUSET:=1,6}"
        : "${FX_SERV_B_CPUSET:=2,7}"
        : "${FX_SERV_C_CPUSET:=3,8,9}"      # 3 vCPUs: event-loop + ZGC/JIT + db-writer
        : "${FX_BENCHMARK_CPUSET:=4,10}"
        ;;
    isolated)
        : "${FX_SERV_0_CPUSET:=0}"
        : "${FX_SERV_A_CPUSET:=1}"
        : "${FX_SERV_B_CPUSET:=2}"
        : "${FX_SERV_C_CPUSET:=3}"
        : "${FX_BENCHMARK_CPUSET:=4}"
        ;;
    *)
        echo "FX_CPU_PROFILE must be 'auto', 'desktop', or 'isolated'." >&2
        exit 1
        ;;
esac

export FX_SERV_0_CPUSET FX_SERV_A_CPUSET FX_SERV_B_CPUSET
export FX_SERV_C_CPUSET FX_BENCHMARK_CPUSET

PIPELINE_SERVICES=(serv-c serv-b serv-a serv-0)
SERVICES=("${PIPELINE_SERVICES[@]}")
STOP_ORDER=(serv-0 serv-a serv-b serv-c)

case "$TRACE_ENABLED" in
    true)
        SERVICES+=(telemetry)
        STOP_ORDER+=(telemetry)
        ;;
    false)
        ;;
    *)
        echo "FX_TRACE_ENABLED must be 'true' or 'false'." >&2
        exit 1
        ;;
esac

AVAILABLE_CPUS=$(docker info --format '{{.NCPU}}')
if [ "$CPU_PROFILE" = "desktop" ]; then
    # Desktop profile needs 11 CPUs: 5 services × 2 vCPUs each + 1 extra for serv-c db-writer.
    if ! [[ "$AVAILABLE_CPUS" =~ ^[0-9]+$ ]] || [ "$AVAILABLE_CPUS" -lt 11 ]; then
        echo "The desktop CPU profile requires at least 11 Docker CPUs; found '${AVAILABLE_CPUS:-unknown}'." >&2
        echo "Increase Docker Desktop's CPU limit or select FX_CPU_PROFILE=isolated explicitly." >&2
        exit 1
    fi
fi

HLOG_FILES=(
    /tmp/fx-telemetry/fx-latency-queue-a.hlog
    /tmp/fx-telemetry/fx-latency-serv-0.hlog
    /tmp/fx-telemetry/fx-latency-serv-a.hlog
    /tmp/fx-telemetry/fx-latency-queue-b.hlog
    /tmp/fx-telemetry/fx-latency-serv-b.hlog
    /tmp/fx-telemetry/fx-latency-queue-c.hlog
    /tmp/fx-telemetry/fx-latency-serv-c.hlog
    /tmp/fx-telemetry/fx-latency.hlog
)

echo "Benchmark configuration:"
echo "  Rate: $TARGET_RATE msg/s"
echo "  Count: $MESSAGE_COUNT"
echo "  Run ID: $RUN_ID"
echo "  CPU profile: $CPU_PROFILE"
echo "  CPU sets: serv-0=$FX_SERV_0_CPUSET serv-a=$FX_SERV_A_CPUSET serv-b=$FX_SERV_B_CPUSET serv-c=$FX_SERV_C_CPUSET benchmark=$FX_BENCHMARK_CPUSET"
echo "  JSON tracing: $TRACE_ENABLED"

stop_pipeline() {
    for service in "${STOP_ORDER[@]}"; do
        docker compose stop --timeout "$STOP_TIMEOUT_SECONDS" "$service"
    done
}

trap 'exit_code=$?; if [ "$exit_code" -ne 0 ]; then stop_pipeline || true; fi' EXIT

mkdir -p fx-data fx-telemetry
rm -f fx-telemetry/*.hlog fx-telemetry/*.hgrm fx-telemetry/*.png fx-telemetry/latency_report.html
rm -f "$MANIFEST_HOST_PATH"

if [ "$SKIP_BUILD" != "true" ]; then
    docker build -t fx-pipeline:latest .
fi

# Pre-run state is discarded, so force-stop stale containers before removing
# them. Graceful producer-first shutdown is used after the measured run.
docker compose kill 2>/dev/null || true
docker compose down --volumes --remove-orphans
docker compose up -d "${SERVICES[@]}"

deadline=$((SECONDS + STARTUP_TIMEOUT_SECONDS))
while true; do
    all_running=true
    for service in "${SERVICES[@]}"; do
        container_id=$(docker compose ps -q "$service")
        if [ -z "$container_id" ] || [ "$(docker inspect --format '{{.State.Running}}' "$container_id")" != "true" ]; then
            all_running=false
            break
        fi
    done

    if [ "$all_running" = true ] &&
       docker compose logs --no-color serv-0 | grep -q "Event loop started" &&
       docker compose logs --no-color serv-a | grep -q "Event loop started" &&
       docker compose logs --no-color serv-b | grep -q "Event loop started" &&
       docker compose logs --no-color serv-c | grep -q "Event loop started"; then
        break
    fi

    if [ "$SECONDS" -ge "$deadline" ]; then
        docker compose ps -a
        docker compose logs --tail=100 serv-0 serv-a serv-b serv-c
        echo "Pipeline did not become ready within ${STARTUP_TIMEOUT_SECONDS}s." >&2
        exit 1
    fi
    sleep 1
done

LOAD_START_SECONDS=$SECONDS

# ── Phase 6: JIT Warm-up ─────────────────────────────────────────────────────
# Run unmeasured traffic before the real load to drive C2 JIT compilation
# to completion. Without warm-up, safepoints (200-500 ms) from JIT compilation
# overlap with the measurement window, inflating P99.9/P99.99 histograms.
# FX_WARMUP_SECONDS=0 skips the warm-up phase entirely.
WARMUP_SECS=${FX_WARMUP_SECONDS:-20}
WARMUP_RATE=$((TARGET_RATE / 10))
WARMUP_COUNT=$((WARMUP_RATE * WARMUP_SECS))

if [ "$WARMUP_SECS" -gt 0 ] && [ "$WARMUP_RATE" -gt 0 ]; then
    echo "==> JIT warm-up: ${WARMUP_SECS}s at ${WARMUP_RATE} msg/s (unmeasured)..."
    docker compose run --rm --no-deps benchmark \
        /app/scripts/run_load_generator.sh \
        /tmp/fx-queues/queue-a "$WARMUP_RATE" "$WARMUP_COUNT" --tcp

    echo "==> Warm-up complete. Resetting telemetry histograms for clean measurement..."
    # Truncate hlog files so warm-up samples do not contaminate the measured run.
    # The TelemetryRecorder background thread will recreate them on the next flush.
    for svc in serv-0 serv-a serv-b serv-c; do
        docker compose exec -T "${svc}" sh -c \
            'rm -f /tmp/fx-telemetry/fx-latency*.hlog' 2>/dev/null || true
    done
    # Brief settle — allow any warm-up tail events to drain before measurement.
    sleep 2
fi

# ── Measured load ────────────────────────────────────────────────────────────
LOAD_START_SECONDS=$SECONDS
docker compose run --rm --no-deps benchmark \
    /app/scripts/run_load_generator.sh \
    /tmp/fx-queues/queue-a "$TARGET_RATE" "$MESSAGE_COUNT" --tcp
LOAD_DURATION_SECONDS=$((SECONDS - LOAD_START_SECONDS))

stop_pipeline

docker compose run --rm --no-deps benchmark \
    /app/scripts/process_latency.sh "${HLOG_FILES[@]}"

HLOG_HOST_FILES=()
for hlog in "${HLOG_FILES[@]}"; do
    HLOG_HOST_FILES+=("fx-telemetry/$(basename "$hlog")")
done

DOCKER_RUNTIME_OS=$(docker info --format '{{.OSType}}')
DOCKER_RUNTIME_ARCH=$(docker info --format '{{.Architecture}}')
DOCKER_JVM_OPTIONS=$(docker image inspect fx-pipeline:latest --format '{{range .Config.Env}}{{println .}}{{end}}' | sed -n 's/^JVM_OPTS=//p')
DOCKER_JDK_VERSION=$(docker run --rm fx-pipeline:latest java -version 2>&1 | head -n 1)
CPUSETS_JSON=$(printf '{"serv-0":"%s","serv-a":"%s","serv-b":"%s","serv-c":"%s","benchmark":"%s","telemetry":"unconstrained"}' \
    "$FX_SERV_0_CPUSET" "$FX_SERV_A_CPUSET" "$FX_SERV_B_CPUSET" \
    "$FX_SERV_C_CPUSET" "$FX_BENCHMARK_CPUSET")

python3 scripts/generate_run_manifest.py \
    --output "$MANIFEST_HOST_PATH" \
    --run-id "$RUN_ID" \
    --environment docker \
    --target-rate "$TARGET_RATE" \
    --message-count "$MESSAGE_COUNT" \
    --actual-load-duration "$LOAD_DURATION_SECONDS" \
    --transport-mode tcp \
    --trace-enabled "$TRACE_ENABLED" \
    --queue-path /tmp/fx-queues/queue-a \
    --cpu-count "$AVAILABLE_CPUS" \
    --cpu-profile "$CPU_PROFILE" \
    --cpusets "$CPUSETS_JSON" \
    --jvm-options "$DOCKER_JVM_OPTIONS" \
    --jdk-version "$DOCKER_JDK_VERSION" \
    --runtime-os "$DOCKER_RUNTIME_OS" \
    --runtime-arch "$DOCKER_RUNTIME_ARCH" \
    "${HLOG_HOST_FILES[@]}"

python3 scripts/generate_html_report.py \
    --manifest "$MANIFEST_HOST_PATH" "${HLOG_HOST_FILES[@]}"

mkdir -p "$RUN_OUTPUT_DIR"
for hlog in "${HLOG_HOST_FILES[@]}"; do
    cp "$hlog" "$hlog.hgrm" "$RUN_OUTPUT_DIR/"
    if [ -f "$hlog.png" ]; then
        cp "$hlog.png" "$RUN_OUTPUT_DIR/"
    fi
done
cp fx-telemetry/latency_report.html "$MANIFEST_HOST_PATH" "$RUN_OUTPUT_DIR/"

echo "Benchmark complete: fx-telemetry/latency_report.html"
echo "Archived run: $RUN_OUTPUT_DIR"