#!/usr/bin/env python3
"""Read native fixture artifact paths from Maven's effective model."""
import sys
import xml.etree.ElementTree as ET
from pathlib import Path


def artifact_paths(effective_pom):
    root = ET.parse(effective_pom).getroot()
    ns = {"m": "http://maven.apache.org/POM/4.0.0"}
    directory = root.findtext("m:build/m:directory", namespaces=ns)
    final_name = root.findtext("m:build/m:finalName", namespaces=ns)
    packaging = root.findtext("m:packaging", default="jar", namespaces=ns)
    if packaging != "jar" or not directory or not final_name:
        raise ValueError("Native fixture requires jar packaging and resolved build.directory/build.finalName")
    if "${" in directory or "${" in final_name or "\n" in directory or "\n" in final_name:
        raise ValueError("Unresolved or multiline Maven build paths")
    return Path(directory), Path(directory) / (final_name + ".jar")


if __name__ == "__main__":
    try:
        directory, jar = artifact_paths(sys.argv[1])
        print(directory)
        print(jar)
    except (ValueError, ET.ParseError, IndexError) as error:
        sys.exit(str(error))
