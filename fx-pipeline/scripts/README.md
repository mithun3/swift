# FX Pipeline Scripts

This directory contains utility and automation scripts for the FX Pipeline.
To adhere to the Single Responsibility Principle, each script is designed to perform a distinct, well-defined unit of work.

## Configuration Profiles

All benchmark runs are driven by environment-specific profiles in `config/profiles/*.env`.
See [`CONFIG_PROFILES.md`](../CONFIG_PROFILES.md) for a full reference of every variable,
the docker two-path hlog design, CPU profile semantics, and runtime override examples.

## Build and Lifecycle
* `build.sh` - Compiles the Maven project and copies dependencies into the `target/dependency` directories.
* `start.sh` - Starts all native pipeline services (serv-c, serv-b, serv-a, serv-0, telemetry) in the correct consumer-first order.
* `stop.sh` - Cleanly terminates all native pipeline services and cleans up PID files.

## Infrastructure & Provisioning
* `provision_rocky.sh` - Automates the configuration of a Rocky Linux host for ultra-low latency tuning (disabling hyperthreading, C-states, and isolating CPU cores).

## Benchmarking & Orchestration
* `run_benchmark.sh` - The unified entrypoint for benchmarking. Orchestrates the end-to-end execution of a benchmark run based on a given configuration profile (`local`, `docker`, etc.).
* `runners/native_runner.sh` - Execution strategy for native environments (macOS/Linux). Implements starting, warming up, measuring, and stopping native services.
* `runners/docker_runner.sh` - Execution strategy for Docker environments. Implements starting, warming up, measuring, and stopping Docker compose services.

## Load Generation & Interaction
* `run_load_generator.sh` - Executes the `LoadGenerator` Java class. Injects a specified number of HFT messages at a fixed rate into a specific queue or via TCP.
* `run_client.sh` - Starts the interactive FIX client shell.
* `send_test_message.sh` - Sends a single, hardcoded FIX `NewOrderSingle` message over TCP to the gateway for quick validation.
* `view_db.sh` - Connects to the H2 database and prints the latest 20 trades.

## Telemetry & Reporting
* `process_latency.sh` - Invokes the `HdrHistogram` processor to extract percentile distribution (`.hgrm`) from binary `.hlog` files.
* `generate_run_manifest.py` - Parses `.hlog` headers and system metrics to generate a structured `run_manifest.json`.
* `plot_latency.py` - Generates a Matplotlib `.png` latency curve from a `.hgrm` file.
* `generate_html_report.py` - Synthesizes the generated `.hgrm` files and `run_manifest.json` into a comprehensive `latency_report.html` file.
* `generate_benchmark_report.sh` - Coordinates the execution of the telemetry & reporting scripts, abstracting away differences in execution environments. Invoked by `run_benchmark.sh` after a run.

## Testing & Diagnostics
* `test.sh` - Diagnostic script to print the default Java `SelectorProvider` on the host OS.
* `tests/test_generate_html_report.py` - Unit tests for the HTML report generation script.
* `tests/test_generate_run_manifest.py` - Unit tests for the run manifest generation script.
