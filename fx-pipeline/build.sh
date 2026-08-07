#!/usr/bin/env bash

# Exit immediately if a command exits with a non-zero status
set -e

# Dynamically change directory to where this script is located
cd "$(dirname "$0")"

echo "=========================================="
echo "    Building FX Pipeline Artifacts"
echo "=========================================="
mvn clean package dependency:copy-dependencies -DskipTests
