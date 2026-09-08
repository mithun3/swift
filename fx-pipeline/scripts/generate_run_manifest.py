#!/usr/bin/env python3
"""Generate a JSON manifest describing one benchmark run."""

import argparse
import json
import os
import platform
import subprocess
from datetime import datetime, timezone


def parse_total_count(hgrm_path: str) -> int:
    """Returns the HdrHistogram total count from a percentile file."""
    try:
        with open(hgrm_path, "r", encoding="utf-8") as hgrm_file:
            for line in hgrm_file:
                if "Total count" in line:
                    try:
                        return int(line.split()[-1].rstrip("]"))
                    except (IndexError, ValueError):
                        return 0
    except OSError:
        return 0
    return 0


def command_output(command: list[str], fallback: str = "unknown") -> str:
    """Runs a local metadata command without making manifest generation fragile."""
    try:
        result = subprocess.run(
            command,
            check=True,
            capture_output=True,
            text=True,
        )
    except (OSError, subprocess.CalledProcessError):
        return fallback
    output = (result.stdout or result.stderr).strip()
    return output.splitlines()[0] if output else fallback


def git_metadata() -> tuple[str, bool | None]:
    """Returns the current Git commit and dirty state when available."""
    sha = command_output(["git", "rev-parse", "HEAD"])
    if sha == "unknown":
        return sha, None
    try:
        result = subprocess.run(
            ["git", "status", "--porcelain"],
            check=True,
            capture_output=True,
            text=True,
        )
    except (OSError, subprocess.CalledProcessError):
        return sha, None
    return sha, bool(result.stdout.strip())


def stage_name(hlog_path: str) -> str:
    """Returns the stable histogram stage name for a telemetry path."""
    name = os.path.basename(hlog_path)
    prefix = "fx-latency-"
    if name == "fx-latency.hlog":
        return "end-to-end"
    if name.startswith(prefix) and name.endswith(".hlog"):
        return name[len(prefix):-len(".hlog")]
    return name


def build_manifest(arguments: argparse.Namespace) -> dict:
    """Builds manifest data from invocation parameters and generated artifacts."""
    git_sha, git_dirty = git_metadata()
    hlog_paths = [os.path.abspath(path) for path in arguments.hlogs]
    hlog_mtimes_utc = {}
    sample_counts = {}
    for path in hlog_paths:
        stage = stage_name(path)
        try:
            modified = datetime.fromtimestamp(os.path.getmtime(path), timezone.utc)
            hlog_mtimes_utc[stage] = modified.isoformat()
        except OSError:
            hlog_mtimes_utc[stage] = "missing"
        sample_counts[stage] = parse_total_count(path + ".hgrm")

    expected_duration_seconds = (
        arguments.message_count / arguments.target_rate
        if arguments.target_rate > 0 and arguments.message_count >= 0
        else None
    )
    return {
        "schema_version": 1,
        "run_id": arguments.run_id,
        "timestamp_utc": datetime.now(timezone.utc).isoformat(),
        "environment_label": arguments.environment,
        "git_sha": git_sha,
        "git_dirty": git_dirty,
        "runtime_os": arguments.runtime_os or platform.platform(),
        "runtime_arch": arguments.runtime_arch or platform.machine(),
        "jdk_version": arguments.jdk_version or command_output(["java", "-version"]),
        "transport_mode": arguments.transport_mode,
        "target_rate_msgs_sec": arguments.target_rate,
        "message_count": arguments.message_count,
        "expected_duration_seconds": expected_duration_seconds,
        "actual_load_duration_seconds": arguments.actual_load_duration,
        "trace_enabled": arguments.trace_enabled == "true",
        "queue_path": arguments.queue_path,
        "cpu_count": arguments.cpu_count,
        "cpu_profile": arguments.cpu_profile,
        "cpu_model": arguments.cpu_model,
        "cpusets": json.loads(arguments.cpusets),
        "effective_jvm_options": arguments.jvm_options,
        "hlog_paths": hlog_paths,
        "hlog_mtimes_utc": hlog_mtimes_utc,
        "sample_counts": sample_counts,
    }


def parse_arguments() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--output", required=True)
    parser.add_argument("--run-id", required=True)
    parser.add_argument("--environment", required=True)
    parser.add_argument("--target-rate", required=True, type=int)
    parser.add_argument("--message-count", required=True, type=int)
    parser.add_argument("--actual-load-duration", type=float)
    parser.add_argument("--transport-mode", choices=("tcp", "direct"), required=True)
    parser.add_argument("--trace-enabled", choices=("true", "false"), required=True)
    parser.add_argument("--queue-path", required=True)
    parser.add_argument("--cpu-count", type=int)
    parser.add_argument("--cpu-profile", default="")
    parser.add_argument("--cpu-model", default="Unknown")
    parser.add_argument("--cpusets", default="{}")
    parser.add_argument("--jvm-options", default="")
    parser.add_argument("--jdk-version", default="")
    parser.add_argument("--runtime-os", default="")
    parser.add_argument("--runtime-arch", default="")
    parser.add_argument("hlogs", nargs="+")
    return parser.parse_args()


def main() -> None:
    arguments = parse_arguments()
    try:
        manifest = build_manifest(arguments)
    except json.JSONDecodeError as error:
        raise SystemExit(f"Invalid --cpusets JSON: {error}") from error

    output_path = os.path.abspath(arguments.output)
    os.makedirs(os.path.dirname(output_path), exist_ok=True)
    with open(output_path, "w", encoding="utf-8") as output_file:
        json.dump(manifest, output_file, indent=2, sort_keys=True)
        output_file.write("\n")
    print(f"Run manifest generated at: {output_path}")


if __name__ == "__main__":
    main()