#!/usr/bin/env bash

# ==============================================================================
# Script: deploy.sh
# Description: Starts all FX Pipeline services (Gateway, Risk, Pricing, Persistence)
#              in the background. Configures optimized JVM arguments (ZGC, 
#              OS-specific SelectorProviders) for high performance.
# Usage: ./deploy.sh
# Note: Pressing Ctrl+C will trap the signal and cleanly stop all services.
# ==============================================================================

# Exit immediately if a command exits with a non-zero status
set -e

# Dynamically change directory to where this script is located
cd "$(dirname "$0")"

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
--add-opens=java.base/java.lang=ALL-UNNAMED \
--add-opens=java.base/java.lang.reflect=ALL-UNNAMED \
--add-opens=java.base/java.io=ALL-UNNAMED \
--add-opens=java.base/java.util=ALL-UNNAMED \
-XX:+UseZGC -XX:+ZGenerational -Xmx512m -Xms512m \
-XX:+AlwaysPreTouch -XX:+DisableExplicitGC \
$SELECTOR_OPT"

# Cleanup function to kill all background jobs when this script exits
cleanup() {
    echo ""
    echo "=========================================="
    echo "    Stopping all FX Pipeline Services"
    echo "=========================================="
    kill $(jobs -p) 2>/dev/null
    wait $(jobs -p) 2>/dev/null || true
    echo "All services stopped cleanly."
    exit
}

# Trap SIGINT (Ctrl+C) and SIGTERM to run the cleanup function
trap cleanup SIGINT SIGTERM

mkdir -p logs

echo "Starting serv-c (Persistence Egress)..."
java $JVM_OPTS -cp "serv-c/target/serv-c-1.0.0-SNAPSHOT.jar:common/target/common-1.0.0-SNAPSHOT.jar:serv-c/target/dependency/*" com.fx.persistence.PersistenceMain > logs/serv-c.log 2>&1 &
sleep 1

echo "Starting Telemetry Stitcher (Distributed Tracing)..."
java $JVM_OPTS -cp "common/target/common-1.0.0-SNAPSHOT.jar:common/target/dependency/*" com.fx.common.telemetry.TelemetryMain > logs/telemetry.log 2>&1 &
sleep 1

echo "Starting serv-b (Pricing Matching)..."
java $JVM_OPTS -cp "serv-b/target/serv-b-1.0.0-SNAPSHOT.jar:common/target/common-1.0.0-SNAPSHOT.jar:serv-b/target/dependency/*" com.fx.pricing.PricingMain > logs/serv-b.log 2>&1 &
sleep 1

echo "Starting serv-a (Risk Validation)..."
java $JVM_OPTS -cp "serv-a/target/serv-a-1.0.0-SNAPSHOT.jar:common/target/common-1.0.0-SNAPSHOT.jar:serv-a/target/dependency/*" com.fx.risk.RiskMain > logs/serv-a.log 2>&1 &
sleep 1

echo "Starting serv-0 (Client Gateway) in TCP mode..."
java $JVM_OPTS -Dfx.gateway.port=5001 -Dfx.gateway.mode=tcp -cp "serv-0/target/serv-0-1.0.0-SNAPSHOT.jar:common/target/common-1.0.0-SNAPSHOT.jar:serv-0/target/dependency/*" com.fx.gateway.GatewayMain > logs/serv-0.log 2>&1 &

echo "=========================================="
echo " Pipeline is RUNNING. Tailing logs..."
echo " Press Ctrl+C to STOP"
echo "=========================================="

tail -f logs/serv-c.log logs/telemetry.log logs/serv-b.log logs/serv-a.log logs/serv-0.log &

# Wait indefinitely for background processes
wait
