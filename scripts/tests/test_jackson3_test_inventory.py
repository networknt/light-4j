import importlib.util
from pathlib import Path
import tempfile
import unittest

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
