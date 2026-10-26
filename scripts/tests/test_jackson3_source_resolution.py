import importlib.util
from pathlib import Path
import unittest

spec = importlib.util.spec_from_file_location("resolution", Path(__file__).parents[1] / "resolve_jackson3_prerequisites.py")
resolution = importlib.util.module_from_spec(spec)
spec.loader.exec_module(resolution)


class SourceResolutionTest(unittest.TestCase):
    def test_defaults_resolve_once_without_branch_or_pr_lookup(self):
        calls = []
        def api(endpoint):
            calls.append(endpoint)
            return {"default_branch": "master"} if "/commits/" not in endpoint else {"sha": "a" * 40}
        self.assertEqual({"yaml-rule": "a" * 40, "http-client": "a" * 40}, resolution.resolve({}, api))
        self.assertEqual(4, len(calls))
        self.assertFalse(any("pulls" in call for call in calls))

    def test_explicit_full_shas_never_resolve_moving_heads(self):
        refs = {"yaml-rule": "b" * 40, "http-client": "c" * 40}
        def forbidden(endpoint):
            self.fail("Unexpected API call: " + endpoint)
        self.assertEqual(refs, resolution.resolve(refs, forbidden))

    def test_invalid_override_is_rejected(self):
        for refs in ([], {"other": "a" * 40}, {"yaml-rule": "master"}, {"http-client": None}):
            with self.subTest(refs=refs), self.assertRaises(ValueError):
                resolution.resolve(refs, lambda endpoint: self.fail(endpoint))

    def test_workflow_has_no_dead_branch_resolver(self):
        text = (Path(__file__).parents[2] / ".github/workflows/jackson3-source-build.yml").read_text()
        self.assertNotIn("pulls?head", text)
        self.assertNotIn("branch=${2", text)
