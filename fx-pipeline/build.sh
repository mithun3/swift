#!/usr/bin/env bash

# ==============================================================================
# Script: build.sh
# Description: Compiles and packages the FX Pipeline Maven artifacts.
#              Copies dependencies into the target directories for easier
#              execution via shell scripts.
# Usage: ./build.sh
# ==============================================================================

# Exit immediately if a command exits with a non-zero status
set -e

# Dynamically change directory to where this script is located
cd "$(dirname "$0")"

echo "=========================================="
echo "    Building FX Pipeline Artifacts"
echo "=========================================="
mvn clean package dependency:copy-dependencies -DskipTests
