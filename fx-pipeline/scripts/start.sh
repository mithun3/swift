#!/usr/bin/env bash

# ==============================================================================
# Script: start.sh
# Description: Starts all FX Pipeline services (Gateway, Risk, Pricing, Persistence)
#              in the background. Configures optimized JVM arguments (ZGC, 
#              OS-specific SelectorProviders) for high performance.
# Usage: ./scripts/start.sh
# ==============================================================================

set -e

# Ensure we are in the project root directory
cd "$(dirname "$0")/.."

PID_FILE="logs/services.pid"

if [ -f "$PID_FILE" ]; then
    echo "Services might already be running. Check $PID_FILE or run ./scripts/stop.sh first."
    exit 1
fi

echo "=========================================="
echo "    Starting FX Pipeline Services"
echo "=========================================="

echo "Cleaning up old queues and telemetry..."
rm -rf "${FX_QUEUE_DIR:-/tmp/fx-queues}"/*
rm -f "${FX_HLOG_DIR:-/tmp}"/fx-latency*.hlog


OS=$(uname)
if [ "$OS" = "Linux" ]; then
    SELECTOR_OPT="-Djava.nio.channels.spi.SelectorProvider=sun.nio.ch.EPollSelectorProvider"
    PREFIX_CMD=""
    # Warn if the queue directory is not on a RAM-backed filesystem.
    # Chronicle Queue's mmap segments must reside on tmpfs for sub-microsecond
    # page-fault costs. /dev/shm is always tmpfs on Linux; /tmp may be ext4.
    QUEUE_FS=$(stat -f -c '%T' "${FX_QUEUE_DIR:-/tmp/fx-queues}" 2>/dev/null || echo "unknown")
    if [ "$QUEUE_FS" != "tmpfs" ]; then
        echo "WARNING: Queue dir '${FX_QUEUE_DIR:-/tmp/fx-queues}' is on '$QUEUE_FS', not tmpfs."
        echo "         Set FX_QUEUE_DIR=/dev/shm/fx-queues in your profile for sub-microsecond latency."
    fi
else
    SELECTOR_OPT="-Djava.nio.channels.spi.SelectorProvider=sun.nio.ch.KQueueSelectorProvider"
    PREFIX_CMD="caffeinate -s"

    echo "macOS detected. Creating 4GB RAM disk for /tmp/fx-queues/..."
    mkdir -p logs
    if [ -f "logs/ramdisk.dev" ]; then
        OLD_DEV=$(cat logs/ramdisk.dev)
        umount /tmp/fx-queues 2>/dev/null || true
        hdiutil detach "$OLD_DEV" 2>/dev/null || true
        rm -f logs/ramdisk.dev
    fi
    mkdir -p /tmp/fx-queues
    RAMDISK_DEV=$(hdiutil attach -nomount ram://8388608 | tr -d ' \t')
    newfs_hfs -v 'FX_QUEUES' "$RAMDISK_DEV" > /dev/null
    mount -t hfs "$RAMDISK_DEV" /tmp/fx-queues
    echo "$RAMDISK_DEV" > logs/ramdisk.dev
fi

export JVM_OPTS="--add-exports=java.base/jdk.internal.ref=ALL-UNNAMED \
--add-exports=java.base/sun.nio.ch=ALL-UNNAMED \
--add-exports=jdk.unsupported/sun.misc=ALL-UNNAMED \
--add-exports=java.base/jdk.internal.misc=ALL-UNNAMED \
--add-opens=java.base/java.lang=ALL-UNNAMED \
--add-opens=java.base/java.lang.reflect=ALL-UNNAMED \
--add-opens=java.base/java.io=ALL-UNNAMED \
--add-opens=java.base/java.util=ALL-UNNAMED \
--add-opens=java.base/jdk.internal.misc=ALL-UNNAMED \
-XX:+UseZGC -XX:+ZGenerational -Xmx512m -Xms512m \
-XX:+AlwaysPreTouch -XX:+DisableExplicitGC \
-XX:ZUncommitDelay=600 -XX:-ZUncommit \
-XX:ZAllocationSpikeTolerance=3.0 -XX:CompileThreshold=500 -XX:+TieredCompilation \
-Dfx.waitstrategy=${FX_WAIT_STRATEGY:-phased} \
-Dfx.queue.base.dir=${FX_QUEUE_DIR:-/tmp/fx-queues} \
-Dfx.telemetry.log.path=${FX_HLOG_DIR:-/tmp}/fx-latency.hlog \
${FX_JVM_OPTS_OVERRIDE:-} \
$SELECTOR_OPT"
mkdir -p logs
mkdir -p "${FX_HLOG_DIR:-/tmp}"

# Ensure traces.jsonl exists
touch logs/traces.jsonl

source "$(dirname "$0")/lib/common.sh"

echo "Starting serv-c (Persistence Egress)..."
TS_CMD=$(apply_taskset "${FX_SERV_C_CPUSET:-}")
$PREFIX_CMD $TS_CMD java $JVM_OPTS -cp "serv-c/target/serv-c-1.0.0-SNAPSHOT.jar:common/target/common-1.0.0-SNAPSHOT.jar:serv-c/target/dependency/*" com.fx.persistence.PersistenceMain > logs/serv-c.log 2>&1 &
echo "$! serv-c" >> "$PID_FILE"
sleep 1

echo "Starting Telemetry Stitcher (Distributed Tracing)..."
TS_CMD=$(apply_taskset "${FX_TELEMETRY_CPUSET:-}")
$PREFIX_CMD $TS_CMD java $JVM_OPTS -cp "common/target/common-1.0.0-SNAPSHOT.jar:common/target/dependency/*" com.fx.common.telemetry.TelemetryMain > logs/telemetry.log 2>&1 &
echo "$! telemetry" >> "$PID_FILE"
sleep 1

echo "Starting serv-b (Pricing Matching)..."
TS_CMD=$(apply_taskset "${FX_SERV_B_CPUSET:-}")
$PREFIX_CMD $TS_CMD java $JVM_OPTS -cp "serv-b/target/serv-b-1.0.0-SNAPSHOT.jar:common/target/common-1.0.0-SNAPSHOT.jar:serv-b/target/dependency/*" com.fx.pricing.PricingMain > logs/serv-b.log 2>&1 &
echo "$! serv-b" >> "$PID_FILE"
sleep 1

echo "Starting serv-a (Risk Validation)..."
TS_CMD=$(apply_taskset "${FX_SERV_A_CPUSET:-}")
$PREFIX_CMD $TS_CMD java $JVM_OPTS -cp "serv-a/target/serv-a-1.0.0-SNAPSHOT.jar:common/target/common-1.0.0-SNAPSHOT.jar:serv-a/target/dependency/*" com.fx.risk.RiskMain > logs/serv-a.log 2>&1 &
echo "$! serv-a" >> "$PID_FILE"
sleep 1

echo "Starting serv-0 (Client Gateway) in TCP mode..."
TS_CMD=$(apply_taskset "${FX_SERV_0_CPUSET:-}")
$PREFIX_CMD $TS_CMD java $JVM_OPTS -Dfx.gateway.port=5001 -Dfx.gateway.mode=tcp -cp "serv-0/target/serv-0-1.0.0-SNAPSHOT.jar:common/target/common-1.0.0-SNAPSHOT.jar:serv-0/target/dependency/*" com.fx.gateway.GatewayMain > logs/serv-0.log 2>&1 &
echo "$! serv-0" >> "$PID_FILE"

echo "=========================================="
echo " Pipeline started in the background."
echo " Use 'tail -f logs/*.log' to view logs."
echo " Run './scripts/stop.sh' to gracefully stop."
echo "=========================================="
