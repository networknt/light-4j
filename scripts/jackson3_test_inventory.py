"""Trusted full-reactor report coverage policy (not supplied by the candidate)."""
import json
from pathlib import Path
import xml.etree.ElementTree as ET

COUNTERS = ("tests", "failures", "errors", "skipped")


def validate_counts(counts):
    if set(counts) != set(COUNTERS) or any(type(n) is not int or n < 0 for n in counts.values()):
        raise ValueError("Invalid test counters")
    if counts["skipped"] > counts["tests"] or counts["failures"] or counts["errors"]:
        raise ValueError("Unsuccessful test reports")


def validate_modules(modules, inventory):
    if set(modules) != set(inventory):
        raise ValueError("Test module coverage differs: missing=" + str(sorted(set(inventory) - set(modules)))
                         + "; unexpected=" + str(sorted(set(modules) - set(inventory))))
    totals = dict.fromkeys(COUNTERS, 0)
    for module, counts in modules.items():
        validate_counts(counts)
        minimum = inventory[module]["minimum_executed"]
        if minimum < 1 and not inventory[module].get("disabled_reason"):
            raise ValueError("Disabled module requires a reviewed reason: " + module)
        if counts["tests"] == 0 or counts["tests"] - counts["skipped"] < minimum:
            raise ValueError("Insufficient executed tests for " + module)
        for key in COUNTERS:
            totals[key] += counts[key]
    if totals["tests"] - totals["skipped"] <= 0:
        raise ValueError("No executed reactor tests")
    return totals


def collect_reports(root, inventory):
    modules = {}
    for path in sorted(root.rglob("target/surefire-reports/TEST-*.xml")):
        if path.is_symlink():
            raise ValueError("Symlinked test report: " + str(path))
        module = path.parent.parent.parent.relative_to(root).as_posix()
        counts = modules.setdefault(module, dict.fromkeys(COUNTERS, 0))
        report = ET.parse(path).getroot()
        suites = [report] if report.tag == "testsuite" else report.findall("testsuite")
        if not suites:
            raise ValueError("Missing testsuite: " + str(path))
        for suite in suites:
            current = {key: int(suite.get(key, "0")) for key in COUNTERS}
            validate_counts(current)
            for key in COUNTERS:
                counts[key] += current[key]
    return modules, validate_modules(modules, inventory)


def load_inventory():
    return json.loads(Path(__file__).with_name("jackson3-test-modules.json").read_text())
