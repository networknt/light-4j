"""Execute the workflow's actual routing shell without contacting GitHub."""
import os
from pathlib import Path
import subprocess
import tempfile
import textwrap
import unittest


class RefreshWorkflowTest(unittest.TestCase):
    def test_event_routing(self):
        workflow = (Path(__file__).parents[2] / ".github/workflows/jackson3-qualification-refresh.yml").read_text()
        self.assertNotIn("        if:", workflow)
        self.assertIn("REFRESH_EVENT: ${{ github.event_name }}", workflow)
        self.assertIn("FORCE_RENEWAL: ${{ inputs.force_renewal || false }}", workflow)
        shell = textwrap.dedent(workflow.split("        run: |\n", 1)[1])
        with tempfile.TemporaryDirectory() as directory:
            stub = Path(directory) / "python3"
            stub.write_text('#!/bin/sh\nprintf "%s\\n" "$@"\n')
            stub.chmod(0o755)
            for event, force, accepted, forced in [
                ("schedule", "", True, False),
                ("schedule", "false", True, False),
                ("workflow_dispatch", "false", True, False),
                ("workflow_dispatch", "true", True, True),
                ("workflow_dispatch", "", False, False),
                ("push", "false", False, False),
            ]:
                with self.subTest(event=event, force=force):
                    result = subprocess.run(["bash", "-e", "-c", shell], capture_output=True, text=True,
                        env={**os.environ, "PATH": directory + os.pathsep + os.environ["PATH"],
                             "REFRESH_EVENT": event, "FORCE_RENEWAL": force})
                    self.assertEqual(result.returncode == 0, accepted, result.stderr)
                    expected = ["scripts/refresh_jackson3_qualifications.py"] if accepted else []
                    if forced:
                        expected.append("--force-renewal")
                    self.assertEqual(result.stdout.splitlines(), expected)
