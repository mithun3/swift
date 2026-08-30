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

if [ "$CPU_PROFILE" = "auto" ]; then
    if [ "$(uname -s)" = "Darwin" ]; then
        CPU_PROFILE=desktop
    else
        CPU_PROFILE=isolated
    fi
fi

case "$CPU_PROFILE" in
    desktop)
        : "${FX_SERV_0_CPUSET:=0,5}"
        : "${FX_SERV_A_CPUSET:=1,6}"
        : "${FX_SERV_B_CPUSET:=2,7}"
        : "${FX_SERV_C_CPUSET:=3,8}"
        : "${FX_BENCHMARK_CPUSET:=4,9}"
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

if [ "$CPU_PROFILE" = "desktop" ]; then
    AVAILABLE_CPUS=$(docker info --format '{{.NCPU}}')
    if ! [[ "$AVAILABLE_CPUS" =~ ^[0-9]+$ ]] || [ "$AVAILABLE_CPUS" -lt 10 ]; then
        echo "The desktop CPU profile requires at least 10 Docker CPUs; found '${AVAILABLE_CPUS:-unknown}'." >&2
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

docker compose run --rm --no-deps benchmark \
    /app/scripts/run_load_generator.sh \
    /tmp/fx-queues/queue-a "$TARGET_RATE" "$MESSAGE_COUNT" --tcp

stop_pipeline

docker compose run --rm --no-deps benchmark \
    /app/scripts/process_latency.sh "${HLOG_FILES[@]}"
docker compose run --rm --no-deps benchmark \
    python3 /app/scripts/generate_html_report.py "${HLOG_FILES[@]}"

echo "Benchmark complete: fx-telemetry/latency_report.html"