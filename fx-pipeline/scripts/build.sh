#!/usr/bin/env bash

# ==============================================================================
# Script: build.sh
# Description: Compiles and packages the FX Pipeline Maven artifacts.
#              Copies dependencies into the target directories for easier
#              execution via shell scripts.
# Usage: ./build.sh [--skip-tests|-s]
# ==============================================================================

# Exit immediately if a command exits with a non-zero status
set -e

# Ensure we are in the project root directory
cd "$(dirname "$0")/.."

SKIP_TESTS=false
for arg in "$@"; do
    case "$arg" in
        --skip-tests|-s)
            SKIP_TESTS=true
            ;;
        *)
            echo "Unknown option: $arg"
            echo "Usage: $0 [--skip-tests|-s]"
            exit 1
            ;;
    esac
done

echo "=========================================="
echo "    Building FX Pipeline Artifacts"
echo "=========================================="

if [ "$SKIP_TESTS" = true ]; then
    echo "(skipping tests)"
    mvn clean package dependency:copy-dependencies -DskipTests
else
    mvn clean package dependency:copy-dependencies
fi
