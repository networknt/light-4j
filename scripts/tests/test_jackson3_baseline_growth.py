from pathlib import Path
import sys
import unittest

sys.path.insert(0, str(Path(__file__).parents[1]))
from jackson3_test_inventory import minimum_executed, require_current_baselines, TestFloorError


class BaselineGrowthTest(unittest.TestCase):
    def check(self, baseline, observed):
        inventory = {"module": {"baseline_executed": baseline,
                                "minimum_executed": minimum_executed(baseline)}}
        counts = {"module": {"tests": observed, "skipped": 0, "failures": 0, "errors": 0}}
        require_current_baselines(counts, inventory)

    def test_growth_requires_review_before_consumer_promotion(self):
        with self.assertRaisesRegex(TestFloorError, "reviewed test-baseline update"):
            self.check(100, 200)

    def test_small_runner_variation_does_not_force_a_new_baseline(self):
        for observed in (95, 100, 105):
            self.check(100, observed)

    def test_reviewed_growth_protects_added_tests(self):
        self.check(200, 200)
        with self.assertRaisesRegex(TestFloorError, "Insufficient executed tests"):
            self.check(200, 100)

    def test_skips_do_not_count_as_growth(self):
        inventory = {"module": {"baseline_executed": 100, "minimum_executed": 95}}
        require_current_baselines({"module": {"tests": 200, "skipped": 100,
                                              "failures": 0, "errors": 0}}, inventory)


if __name__ == "__main__":
    unittest.main()
