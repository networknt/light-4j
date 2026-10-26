"""Frozen v1 acceptance-log protocol, including historical coordinator runs.

These messages are machine-readable records, not cosmetic UI text. Add a new
version for wording changes and retain v1 parsing for existing acceptance files.
"""
import re
from types import MappingProxyType


V1 = MappingProxyType({
    "dispatched": "Dispatched qualification run: {run_id}",
    "renewed": "Verified renewed proof: {core_sha} run {run_id}",
    "healthy": "Healthy retained proof; no reactor dispatched: {core_sha}",
    "forced": "Verified forced registered renewal of {count} qualifications",
    "normal": "Verified renewal of {count} qualifications; healthy proofs did not trigger builds",
})


def protocol(version=1):
    if type(version) is not int or version != 1:
        raise ValueError("Unsupported refresh-log protocol version")
    return V1


def message(name, version=1, **values):
    return protocol(version)[name].format(**values)


def has_forced_success(log, version=1):
    pattern = re.escape(protocol(version)["forced"]).replace(re.escape("{count}"), "[1-9][0-9]*")
    return re.search(r"(?m)" + pattern + r"\s*$", log) is not None
