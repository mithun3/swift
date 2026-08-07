#!/usr/bin/env bash

# ==============================================================================
# Script: send_test_message.sh
# Description: Sends a test FIX NewOrderSingle message to the FX Pipeline Gateway.
# 
# Usage: ./send_test_message.sh [HOST] [PORT]
#   HOST - Optional. The gateway host (default: 127.0.0.1)
#   PORT - Optional. The gateway TCP port (default: 5000)
#
# Design & Best Practices:
# - Bash Strict Mode (set -euo pipefail) ensures fail-fast behavior.
# - Explicit pre-flight checks to avoid cryptic Java ClassNotFound exceptions.
# - Standardized logging function for observability.
# ==============================================================================

set -euo pipefail

# --- Configuration & Defaults ---
# Change to the directory where this script is located
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "${SCRIPT_DIR}"

HOST="${1:-127.0.0.1}"
PORT="${2:-5000}"

CLIENT_JAR="client/target/client-1.0.0-SNAPSHOT.jar"
COMMON_JAR="common/target/common-1.0.0-SNAPSHOT.jar"
DEP_DIR="client/target/dependency"

# --- Functions ---

# Print informational messages
log_info() {
    echo -e "[INFO] $(date +'%Y-%m-%dT%H:%M:%S%z') - $*"
}

# Print error messages and exit
log_error() {
    echo -e "[ERROR] $(date +'%Y-%m-%dT%H:%M:%S%z') - $*" >&2
    exit 1
}

# --- Pre-flight Checks ---

if [[ ! -f "$CLIENT_JAR" ]]; then
    log_error "Client JAR not found at $CLIENT_JAR. Please build the project by running ./build.sh first."
fi

# --- Main Execution ---

log_info "Starting FX Standalone Client..."
log_info "Targeting Gateway at ${HOST}:${PORT}"

# Execute the Java client.
# We pass the HOST and PORT as system properties which are read by com.fx.client.FixClientMain.
# The classpath (-cp) includes the client jar, common jar, and any client dependencies.
java \
    -Dfx.client.host="${HOST}" \
    -Dfx.client.port="${PORT}" \
    -cp "${CLIENT_JAR}:${COMMON_JAR}:${DEP_DIR}/*" \
    com.fx.client.FixClientMain

log_info "Client execution completed successfully."
