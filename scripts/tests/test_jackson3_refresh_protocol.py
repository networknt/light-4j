"""Guard compatibility with logs emitted before shared protocol extraction."""
from pathlib import Path
import sys
import unittest

sys.path.insert(0, str(Path(__file__).parents[1]))
from jackson3_refresh_protocol import message, has_forced_success, protocol


class RefreshProtocolTest(unittest.TestCase):
    def test_frozen_v1_matches_historical_messages(self):
        self.assertEqual("Dispatched qualification run: 11", message("dispatched", run_id=11))
        self.assertEqual("Verified renewed proof: abc run 11", message("renewed", core_sha="abc", run_id=11))
        self.assertEqual("Healthy retained proof; no reactor dispatched: abc", message("healthy", core_sha="abc"))
        self.assertEqual("Verified forced registered renewal of 1 qualifications", message("forced", count=1))
        self.assertEqual("Verified renewal of 0 qualifications; healthy proofs did not trigger builds", message("normal", count=0))
        self.assertTrue(has_forced_success("timestamp Verified forced registered renewal of 1 qualifications\n"))
        self.assertFalse(has_forced_success("Verified forced registered renewal of 0 qualifications"))

    def test_protocol_cannot_be_silently_rewritten_or_unknown_versions_assumed(self):
        with self.assertRaises(TypeError):
            protocol()["forced"] = "new wording"
        for version in (2, True, "1"):
            with self.subTest(version=version), self.assertRaises(ValueError):
                protocol(version)
