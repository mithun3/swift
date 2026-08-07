#!/usr/bin/env bash

# Dynamically change directory to where this script is located
cd "$(dirname "$0")"

echo "Starting FX Standalone Client..."
java -cp "client/target/client-1.0.0-SNAPSHOT.jar:common/target/common-1.0.0-SNAPSHOT.jar:client/target/dependency/*" com.fx.client.FixClientMain
