import importlib.util
import tempfile
import unittest
from argparse import Namespace
from pathlib import Path
from unittest.mock import patch


MODULE_PATH = Path(__file__).with_name("generate_run_manifest.py")
SPEC = importlib.util.spec_from_file_location("generate_run_manifest", MODULE_PATH)
MANIFEST = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(MANIFEST)


class GenerateRunManifestTest(unittest.TestCase):
    def test_parse_total_count(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "latency.hgrm"
            path.write_text("#[Total count = 12345]\n", encoding="utf-8")

            self.assertEqual(12345, MANIFEST.parse_total_count(str(path)))

    def test_stage_name_recognizes_pipeline_files(self):
        self.assertEqual("queue-a", MANIFEST.stage_name("/tmp/fx-latency-queue-a.hlog"))
        self.assertEqual("end-to-end", MANIFEST.stage_name("/tmp/fx-latency.hlog"))

    @patch.object(MANIFEST, "git_metadata", return_value=("abc123", False, "", []))
    @patch.object(MANIFEST, "command_output", return_value="openjdk version 21")
    def test_build_manifest_uses_artifact_counts(self, _command_output, _git_metadata):
        with tempfile.TemporaryDirectory() as directory:
            hlog = Path(directory) / "fx-latency-serv-0.hlog"
            hlog.write_text("histogram", encoding="utf-8")
            Path(str(hlog) + ".hgrm").write_text(
                "#[Total count = 100000]\n", encoding="utf-8"
            )
            arguments = Namespace(
                run_id="docker-test",
                environment="docker-desktop",
                target_rate=10_000,
                message_count=1_000_000,
                actual_load_duration=101.0,
                transport_mode="tcp",
                trace_enabled="true",
                queue_path="/tmp/fx-queues/queue-a",
                cpu_count=10,
                cpu_profile="desktop",
                cpu_model="Apple M2 Max",
                cpusets='{"serv-0": "0,5"}',
                jvm_options="-XX:+UseZGC",
                jdk_version="Eclipse Temurin 21",
                runtime_os="linux",
                runtime_arch="aarch64",
                wait_strategy="busyspin",
                docker_image_digest="sha256:deadbeef",
                artifact_hashes='{"serv-0": "abc"}',
                gc_log_paths='{"serv-0": "/tmp/serv-0-gc.log"}',
                exit_status='{"serv-0": "graceful"}',
                hlogs=[str(hlog)],
            )

            result = MANIFEST.build_manifest(arguments)

        self.assertEqual(2, result["schema_version"])
        self.assertEqual(100.0, result["expected_duration_seconds"])
        self.assertEqual(101.0, result["actual_load_duration_seconds"])
        self.assertEqual(100_000, result["sample_counts"]["serv-0"])
        self.assertEqual({"serv-0": "0,5"}, result["cpusets"])
        self.assertEqual("Eclipse Temurin 21", result["jdk_version"])
        self.assertTrue(result["trace_enabled"])
        self.assertEqual("busyspin", result["wait_strategy"])
        self.assertEqual("sha256:deadbeef", result["docker_image_digest"])
        self.assertEqual({"serv-0": "abc"}, result["artifact_hashes"])
        self.assertEqual({"serv-0": "/tmp/serv-0-gc.log"}, result["gc_log_paths"])
        self.assertEqual({"serv-0": "graceful"}, result["service_exit_status"])
        self.assertEqual("", result["dirty_patch_sha256"])
        self.assertEqual([], result["untracked_files"])

    @patch.object(MANIFEST, "git_metadata", return_value=("abc123", False, "", []))
    @patch.object(MANIFEST, "command_output", return_value="openjdk version 21")
    def test_build_manifest_defaults_new_fields_when_omitted(self, _command_output, _git_metadata):
        """Callers that haven't been updated to pass the new v2 flags must not crash."""
        with tempfile.TemporaryDirectory() as directory:
            hlog = Path(directory) / "fx-latency-serv-0.hlog"
            hlog.write_text("histogram", encoding="utf-8")
            Path(str(hlog) + ".hgrm").write_text(
                "#[Total count = 100000]\n", encoding="utf-8"
            )
            arguments = Namespace(
                run_id="native-test",
                environment="local",
                target_rate=10_000,
                message_count=1_000_000,
                actual_load_duration=101.0,
                transport_mode="tcp",
                trace_enabled="false",
                queue_path="/tmp/fx-queues/queue-a",
                cpu_count=10,
                cpu_profile="host",
                cpu_model="AMD EPYC",
                cpusets="{}",
                jvm_options="-XX:+UseZGC",
                jdk_version="Eclipse Temurin 21",
                runtime_os="linux",
                runtime_arch="aarch64",
                hlogs=[str(hlog)],
            )

            result = MANIFEST.build_manifest(arguments)

        self.assertEqual("unknown", result["wait_strategy"])
        self.assertEqual("", result["docker_image_digest"])
        self.assertEqual({}, result["artifact_hashes"])
        self.assertEqual({}, result["gc_log_paths"])
        self.assertEqual({}, result["service_exit_status"])

    def test_git_metadata_reports_dirty_patch_hash_and_untracked_files(self):
        with tempfile.TemporaryDirectory() as directory:
            subprocess_run_calls = []

            def fake_run(command, **_kwargs):
                subprocess_run_calls.append(command)
                if command[:2] == ["git", "status"]:
                    return type(
                        "Result", (), {"stdout": "M  tracked.txt\n?? scratch/new.java\n"}
                    )()
                if command[:2] == ["git", "diff"]:
                    return type("Result", (), {"stdout": "diff --git a/x b/x\n"})()
                raise AssertionError(f"Unexpected command: {command}")

            with patch.object(MANIFEST, "command_output", return_value="deadbeef"), \
                 patch.object(MANIFEST.subprocess, "run", side_effect=fake_run):
                sha, dirty, patch_sha256, untracked = MANIFEST.git_metadata()

        self.assertEqual("deadbeef", sha)
        self.assertTrue(dirty)
        self.assertEqual(["scratch/new.java"], untracked)
        self.assertEqual(64, len(patch_sha256))
        del directory


if __name__ == "__main__":
    unittest.main()