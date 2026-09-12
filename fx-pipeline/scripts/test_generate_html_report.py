import importlib.util
import json
import sys
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch


MODULE_PATH = Path(__file__).with_name("generate_html_report.py")
SPEC = importlib.util.spec_from_file_location("generate_html_report", MODULE_PATH)
REPORT = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(REPORT)


class GenerateHtmlReportTest(unittest.TestCase):
    def test_get_percentiles_matches_individual_lookups_in_one_pass(self):
        with tempfile.TemporaryDirectory() as directory:
            hgrm = Path(directory) / "fx-latency.hlog.hgrm"
            hgrm.write_text(
                "1000.0 0.500000000000 5\n"
                "2000.0 0.900000000000 9\n"
                "5000.0 0.990000000000 10\n"
                "#[Total count = 10]\n",
                encoding="utf-8",
            )

            batched, total = REPORT.get_percentiles(str(hgrm), (0.50, 0.90, 0.99))

            self.assertEqual(10, total)
            self.assertEqual((1000.0, 10), REPORT.get_percentile(str(hgrm), 0.50))
            self.assertEqual((2000.0, 10), REPORT.get_percentile(str(hgrm), 0.90))
            self.assertEqual(
                {0.50: 1000.0, 0.90: 2000.0, 0.99: 5000.0}, batched
            )

    def test_parse_arguments_preserves_legacy_positional_inputs(self):
        manifest, hlogs = REPORT.parse_arguments(["a.hlog", "b.hlog"])

        self.assertIsNone(manifest)
        self.assertEqual(["a.hlog", "b.hlog"], hlogs)

    def test_parse_arguments_accepts_manifest(self):
        manifest, hlogs = REPORT.parse_arguments(
            ["--manifest", "run.json", "a.hlog"]
        )

        self.assertEqual("run.json", manifest)
        self.assertEqual(["a.hlog"], hlogs)

    def test_load_manifest_requires_json_object(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "run.json"
            path.write_text(json.dumps(["not", "an", "object"]), encoding="utf-8")

            with self.assertRaisesRegex(ValueError, "JSON object"):
                REPORT.load_manifest(str(path))

    def test_manifest_card_escapes_values(self):
        card = REPORT.build_manifest_card({
            "environment_label": "docker <desktop>",
            "target_rate_msgs_sec": 10_000,
            "trace_enabled": True,
            "cpusets": {"serv-0": "0,5"},
        })

        self.assertIn("Run Configuration", card)
        self.assertIn("docker &lt;desktop&gt;", card)
        self.assertIn("10,000", card)
        self.assertIn("true", card)
        self.assertIn("{&quot;serv-0&quot;: &quot;0,5&quot;}", card)

    def test_empty_manifest_has_no_card(self):
        self.assertEqual("", REPORT.build_manifest_card({}))

    def test_main_embeds_manifest_in_generated_report(self):
        with tempfile.TemporaryDirectory() as directory:
            hlog = Path(directory) / "fx-latency.hlog"
            Path(str(hlog) + ".hgrm").write_text(
                "1.0 0.500 2\n2.0 1.000 1\n#[Total count = 2]\n",
                encoding="utf-8",
            )
            manifest = Path(directory) / "run_manifest.json"
            manifest.write_text(
                json.dumps({"run_id": "docker-test", "target_rate_msgs_sec": 10_000}),
                encoding="utf-8",
            )

            with patch.object(
                sys,
                "argv",
                [str(MODULE_PATH), "--manifest", str(manifest), str(hlog)],
            ):
                REPORT.main()

            report = (Path(directory) / "latency_report.html").read_text(encoding="utf-8")

        self.assertIn("Run Configuration", report)
        self.assertIn("docker-test", report)
        self.assertIn("10,000", report)


if __name__ == "__main__":
    unittest.main()