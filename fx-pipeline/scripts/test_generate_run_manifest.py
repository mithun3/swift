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

    @patch.object(MANIFEST, "git_metadata", return_value=("abc123", False))
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
                cpusets='{"serv-0": "0,5"}',
                jvm_options="-XX:+UseZGC",
                jdk_version="Eclipse Temurin 21",
                runtime_os="linux",
                runtime_arch="aarch64",
                hlogs=[str(hlog)],
            )

            result = MANIFEST.build_manifest(arguments)

        self.assertEqual(100.0, result["expected_duration_seconds"])
        self.assertEqual(101.0, result["actual_load_duration_seconds"])
        self.assertEqual(100_000, result["sample_counts"]["serv-0"])
        self.assertEqual({"serv-0": "0,5"}, result["cpusets"])
        self.assertEqual("Eclipse Temurin 21", result["jdk_version"])
        self.assertTrue(result["trace_enabled"])


if __name__ == "__main__":
    unittest.main()