import hashlib
from pathlib import Path
import sys
import tempfile
import unittest
from unittest.mock import patch

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import jackson3_artifact_audit as audit


class ArtifactAuditTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)

    def artifact(self, version, local=False, name="example", classifier=""):
        artifact = audit.Artifact("com.networknt", name, "jar", version, classifier)
        path = artifact.path(self.root)
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_bytes(b"fixture")
        (path.parent / "_remote.repositories").write_text(artifact.filename + (">=\n" if local else ">central=\n"))
        return artifact

    def test_remote_snapshots_and_releases_both_fail(self):
        for version in ("4.0.0-SNAPSHOT", "4.0.0"):
            artifact = self.artifact(version)
            result = audit.audit(self.root, artifact.coordinates)
            self.assertEqual(len(result["failures"]), 1)

    def test_source_installed_releases_and_snapshots_both_pass(self):
        for version in ("4.0.0-SNAPSHOT", "4.0.0"):
            artifact = self.artifact(version, local=True)
            self.assertFalse(audit.audit(self.root, artifact.coordinates)["failures"])

    def test_known_jackson2_releases_fail_even_with_local_marker(self):
        for name, version in (("yaml-rule", "3.0.1"), ("chaos-monkey", "2.4.1")):
            artifact = self.artifact(version, local=True, name=name)
            self.assertIn("known Jackson 2 release", audit.audit(self.root, artifact.coordinates)["failures"][0])

    def test_release_exception_requires_exact_coordinate_and_hash(self):
        artifact = self.artifact("3.0.8", name="json-schema-validator")
        with patch.dict(audit.APPROVED_RELEASES, {artifact.coordinates: hashlib.sha256(b"fixture").hexdigest()}, clear=True):
            self.assertFalse(audit.audit(self.root, artifact.coordinates)["failures"])
            artifact.path(self.root).write_bytes(b"changed")
            self.assertIn("SHA-256 mismatch", audit.audit(self.root, artifact.coordinates)["failures"][0])

    def test_source_built_validator_is_reported_as_source_not_published_exception(self):
        artifact = self.artifact("3.0.8", local=True, name="json-schema-validator")
        result = audit.audit(self.root, artifact.coordinates)
        self.assertFalse(result["failures"])
        self.assertEqual(result["source_installed"], [artifact.coordinates])
        self.assertEqual(result["approved_releases"], [])

    def test_classifier_and_all_reactor_modules_are_checked(self):
        one = self.artifact("4.0.0-SNAPSHOT", local=True)
        two = self.artifact("4.0.0-SNAPSHOT", name="other", classifier="tests")
        tree = "[INFO] " + one.coordinates + "\n[INFO] +- " + two.coordinates + ":test\n"
        result = audit.audit(self.root, tree)
        self.assertEqual(len(result["checked"]), 2)
        self.assertIn(two.coordinates, result["failures"][0])

    def test_foreign_jackson2_dependencies_are_reported_but_annotations_are_allowed(self):
        one = self.artifact("4.0.0-SNAPSHOT", local=True)
        tree = one.coordinates + "\ncom.fasterxml.jackson.core:jackson-databind:jar:2.22.3:compile\n" + \
               "com.fasterxml.jackson.core:jackson-annotations:jar:2.22:compile\n"
        result = audit.audit(self.root, tree)
        self.assertEqual(result["jackson2_dependencies"], ["com.fasterxml.jackson.core:jackson-databind:jar:2.22.3"])

    def test_empty_tree_is_rejected(self):
        self.assertTrue(audit.audit(self.root, "")["failures"])


if __name__ == "__main__":
    unittest.main()
