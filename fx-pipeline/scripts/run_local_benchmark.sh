#!/usr/bin/env bash

set -euo pipefail

cd "$(dirname "$0")/.."

TARGET_RATE=${1:-10000}
MESSAGE_COUNT=${2:-1000000}
STARTUP_TIMEOUT_SECONDS=${FX_STARTUP_TIMEOUT_SECONDS:-30}
RUN_ID=${FX_RUN_ID:-local-$(date -u +%Y%m%dT%H%M%SZ)}
RUN_OUTPUT_DIR=${FX_RUN_OUTPUT_DIR:-benchmark-runs/$RUN_ID/local}

HLOG_FILES=(
    /tmp/fx-latency-queue-a.hlog
    /tmp/fx-latency-serv-0.hlog
    /tmp/fx-latency-serv-a.hlog
    /tmp/fx-latency-queue-b.hlog
    /tmp/fx-latency-serv-b.hlog
    /tmp/fx-latency-queue-c.hlog
    /tmp/fx-latency-serv-c.hlog
    /tmp/fx-latency.hlog
)

if [ -f logs/services.pid ]; then
    echo "Local services are already registered in logs/services.pid." >&2
    echo "Run ./scripts/stop.sh before starting a clean benchmark." >&2
    exit 1
fi

cleanup_failed_run() {
    exit_code=$?
    if [ "$exit_code" -ne 0 ] && [ -f logs/services.pid ]; then
        ./scripts/stop.sh || true
    fi
}
trap cleanup_failed_run EXIT

export FX_RUN_ID="$RUN_ID"

echo "Benchmark configuration:"
echo "  Environment: local"
echo "  Rate: $TARGET_RATE msg/s"
echo "  Count: $MESSAGE_COUNT"
echo "  Run ID: $RUN_ID"
echo "  Mode: tcp"

./scripts/start.sh

deadline=$((SECONDS + STARTUP_TIMEOUT_SECONDS))
while true; do
    all_ready=true
    for service in serv-0 serv-a serv-b serv-c; do
        if ! grep -q "Event loop started" "logs/${service}.log"; then
            all_ready=false
            break
        fi
    done

    if [ "$all_ready" = true ]; then
        break
    fi
    if [ "$SECONDS" -ge "$deadline" ]; then
        tail -n 100 logs/serv-0.log logs/serv-a.log logs/serv-b.log logs/serv-c.log
        echo "Local pipeline did not become ready within ${STARTUP_TIMEOUT_SECONDS}s." >&2
        exit 1
    fi
    sleep 1
done

# ── Phase 6: JIT Warm-up ─────────────────────────────────────────────────────
# Run unmeasured traffic before the real load to drive C2 JIT compilation
# to completion so safepoints do not contaminate the measurement window.
# FX_WARMUP_SECONDS=0 skips the warm-up phase.
WARMUP_SECS=${FX_WARMUP_SECONDS:-20}
WARMUP_RATE=$((TARGET_RATE / 10))
WARMUP_COUNT=$((WARMUP_RATE * WARMUP_SECS))

if [ "$WARMUP_SECS" -gt 0 ] && [ "$WARMUP_RATE" -gt 0 ]; then
    echo "==> JIT warm-up: ${WARMUP_SECS}s at ${WARMUP_RATE} msg/s (unmeasured)..."
    ./scripts/run_load_generator.sh \
        /tmp/fx-queues/queue-a "$WARMUP_RATE" "$WARMUP_COUNT" --tcp

    echo "==> Warm-up complete. Clearing histograms for clean measurement..."
    rm -f /tmp/fx-latency*.hlog
    sleep 2
fi

# ── Measured run ─────────────────────────────────────────────────────────────
./scripts/run_benchmark_suite.sh \
    /tmp/fx-queues/queue-a "$TARGET_RATE" "$MESSAGE_COUNT" --tcp \
    "${HLOG_FILES[@]}"

mkdir -p "$RUN_OUTPUT_DIR"
for hlog in "${HLOG_FILES[@]}"; do
    cp "$hlog" "$hlog.hgrm" "$RUN_OUTPUT_DIR/"
    if [ -f "$hlog.png" ]; then
        cp "$hlog.png" "$RUN_OUTPUT_DIR/"
    fi
done
cp /tmp/latency_report.html /tmp/run_manifest.json "$RUN_OUTPUT_DIR/"

echo "Benchmark complete: /tmp/latency_report.html"
echo "Archived run: $RUN_OUTPUT_DIR"