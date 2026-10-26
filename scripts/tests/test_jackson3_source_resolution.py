import json
import os
from pathlib import Path
import re
import subprocess
import tempfile
import unittest


class SourceResolutionTest(unittest.TestCase):
    def resolve(self, state, override=None):
        workflow = Path(__file__).parents[2] / ".github/workflows/jackson3-source-build.yml"
        function = re.search(r"          resolve\(\) \{.*?\n          }\n", workflow.read_text(), re.S).group()
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            fake = root / "gh"
            fake.write_text('''#!/usr/bin/env python3
import json, os, sys
state = os.environ["TEST_STATE"]
with open(os.environ["TEST_CALLS"], "a") as calls: calls.write(" ".join(sys.argv[1:]) + "\\n")
if any("/pulls?" in item for item in sys.argv):
    pull = {"state": "open" if state == "open" else "closed", "head": {"sha": "a" * 40},
            "merged_at": "2026-01-01" if state == "merged" else None}
    print(json.dumps([[pull]]))
elif any("/commits/" in item for item in sys.argv):
    ref = next(item.split("/commits/")[1] for item in sys.argv if "/commits/" in item)
    print("b" * 40 if ref == "main" else ref)
else: print("main")
''')
            fake.chmod(0o755)
            output, calls = root / "output", root / "calls"
            env = dict(os.environ, PATH=str(root) + os.pathsep + os.environ["PATH"],
                       TEST_STATE=state, TEST_CALLS=str(calls), GITHUB_OUTPUT=str(output),
                       SOURCE_REFS=json.dumps(override or {}))
            run = subprocess.run(["bash", "-euo", "pipefail", "-c", function + "\nresolve example migration-branch"],
                                 env=env, text=True, capture_output=True)
            return run, output.read_text() if output.exists() else "", calls.read_text()

    def test_open_branch_uses_current_pull_head(self):
        run, output, _ = self.resolve("open")
        self.assertEqual(0, run.returncode, run.stderr)
        self.assertEqual("example=" + "a" * 40 + "\n", output)

    def test_merged_branch_uses_current_default_branch(self):
        run, output, _ = self.resolve("merged")
        self.assertEqual(0, run.returncode, run.stderr)
        self.assertEqual("example=" + "b" * 40 + "\n", output)

    def test_closed_unmerged_branch_fails(self):
        run, _, _ = self.resolve("closed")
        self.assertNotEqual(0, run.returncode)
        self.assertIn("closed without merging", run.stderr)

    def test_explicit_source_replay_bypasses_branch_discovery(self):
        run, output, calls = self.resolve("closed", {"example": "c" * 40})
        self.assertEqual(0, run.returncode, run.stderr)
        self.assertEqual("example=" + "c" * 40 + "\n", output)
        self.assertNotIn("/pulls?", calls)
