import importlib.util
import json
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch

spec = importlib.util.spec_from_file_location("inventory", Path(__file__).parents[1] / "jackson3_test_inventory.py")
inventory = importlib.util.module_from_spec(spec)
spec.loader.exec_module(inventory)


class ReportCoverageTest(unittest.TestCase):
    def report(self, root, module, tests=3, skipped=1, failures=0, errors=0):
        path = root / module / "target/surefire-reports/TEST-example.xml"
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(f'<testsuite tests="{tests}" skipped="{skipped}" failures="{failures}" errors="{errors}"/>')

    def test_recursive_reports_and_executed_count(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            self.report(root, "parent/child")
            modules, total = inventory.collect_reports(root, {"parent/child": {"minimum_executed": 1}})
            self.assertEqual(3, total["tests"])
            self.assertEqual(1, total["skipped"])
            self.assertIn("parent/child", modules)

    def test_missing_unexpected_empty_all_skipped_and_failed_reports(self):
        for kind in ("missing", "unexpected", "empty", "skipped", "failed", "errors"):
            with self.subTest(kind=kind), tempfile.TemporaryDirectory() as directory:
                root = Path(directory)
                policy = {"required": {"minimum_executed": 1}}
                if kind != "missing":
                    self.report(root, "other" if kind == "unexpected" else "required",
                                tests=0 if kind == "empty" else 3,
                                skipped=3 if kind == "skipped" else 0,
                                failures=int(kind == "failed"), errors=int(kind == "errors"))
                with self.assertRaises(ValueError):
                    inventory.collect_reports(root, policy)

    def test_skip_only_exception_is_explicit_and_does_not_qualify_whole_reactor(self):
        modules = {"disabled": dict(tests=3, skipped=3, failures=0, errors=0)}
        with self.assertRaisesRegex(ValueError, "reviewed reason"):
            inventory.validate_modules(modules, {"disabled": {"minimum_executed": 0}})
        with self.assertRaisesRegex(ValueError, "No executed"):
            inventory.validate_modules(modules, {"disabled": {"minimum_executed": 0, "disabled_reason": "existing skip"}})

    def test_dtd_entity_expansion_and_external_entities_are_rejected_in_both_encodings(self):
        documents = [
            '<!DOCTYPE testsuite><testsuite tests="1"/>',
            '<!DOCTYPE testsuite [<!ENTITY a "expanded"><!ENTITY b "&a;&a;">]><testsuite tests="1">&b;</testsuite>',
            '<!DOCTYPE testsuite [<!ENTITY ext SYSTEM "file:///nonexistent">]><testsuite tests="1">&ext;</testsuite>',
            '<!DOCTYPE testsuite SYSTEM "https://invalid.example/report.dtd"><testsuite tests="1"/>',
            '<!DOCTYPE testsuite [<!ENTITY % p SYSTEM "https://invalid.example/entity">%p;]><testsuite tests="1"/>',
        ]
        for document in documents:
            for encoding in ("utf-8", "utf-16"):
                with self.subTest(document=document, encoding=encoding), tempfile.TemporaryDirectory() as directory:
                    path = Path(directory) / "report.xml"
                    path.write_bytes(document.encode(encoding))
                    with self.assertRaisesRegex(inventory.UnsafeReportError, "DTD and entity"):
                        inventory.read_report(path, inventory.MAX_TOTAL_BYTES)

    def test_normal_utf8_and_utf16_reports_work_and_limits_are_enforced(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "report.xml"
            text = '<testsuites><testsuite tests="3" skipped="1" failures="0" errors="0"/></testsuites>'
            for encoding in ("utf-8", "utf-16"):
                path.write_bytes(text.encode(encoding))
                counts, size = inventory.read_report(path, inventory.MAX_TOTAL_BYTES)
                self.assertEqual(3, counts[0]["tests"])
                self.assertEqual(path.stat().st_size, size)
                with self.assertRaisesRegex(inventory.UnsafeReportError, "byte limit"):
                    inventory.read_report(path, 1)
            path.write_text('<testsuite tests="1">' + '<a>' * 65 + '</a>' * 65 + '</testsuite>')
            with self.assertRaisesRegex(inventory.UnsafeReportError, "depth limit"):
                inventory.read_report(path, inventory.MAX_TOTAL_BYTES)
            path.write_text('<testsuite')
            with self.assertRaisesRegex(inventory.UnsafeReportError, "Malformed"):
                inventory.read_report(path, inventory.MAX_TOTAL_BYTES)

    def test_total_report_bytes_and_file_count_are_bounded(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            self.report(root, "required")
            for limit in ("MAX_TOTAL_BYTES", "MAX_REPORTS"):
                with patch.object(inventory, limit, 0), self.assertRaises(inventory.UnsafeReportError):
                    inventory.collect_reports(root, {"required": {"minimum_executed": 1}})

    def test_inventory_matches_the_reviewed_report_snapshot(self):
        policy = inventory.load_inventory()
        snapshot = json.loads((Path(__file__).parents[1] / "jackson3-test-baseline-evidence.json").read_text())
        modules = {module: {key: counts[key] for key in inventory.COUNTERS}
                   for module, counts in snapshot["modules"].items()}
        observed = {module: counts["tests"] - counts["skipped"]
                    for module, counts in modules.items()}
        self.assertEqual(observed, {module: entry["baseline_executed"] for module, entry in policy.items()})
        totals = inventory.validate_modules(modules, policy)
        self.assertEqual(snapshot["totals"], totals)

    def test_reviewed_tolerance_boundaries(self):
        for baseline, expected in ((1, 1), (2, 1), (3, 2), (20, 19), (100, 95), (101, 95)):
            with self.subTest(baseline=baseline):
                self.assertEqual(expected, inventory.minimum_executed(baseline))
        for value in (-1, True, 1.5):
            with self.assertRaises(ValueError):
                inventory.minimum_executed(value)

    def test_small_corpus_change_passes_but_beyond_tolerance_and_zero_fail(self):
        rule = {"large": {"minimum_executed": inventory.minimum_executed(100)}}
        inventory.validate_modules({"large": dict(tests=95, skipped=0, failures=0, errors=0)}, rule)
        for executed in (94, 1, 0):
            with self.subTest(executed=executed), self.assertRaises(inventory.TestFloorError):
                inventory.validate_modules({"large": dict(tests=executed, skipped=0, failures=0, errors=0)}, rule)
