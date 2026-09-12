#!/usr/bin/env bash

runner_start_services() {
    echo "Starting native services..."
    local skip_build=${FX_SKIP_BUILD:-false}
    if [ "$skip_build" != "true" ]; then
        # Native has no build/tag step by default (unlike Docker's conditional
        # `docker build`), so identical git_sha never proved identical binaries —
        # rebuild by default; reuse is opt-in and explicit via FX_SKIP_BUILD=true.
        echo "Building native artifacts (set FX_SKIP_BUILD=true to reuse existing target/ jars)..."
        mvn -q clean package dependency:copy-dependencies -DskipTests
    else
        echo "FX_SKIP_BUILD=true — reusing existing target/ jars without rebuilding."
    fi
    if [ -f logs/services.pid ]; then
        echo "Services already running. Stopping them first..."
        ./scripts/stop.sh || true
    fi
    ./scripts/start.sh
}

runner_wait_ready() {
    local timeout=${FX_STARTUP_TIMEOUT_SECONDS:-30}
    local deadline=$((SECONDS + timeout))
    echo "Waiting for native services to be ready..."
    while true; do
        all_ready=true
        for service in serv-0 serv-a serv-b serv-c; do
            if [ ! -f "logs/${service}.log" ] || ! grep -q "Event loop started" "logs/${service}.log"; then
                all_ready=false
                break
            fi
        done
        if [ "$all_ready" = true ]; then break; fi
        if [ "$SECONDS" -ge "$deadline" ]; then
            echo "Services did not become ready in time." >&2
            exit 1
        fi
        sleep 1
    done
}

runner_warmup() {
    local queue_path=$1
    local target_rate=$2
    local load_mode=$3
    local warmup_secs=${FX_WARMUP_SECONDS:-20}
    local warmup_rate=$((target_rate / 10))
    local warmup_count=$((warmup_rate * warmup_secs))

    if [ "$warmup_secs" -gt 0 ] && [ "$warmup_rate" -gt 0 ]; then
        echo "==> JIT warm-up: ${warmup_secs}s at ${warmup_rate} msg/s..."
        ./scripts/run_load_generator.sh "$queue_path" "$warmup_rate" "$warmup_count" "$load_mode"
        echo "==> Warm-up complete. Clearing histograms..."
        rm -f ${FX_HLOG_DIR}/fx-latency*.hlog
        sleep 2
    fi
}

runner_measure() {
    local queue_path=$1
    local target_rate=$2
    local message_count=$3
    local load_mode=$4
    echo "==> Starting measured load..."
    export LOAD_START_SECONDS=$SECONDS
    ./scripts/run_load_generator.sh "$queue_path" "$target_rate" "$message_count" "$load_mode"
    export LOAD_DURATION_SECONDS=$((SECONDS - LOAD_START_SECONDS))
    sleep 1
}

runner_stop_services() {
    echo "Stopping native services..."
    ./scripts/stop.sh || true
}

