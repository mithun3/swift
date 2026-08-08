#!/usr/bin/env bash

# ==============================================================================
# Script: run_client.sh
# Description: Legacy script to run the standalone FX client. 
#              Consider using send_test_message.sh instead for more robustness.
# Usage: ./run_client.sh
# ==============================================================================

# Ensure we are in the project root directory
cd "$(dirname "$0")/.."

echo "Starting FX Standalone Client..."
java -cp "client/target/client-1.0.0-SNAPSHOT.jar:common/target/common-1.0.0-SNAPSHOT.jar:client/target/dependency/*" com.fx.client.FixClientMain
