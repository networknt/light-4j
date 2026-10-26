"""Trusted full-reactor report coverage policy (not supplied by the candidate)."""
import json
from pathlib import Path
from xml.parsers import expat

COUNTERS = ("tests", "failures", "errors", "skipped")
MAX_REPORT_BYTES = 16 * 1024 * 1024
MAX_TOTAL_BYTES = 256 * 1024 * 1024
MAX_REPORTS = 10000
MAX_XML_DEPTH = 64
EXECUTED_TOLERANCE_PERCENT = 5


def minimum_executed(baseline):
    if type(baseline) is not int or baseline < 0:
        raise ValueError("Invalid executed-test baseline")
    if baseline == 0:
        return 0
    allowance = max(1, (baseline * EXECUTED_TOLERANCE_PERCENT + 99) // 100)
    return max(1, baseline - allowance)


class InventoryMismatch(ValueError):
    pass


class TestFloorError(ValueError):
    pass


class UnsafeReportError(ValueError):
    pass


class BaselineGrowthError(TestFloorError):
    pass


def require_current_baselines(modules, inventory):
    """Promotion must review sustained growth; renewal never mutates policy."""
    validate_modules(modules, inventory)
    stale = []
    for module, counts in modules.items():
        executed = counts["tests"] - counts["skipped"]
        if minimum_executed(executed) > inventory[module]["baseline_executed"]:
            stale.append(module + ": baseline=" + str(inventory[module]["baseline_executed"])
                         + ", observed=" + str(executed))
    if stale:
        raise BaselineGrowthError("Qualification/promotion requires a reviewed test-baseline update for corpus growth: "
                             + "; ".join(stale)
                             + ". Confirm representative counts, update the policy inventory, "
                             "then qualify and promote that reviewed policy. No consumer patches were written.")


def read_report(path, remaining_bytes):
    """Read counters only; reject DTD/entities before expansion, in any encoding."""
    parser = expat.ParserCreate()
    suites, stack = [], []

    def reject(*args):
        raise UnsafeReportError("DTD and entity declarations/references are forbidden in test reports")

    def start(name, attrs):
        stack.append(name)
        if len(stack) > MAX_XML_DEPTH:
            raise UnsafeReportError("Test report exceeds XML depth limit")
        if stack == ["testsuite"] or stack == ["testsuites", "testsuite"]:
            if len(suites) >= 10000:
                raise UnsafeReportError("Too many test suites in one report")
            counts = {key: int(attrs.get(key, "0")) for key in COUNTERS}
            validate_counts(counts)
            suites.append(counts)

    parser.StartElementHandler = start
    parser.EndElementHandler = lambda name: stack.pop()
    parser.StartDoctypeDeclHandler = reject
    parser.EntityDeclHandler = reject
    parser.ExternalEntityRefHandler = reject
    parser.SkippedEntityHandler = reject
    parser.SetParamEntityParsing(expat.XML_PARAM_ENTITY_PARSING_NEVER)
    consumed = 0
    try:
        with path.open("rb") as stream:
            while chunk := stream.read(64 * 1024):
                consumed += len(chunk)
                if consumed > min(MAX_REPORT_BYTES, remaining_bytes):
                    raise UnsafeReportError("Test reports exceed byte limit")
                parser.Parse(chunk, False)
        parser.Parse(b"", True)
    except expat.ExpatError as error:
        raise UnsafeReportError("Malformed test report: " + str(path)) from error
    if not suites:
        raise ValueError("Missing testsuite: " + str(path))
    return suites, consumed


def validate_counts(counts):
    if set(counts) != set(COUNTERS) or any(type(n) is not int or n < 0 for n in counts.values()):
        raise ValueError("Invalid test counters")
    if counts["skipped"] > counts["tests"] or counts["failures"] or counts["errors"]:
        raise ValueError("Unsuccessful test reports")


def validate_modules(modules, inventory):
    if set(modules) != set(inventory):
        raise InventoryMismatch("Test module coverage differs: missing=" + str(sorted(set(inventory) - set(modules)))
                         + "; unexpected=" + str(sorted(set(modules) - set(inventory))))
    totals = dict.fromkeys(COUNTERS, 0)
    for module, counts in modules.items():
        validate_counts(counts)
        minimum = inventory[module]["minimum_executed"]
        if minimum < 1 and not inventory[module].get("disabled_reason"):
            raise ValueError("Disabled module requires a reviewed reason: " + module)
        if counts["tests"] == 0 or counts["tests"] - counts["skipped"] < minimum:
            raise TestFloorError("Insufficient executed tests for " + module + ": executed="
                                 + str(counts["tests"] - counts["skipped"]) + "; required=" + str(minimum))
        for key in COUNTERS:
            totals[key] += counts[key]
    if totals["tests"] - totals["skipped"] <= 0:
        raise ValueError("No executed reactor tests")
    return totals


def collect_report_counts(root):
    """Read bounded report counters without deciding inventory/floor acceptance."""
    modules = {}
    consumed = 0
    for number, path in enumerate(root.rglob("target/surefire-reports/TEST-*.xml"), start=1):
        if number > MAX_REPORTS:
            raise UnsafeReportError("Too many test report files")
        if path.is_symlink():
            raise ValueError("Symlinked test report: " + str(path))
        module = path.parent.parent.parent.relative_to(root).as_posix()
        counts = modules.setdefault(module, dict.fromkeys(COUNTERS, 0))
        suites, report_bytes = read_report(path, MAX_TOTAL_BYTES - consumed)
        consumed += report_bytes
        for current in suites:
            for key in COUNTERS:
                counts[key] += current[key]
    return modules


def collect_reports(root, inventory):
    modules = collect_report_counts(root)
    return modules, validate_modules(modules, inventory)


def load_inventory():
    inventory = json.loads(Path(__file__).with_name("jackson3-test-modules.json").read_text())
    for module, entry in inventory.items():
        if entry["minimum_executed"] != minimum_executed(entry["baseline_executed"]):
            raise ValueError("Inventory floor differs from reviewed tolerance: " + module)
    return inventory
