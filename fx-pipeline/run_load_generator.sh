#!/usr/bin/env bash

# ==============================================================================
# Script: run_load_generator.sh
# Description: Executes the high-throughput garbage-free load generator
# Usage: ./run_load_generator.sh <queue-path> <target-rate> [message-count]
# Example: ./run_load_generator.sh /tmp/fx-queues/queue-a 5000000
# Example: ./run_load_generator.sh /tmp/fx-queues/queue-a 1 1  (Sends exactly 1 message at 1 msgs/sec)
# ==============================================================================

set -e

if [ "$#" -lt 2 ] || [ "$#" -gt 3 ]; then
    echo "Usage: $0 <queue-path> <target-rate> [message-count]"
    echo "Example: $0 /tmp/fx-queues/queue-a 5000000"
    echo "Example: $0 /tmp/fx-queues/queue-a 1 1"
    exit 1
fi

QUEUE_PATH=$1
TARGET_RATE=$2
MESSAGE_COUNT=${3:-"-1"}

# Ensure we are in the script's directory
cd "$(dirname "$0")"

# Check if JAR exists, if not warn the user to run build.sh
if [ ! -f "test/target/test-1.0.0-SNAPSHOT.jar" ]; then
    echo "Error: test-1.0.0-SNAPSHOT.jar not found."
    echo "Please run ./build.sh first to compile and package the dependencies."
    exit 1
fi

# We use the same JVM options used for optimal latency and Chronicle Queue compatibility
JVM_OPTS="--add-exports=java.base/jdk.internal.ref=ALL-UNNAMED \
--add-exports=java.base/sun.nio.ch=ALL-UNNAMED \
--add-exports=jdk.unsupported/sun.misc=ALL-UNNAMED \
--add-opens=java.base/java.lang=ALL-UNNAMED \
--add-opens=java.base/java.lang.reflect=ALL-UNNAMED \
--add-opens=java.base/java.io=ALL-UNNAMED \
--add-opens=java.base/java.util=ALL-UNNAMED \
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
echo "=========================================="

java $JVM_OPTS -cp "test/target/test-1.0.0-SNAPSHOT.jar:test/target/dependency/*" com.fx.test.LoadGenerator "$QUEUE_PATH" "$TARGET_RATE" "$MESSAGE_COUNT"
