#!/usr/bin/env python3
"""Run after HandlerContinuationTest has built this worktree; JDK allocation counters required."""
from pathlib import Path
import hashlib
import json
import statistics
import subprocess
import tempfile
import xml.etree.ElementTree as ET

folder = Path(__file__).resolve().parent
root = folder.parents[1]
report = ET.parse(root / "handler/target/surefire-reports/TEST-com.networknt.handler.HandlerContinuationTest.xml")
classpath = next(p.attrib["value"] for p in report.findall(".//property") if p.attrib["name"] == "java.class.path")
with tempfile.TemporaryDirectory(prefix="continuation-allocation-") as output:
    subprocess.run(["javac", "-cp", classpath, "-d", output, str(folder / "MdcAllocationProbe.java")], check=True)
    result = subprocess.run(["java", "-Xms64m", "-Xmx256m", "-cp", output + ":" + classpath, "MdcAllocationProbe"],
                            check=True, text=True, capture_output=True)
samples = [json.loads(line[6:]) for line in result.stdout.splitlines() if line.startswith("ALLOC:")]
sources = ["handler/src/main/java/com/networknt/handler/HandlerContinuation.java",
           "handler/src/main/java/com/networknt/handler/RequestMdcContext.java",
           "proxy-handler/src/main/java/com/networknt/handler/ProxyHandler.java"]
data = {
    "method": "ThreadMXBean warmed allocation probe: 100000 warmup, 100000 iterations per sample, three samples",
    "exclusions": ["registration/capture", "network transport", "logging", "gateway throughput"],
    "java": subprocess.run(["java", "-version"], text=True, capture_output=True, check=True).stderr,
    "samples": samples,
    "median_bytes_per_resume": {name: statistics.median(s["bytes_per_frame"] for s in samples if s["case"] == name)
                                for name in dict.fromkeys(s["case"] for s in samples)},
    "source_sha256": {name: hashlib.sha256((root / name).read_bytes()).hexdigest() for name in sources},
}
(folder / "allocation.json").write_text(json.dumps(data, indent=2) + "\n")
print(json.dumps(data["median_bytes_per_resume"], indent=2))
