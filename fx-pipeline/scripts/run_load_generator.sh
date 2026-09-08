#!/usr/bin/env bash

# ==============================================================================
# Script: run_load_generator.sh
# Description: Executes the high-throughput garbage-free load generator
# Usage: ./run_load_generator.sh <queue-path> <target-rate> [message-count] [--tcp|--direct]
# Modes:
#   --tcp    (default) Connects to serv-0 on :5001 and injects raw FIX bytes over TCP.
#            serv-0 must already be running in tcp mode before calling this script.
#   --direct Writes directly to the Chronicle Queue at <queue-path>, bypassing serv-0.
#            Use this for downstream-only latency measurement.
# Example 1: ./run_load_generator.sh /tmp/fx-queues/queue-a 500000 500000
#   -> TCP mode (default): 500k msgs/sec through serv-0.
# Example 2: ./run_load_generator.sh /tmp/fx-queues/queue-a 500000 500000 --direct
#   -> Direct mode: bypasses serv-0, writes to queue-a.
# Example 3: ./run_load_generator.sh /tmp/fx-queues/queue-a 5000000
#   -> Runs infinitely in TCP mode.
# ==============================================================================

set -e

if [ "$#" -lt 2 ]; then
    echo "Usage: $0 <queue-path> <target-rate> [message-count] [--tcp|--direct]"
    echo "Example 1: $0 /tmp/fx-queues/queue-a 500000 500000"
    echo "Example 2: $0 /tmp/fx-queues/queue-a 500000 500000 --direct"
    echo "Example 3: $0 /tmp/fx-queues/queue-a 5000000"
    exit 1
fi

QUEUE_PATH=$1
TARGET_RATE=$2
MESSAGE_COUNT=${3:-"-1"}

# Parse optional mode flag (4th argument). Default to tcp.
LOAD_MODE_ARG="${4:---tcp}"
if [ "$LOAD_MODE_ARG" = "--tcp" ]; then
    LOAD_MODE="tcp"
elif [ "$LOAD_MODE_ARG" = "--direct" ]; then
    LOAD_MODE="direct"
else
    echo "Error: Unknown mode '$LOAD_MODE_ARG'. Use --tcp or --direct."
    exit 1
fi

# Ensure we are in the project root directory
cd "$(dirname "$0")/.."

# Check if JAR exists, if not warn the user to run build.sh
if [ ! -f "test/target/test-1.0.0-SNAPSHOT.jar" ]; then
    echo "Error: test-1.0.0-SNAPSHOT.jar not found."
    echo "Please run ./scripts/build.sh first to compile and package the dependencies."
    exit 1
fi

# We use the same JVM options used for optimal latency and Chronicle Queue compatibility
JVM_OPTS="$JVM_OPTS --add-exports=java.base/jdk.internal.ref=ALL-UNNAMED \
--add-exports=java.base/sun.nio.ch=ALL-UNNAMED \
--add-exports=jdk.unsupported/sun.misc=ALL-UNNAMED \
--add-exports=java.base/jdk.internal.misc=ALL-UNNAMED \
--add-opens=java.base/java.lang=ALL-UNNAMED \
--add-opens=java.base/java.lang.reflect=ALL-UNNAMED \
--add-opens=java.base/java.io=ALL-UNNAMED \
--add-opens=java.base/java.util=ALL-UNNAMED \
--add-opens=java.base/jdk.internal.misc=ALL-UNNAMED \
-XX:+UseZGC -XX:+AlwaysPreTouch -XX:CompileThreshold=10000 -Xmx2G -Xms2G"

# If macOS, inject the correct SelectorProvider
if [[ "$OSTYPE" == "darwin"* ]]; then
    JVM_OPTS="$JVM_OPTS -Djava.nio.channels.spi.SelectorProvider=sun.nio.ch.KQueueSelectorProvider"
fi

echo "=========================================="
echo "    Starting HFT Load Generator"
echo "    Queue: $QUEUE_PATH"
echo "    Rate:  $TARGET_RATE msgs/sec"
if [ "$MESSAGE_COUNT" != "-1" ]; then
    echo "    Count: $MESSAGE_COUNT messages"
else
    echo "    Count: Infinite"
fi
echo "    Mode:  $LOAD_MODE"
echo "=========================================="

apply_taskset() {
    local cpuset=$1
    if [ -n "$cpuset" ] && command -v taskset >/dev/null 2>&1; then
        echo "taskset -c $cpuset"
    else
        echo ""
    fi
}

TS_CMD=$(apply_taskset "${FX_BENCHMARK_CPUSET:-}")
$TS_CMD java $JVM_OPTS -Dfx.load.mode="$LOAD_MODE" -cp "test/target/test-1.0.0-SNAPSHOT.jar:test/target/dependency/*" com.fx.test.LoadGenerator "$QUEUE_PATH" "$TARGET_RATE" "$MESSAGE_COUNT"
