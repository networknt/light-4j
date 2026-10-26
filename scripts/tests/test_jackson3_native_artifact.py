import importlib.util
import tempfile
import unittest
from pathlib import Path

spec = importlib.util.spec_from_file_location("native_config_artifact",
        Path(__file__).parents[1] / "native_config_artifact.py")
module = importlib.util.module_from_spec(spec)
spec.loader.exec_module(module)


class NativeArtifactPathsTest(unittest.TestCase):
    def model(self, directory, name, packaging="jar"):
        return ('<project xmlns="http://maven.apache.org/POM/4.0.0">'
                f'<packaging>{packaging}</packaging><build><directory>{directory}</directory>'
                f'<finalName>{name}</finalName></build></project>')

    def parse(self, text):
        with tempfile.TemporaryDirectory() as work:
            pom = Path(work) / "effective.xml"
            pom.write_text(text)
            return module.artifact_paths(pom)

    def test_version_bump_and_custom_build_paths(self):
        directory, jar = self.parse(self.model("/tmp/custom build", "renamed-3.0.0-RC1"))
        self.assertEqual(Path("/tmp/custom build"), directory)
        self.assertEqual(directory / "renamed-3.0.0-RC1.jar", jar)

    def test_rejects_unresolved_or_non_jar_model(self):
        for text in [self.model("${project.basedir}/target", "artifact"),
                     self.model("/tmp/build", "${revision}"),
                     self.model("/tmp/build", "artifact", "pom")]:
            with self.assertRaises(ValueError):
                self.parse(text)
