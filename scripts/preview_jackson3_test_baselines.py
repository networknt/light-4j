#!/usr/bin/env python3
"""Report observed counts and review gaps; never qualify or change an inventory."""
import argparse
import json
from pathlib import Path

from jackson3_test_inventory import COUNTERS, collect_report_counts, load_inventory, minimum_executed


def preview(root, inventory):
    counts = collect_report_counts(root)
    missing = sorted(set(inventory) - set(counts))
    modules = {}
    totals = dict.fromkeys(COUNTERS, 0)
    for module, observed in sorted(counts.items()):
        executed = observed["tests"] - observed["skipped"]
        policy = inventory.get(module)
        issues = []
        if policy is None:
            issues.append("new_module_requires_inventory_review")
        else:
            if observed["tests"] == 0 or executed < policy["minimum_executed"]:
                issues.append("below_current_floor")
            if minimum_executed(executed) > policy["baseline_executed"]:
                issues.append("growth_requires_baseline_review")
        modules[module] = dict(observed, executed=executed,
                               baseline_executed=policy["baseline_executed"] if policy else None,
                               current_floor=policy["minimum_executed"] if policy else None,
                               review_issues=issues)
        for counter in COUNTERS:
            totals[counter] += observed[counter]
    return {"purpose": "review-only counts; not qualification evidence",
            "modules": modules, "missing_modules": missing, "totals": totals,
            "needs_review": bool(missing or any(m["review_issues"] for m in modules.values()))}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--reports-root", type=Path, required=True)
    parser.add_argument("--output", type=Path)
    args = parser.parse_args()
    if not args.reports_root.is_dir():
        parser.error("--reports-root must be the extracted candidate reports directory")
    report = json.dumps(preview(args.reports_root, load_inventory()), indent=2) + "\n"
    if args.output:
        args.output.write_text(report)
    else:
        print(report, end="")


if __name__ == "__main__":
    main()
