#!/usr/bin/env bash

set -euo pipefail

cd "$(dirname "$0")/.."

TARGET_RATE=${1:-150000}
MESSAGE_COUNT=${2:-100000}
STARTUP_TIMEOUT_SECONDS=${FX_STARTUP_TIMEOUT_SECONDS:-30}
STOP_TIMEOUT_SECONDS=${FX_STOP_TIMEOUT_SECONDS:-40}
SKIP_BUILD=${FX_SKIP_BUILD:-false}

SERVICES=(serv-c telemetry serv-b serv-a serv-0)
STOP_ORDER=(serv-0 serv-a serv-b serv-c telemetry)
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
docker compose up -d

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