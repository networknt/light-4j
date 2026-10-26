import json
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest

sys.path.insert(0, str(Path(__file__).parents[1]))
from preview_jackson3_test_baselines import preview
from jackson3_test_inventory import (InventoryMismatch, TestFloorError, UnsafeReportError,
                                    collect_reports, load_inventory)


class BaselinePreviewTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.inventory = {"known": {"baseline_executed": 100, "minimum_executed": 95}}

    def report(self, module, tests, skipped=0):
        path = self.root / module / "target/surefire-reports/TEST-example.xml"
        path.parent.mkdir(parents=True)
        path.write_text(f'<testsuite tests="{tests}" skipped="{skipped}" failures="0" errors="0"/>')
        return path

    def test_below_floor_still_reports_observed_counts_without_lowering_policy(self):
        self.report("known", 10, 2)
        result = preview(self.root, self.inventory)
        self.assertEqual(8, result["modules"]["known"]["executed"])
        self.assertEqual(["below_current_floor"], result["modules"]["known"]["review_issues"])
        self.assertEqual(100, self.inventory["known"]["baseline_executed"])
        with self.assertRaises(TestFloorError):
            collect_reports(self.root, self.inventory)

    def test_added_and_missing_modules_are_reported_alongside_counts(self):
        self.report("added", 12, 2)
        result = preview(self.root, self.inventory)
        self.assertEqual(["known"], result["missing_modules"])
        self.assertEqual(10, result["modules"]["added"]["executed"])
        self.assertIsNone(result["modules"]["added"]["current_floor"])
        self.assertEqual(["new_module_requires_inventory_review"],
                         result["modules"]["added"]["review_issues"])
        self.assertTrue(result["needs_review"])
        with self.assertRaises(InventoryMismatch):
            collect_reports(self.root, self.inventory)

    def test_growth_and_totals_are_visible(self):
        self.report("known", 205, 5)
        result = preview(self.root, self.inventory)
        self.assertEqual(200, result["modules"]["known"]["executed"])
        self.assertEqual(["growth_requires_baseline_review"],
                         result["modules"]["known"]["review_issues"])
        self.assertEqual(205, result["totals"]["tests"])

    def test_empty_reports_are_diagnostic_not_qualification(self):
        result = preview(self.root, self.inventory)
        self.assertEqual({}, result["modules"])
        self.assertEqual(["known"], result["missing_modules"])
        self.assertTrue(result["needs_review"])

    def test_preview_preserves_xml_rejection(self):
        path = self.report("known", 100)
        path.write_text('<!DOCTYPE testsuite [<!ENTITY x "100">]><testsuite tests="&x;"/>')
        with self.assertRaises(UnsafeReportError):
            preview(self.root, self.inventory)

    def test_documented_cli_prints_json_even_when_coverage_and_floor_differ(self):
        module = next(name for name, policy in load_inventory().items() if policy["minimum_executed"] > 1)
        self.report(module, 1)
        self.report("new-module", 3)
        output = subprocess.check_output([
            sys.executable, str(Path(__file__).parents[1] / "preview_jackson3_test_baselines.py"),
            "--reports-root", str(self.root)], text=True)
        result = json.loads(output)
        self.assertEqual(1, result["modules"][module]["executed"])
        self.assertIn("below_current_floor", result["modules"][module]["review_issues"])
        self.assertEqual(3, result["modules"]["new-module"]["executed"])
        self.assertTrue(result["missing_modules"])
        self.assertIn("not qualification", result["purpose"])
