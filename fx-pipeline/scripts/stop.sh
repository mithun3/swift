#!/usr/bin/env bash

# ==============================================================================
# Script: stop.sh
# Description: Gracefully stops all FX Pipeline services, one at a time, in strict
#              producer-first pipeline order: serv-0 -> serv-a -> serv-b -> serv-c
#              -> telemetry. Each service is sent SIGTERM and fully awaited before
#              the next one is signalled.
#
#              This ordering matters: AbstractEventLoop now drains any backlog
#              still sitting in its input queue before actually exiting (see
#              common/src/main/java/com/fx/common/handler/AbstractEventLoop.java).
#              That drain is only safe/complete once the loop's producer has
#              fully stopped writing — otherwise a downstream stage could observe
#              "no data currently available", decide it has finished draining,
#              and exit while its upstream is still mid-flight, silently
#              re-introducing dropped/unprocessed backlog. Stopping upstream-first
#              and waiting for full exit before signalling the next stage
#              eliminates that race.
# Usage: ./scripts/stop.sh
# ==============================================================================

set -e

# Ensure we are in the project root directory
cd "$(dirname "$0")/.."

PID_FILE="logs/services.pid"

if [ ! -f "$PID_FILE" ]; then
    echo "No $PID_FILE found. Are the services running?"
    exit 0
fi

# Must exceed AbstractEventLoop's own backlog-drain timeout
# (-Dfx.eventloop.drainTimeoutMillis, default 30s) so we never declare a stall
# while a service is still legitimately draining its backlog.
WAIT_TIMEOUT_SECONDS=${FX_STOP_TIMEOUT_SECONDS:-40}

# Stops one labelled service (as written to services.pid by start.sh) and blocks
# until its process exits, or force-kills it after WAIT_TIMEOUT_SECONDS.
stop_service() {
    local label="$1"
    local pid
    pid=$(awk -v l="$label" '$2 == l { print $1 }' "$PID_FILE")

    if [ -z "$pid" ]; then
        echo "  (No PID found for $label — already stopped or never started.)"
        return
    fi
    if ! ps -p "$pid" > /dev/null 2>&1; then
        echo "  $label (PID $pid) is not running."
        return
    fi

    echo "  Stopping $label (PID $pid)..."
    kill -15 "$pid"

    local waited_half_seconds=0
    while ps -p "$pid" > /dev/null 2>&1; do
        sleep 0.5
        waited_half_seconds=$((waited_half_seconds + 1))
        if [ "$waited_half_seconds" -ge $((WAIT_TIMEOUT_SECONDS * 2)) ]; then
            echo "  WARNING: $label (PID $pid) did not exit within ${WAIT_TIMEOUT_SECONDS}s — forcing kill."
            kill -9 "$pid" 2>/dev/null || true
            break
        fi
    done
    echo "  $label stopped."
}

echo "=========================================="
echo "    Stopping FX Pipeline Services"
echo "    (staged, producer-first, so each stage"
echo "     can safely drain its own backlog)"
echo "=========================================="

stop_service "serv-0"
stop_service "serv-a"
stop_service "serv-b"
stop_service "serv-c"
stop_service "telemetry"

echo "Cleaning up $PID_FILE..."
rm -f "$PID_FILE"

echo "=========================================="
echo " All services stopped cleanly."
echo "=========================================="
