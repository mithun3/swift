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

OS=$(uname)
if [ "$OS" = "Linux" ]; then
    SELECTOR_OPT="-Djava.nio.channels.spi.SelectorProvider=sun.nio.ch.EPollSelectorProvider"
else
    SELECTOR_OPT=""
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
$SELECTOR_OPT"

mkdir -p logs

# Ensure traces.jsonl exists
touch logs/traces.jsonl

echo "Starting serv-c (Persistence Egress)..."
java $JVM_OPTS -cp "serv-c/target/serv-c-1.0.0-SNAPSHOT.jar:common/target/common-1.0.0-SNAPSHOT.jar:serv-c/target/dependency/*" com.fx.persistence.PersistenceMain > logs/serv-c.log 2>&1 &
echo $! >> "$PID_FILE"
sleep 1

echo "Starting Telemetry Stitcher (Distributed Tracing)..."
java $JVM_OPTS -cp "common/target/common-1.0.0-SNAPSHOT.jar:common/target/dependency/*" com.fx.common.telemetry.TelemetryMain > logs/telemetry.log 2>&1 &
echo $! >> "$PID_FILE"
sleep 1

echo "Starting serv-b (Pricing Matching)..."
java $JVM_OPTS -cp "serv-b/target/serv-b-1.0.0-SNAPSHOT.jar:common/target/common-1.0.0-SNAPSHOT.jar:serv-b/target/dependency/*" com.fx.pricing.PricingMain > logs/serv-b.log 2>&1 &
echo $! >> "$PID_FILE"
sleep 1

echo "Starting serv-a (Risk Validation)..."
java $JVM_OPTS -cp "serv-a/target/serv-a-1.0.0-SNAPSHOT.jar:common/target/common-1.0.0-SNAPSHOT.jar:serv-a/target/dependency/*" com.fx.risk.RiskMain > logs/serv-a.log 2>&1 &
echo $! >> "$PID_FILE"
sleep 1

echo "Starting serv-0 (Client Gateway) in TCP mode..."
java $JVM_OPTS -Dfx.gateway.port=5001 -Dfx.gateway.mode=tcp -cp "serv-0/target/serv-0-1.0.0-SNAPSHOT.jar:common/target/common-1.0.0-SNAPSHOT.jar:serv-0/target/dependency/*" com.fx.gateway.GatewayMain > logs/serv-0.log 2>&1 &
echo $! >> "$PID_FILE"

echo "=========================================="
echo " Pipeline started in the background."
echo " Use 'tail -f logs/*.log' to view logs."
echo " Run './scripts/stop.sh' to gracefully stop."
echo "=========================================="
