import importlib.util
from pathlib import Path
import unittest

spec = importlib.util.spec_from_file_location("qualification", Path(__file__).parents[1] / "jackson3_core_qualification.py")
qualification = importlib.util.module_from_spec(spec)
spec.loader.exec_module(qualification)


class CoreQualificationTest(unittest.TestCase):
    def run_record(self, **changes):
        result = dict(head_sha="a" * 40, path=".github/workflows/jackson3-source-build.yml",
                      run_number=1, status="completed", conclusion="success", html_url="https://example/run/1")
        result.update(changes)
        return result

    def test_exact_success(self):
        self.assertEqual("a" * 40, qualification.qualify([self.run_record()], "a" * 40)["head_sha"])

    def test_missing_wrong_sha_or_workflow(self):
        for records in ([], [self.run_record(head_sha="b" * 40)], [self.run_record(path="other.yml")]):
            with self.subTest(records=records), self.assertRaises(ValueError):
                qualification.qualify(records, "a" * 40)

    def test_pending_failed_or_cancelled_latest_blocks_older_green(self):
        for changes in (dict(status="in_progress", conclusion=None), dict(conclusion="failure"),
                        dict(conclusion="cancelled")):
            with self.subTest(changes=changes), self.assertRaises(ValueError):
                qualification.qualify([self.run_record(), self.run_record(run_number=2, **changes)], "a" * 40)

    def test_latest_attempt_is_required(self):
        with self.assertRaises(ValueError):
            qualification.qualify([self.run_record(run_attempt=1),
                                   self.run_record(run_attempt=2, conclusion="failure")], "a" * 40)
