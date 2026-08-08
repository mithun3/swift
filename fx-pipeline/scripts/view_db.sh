#!/usr/bin/env bash

# ==============================================================================
# Script: view_db.sh
# Description: Helper script to query the in-memory H2 database directly from 
#              the terminal, bypassing the need for an external DB GUI client.
# Usage: ./view_db.sh
# ==============================================================================

set -euo pipefail
# Ensure we are in the project root directory
cd "$(dirname "$0")/.."

# Find the H2 jar in the project dependencies
H2_JAR=$(find . -name "h2-*.jar" | head -n 1 || true)

if [[ -z "$H2_JAR" ]]; then
    echo "H2 jar not found. Please build the project by running ./scripts/build.sh first."
    exit 1
fi

echo "=========================================="
echo "    Querying H2 Database (fx_trades)"
echo "=========================================="

# Use the H2 Shell tool to execute a query against the running TCP server
java -cp "$H2_JAR" org.h2.tools.Shell \
    -url "jdbc:h2:tcp://localhost:9092/mem:fxdb" \
    -user "sa" \
    -password "" \
    -sql "SELECT * FROM fx_trades;"
