#!/usr/bin/env bash

# ==============================================================================
# Library: scripts/lib/common.sh
# Description: Shared utility functions sourced by FX Pipeline scripts.
# Usage: source "$(dirname "$0")/lib/common.sh"   (from scripts/ root)
#        source "$(dirname "$0")/../lib/common.sh" (from scripts/runners/)
# ==============================================================================

# Returns a "taskset -c <cpuset>" prefix command if taskset is available and
# the cpuset string is non-empty; otherwise returns an empty string.
# Usage:
#   TS_CMD=$(apply_taskset "${FX_SERV_0_CPUSET:-}")
#   $TS_CMD java $JVM_OPTS ...
apply_taskset() {
    local cpuset=$1
    if [ -n "$cpuset" ] && command -v taskset > /dev/null 2>&1; then
        echo "taskset -c $cpuset"
    else
        echo ""
    fi
}
