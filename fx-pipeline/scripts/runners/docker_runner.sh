#!/usr/bin/env bash

runner_start_services() {
    echo "Starting docker services..."
    local skip_build=${FX_SKIP_BUILD:-false}
    if [ "$skip_build" != "true" ]; then
        docker build -t fx-pipeline:latest .
    fi

    # Set up cpuset variables
    if [ "$FX_CPU_PROFILE" = "desktop" ]; then
        export FX_SERV_0_CPUSET="${FX_SERV_0_CPUSET:-0,5}"
        export FX_SERV_A_CPUSET="${FX_SERV_A_CPUSET:-1,6}"
        export FX_SERV_B_CPUSET="${FX_SERV_B_CPUSET:-2,7}"
        export FX_SERV_C_CPUSET="${FX_SERV_C_CPUSET:-3,8,9}"
        export FX_BENCHMARK_CPUSET="${FX_BENCHMARK_CPUSET:-4,10}"
    else
        export FX_SERV_0_CPUSET="${FX_SERV_0_CPUSET:-0}"
        export FX_SERV_A_CPUSET="${FX_SERV_A_CPUSET:-1}"
        export FX_SERV_B_CPUSET="${FX_SERV_B_CPUSET:-2}"
        export FX_SERV_C_CPUSET="${FX_SERV_C_CPUSET:-3}"
        export FX_BENCHMARK_CPUSET="${FX_BENCHMARK_CPUSET:-4}"
    fi

    PIPELINE_SERVICES=(serv-c serv-b serv-a serv-0)
    SERVICES=("${PIPELINE_SERVICES[@]}")
    SERVICES+=(telemetry)

    docker compose kill 2>/dev/null || true
    docker compose down --volumes --remove-orphans
    docker compose up -d "${SERVICES[@]}"
}

runner_wait_ready() {
    local timeout=${FX_STARTUP_TIMEOUT_SECONDS:-30}
    local deadline=$((SECONDS + timeout))
    echo "Waiting for docker services to be ready..."
    
    PIPELINE_SERVICES=(serv-c serv-b serv-a serv-0)
    SERVICES=("${PIPELINE_SERVICES[@]}")
    SERVICES+=(telemetry)

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
            echo "Pipeline did not become ready within ${timeout}s." >&2
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
        echo "==> JIT warm-up: ${warmup_secs}s at ${warmup_rate} msg/s (unmeasured)..."
        docker compose run --rm --no-deps benchmark \
            /app/scripts/run_load_generator.sh "$queue_path" "$warmup_rate" "$warmup_count" "$load_mode"

        echo "==> Warm-up complete. Clearing histograms for clean measurement..."
        for svc in serv-0 serv-a serv-b serv-c; do
            docker compose exec -T "${svc}" sh -c "rm -f ${FX_HLOG_DIR}/fx-latency*.hlog" 2>/dev/null || true
        done
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
    docker compose run --rm --no-deps benchmark \
        /app/scripts/run_load_generator.sh "$queue_path" "$target_rate" "$message_count" "$load_mode"
    export LOAD_DURATION_SECONDS=$((SECONDS - LOAD_START_SECONDS))
}

runner_stop_services() {
    local stop_timeout=${FX_STOP_TIMEOUT_SECONDS:-40}
    STOP_ORDER=(serv-0 serv-a serv-b serv-c telemetry)
    echo "Stopping docker services..."
    for service in "${STOP_ORDER[@]}"; do
        docker compose stop --timeout "$stop_timeout" "$service" || true
    done
}

