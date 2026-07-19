#!/usr/bin/env python3
"""Turns the aggregated JaCoCo XML report into a Markdown summary table.

Usage: coverage_summary.py <path-to-jacocoFullReport.xml>
CI appends the output to $GITHUB_STEP_SUMMARY so every PR run shows line
coverage per app area without downloading the HTML report artifact.
"""
import sys
import xml.etree.ElementTree as ET
from collections import defaultdict


def area_for(package_name: str) -> str:
    """Groups JVM packages into app areas: core/feature modules keep their
    module segment (com/podbelly/core/playback), app packages keep one level
    (com/podbelly/navigation), everything else lands on its own name."""
    parts = package_name.split("/")
    if len(parts) >= 4 and parts[:2] == ["com", "podbelly"] and parts[2] in ("core", "feature"):
        return ".".join(parts[:4])
    return ".".join(parts[:3])


def percent(missed: int, covered: int) -> str:
    total = missed + covered
    if total == 0:
        return "n/a"
    return f"{100.0 * covered / total:.1f}%"


def main() -> None:
    root = ET.parse(sys.argv[1]).getroot()

    areas = defaultdict(lambda: [0, 0])  # area -> [missed, covered]
    for package in root.findall("package"):
        for counter in package.findall("counter"):
            if counter.get("type") == "LINE":
                area = areas[area_for(package.get("name"))]
                area[0] += int(counter.get("missed"))
                area[1] += int(counter.get("covered"))

    print("## Unit-test line coverage")
    print()
    print("| Area | Coverage | Lines |")
    print("|---|---:|---:|")
    for name in sorted(areas):
        missed, covered = areas[name]
        print(f"| `{name}` | {percent(missed, covered)} | {covered}/{missed + covered} |")

    for counter in root.findall("counter"):
        if counter.get("type") == "LINE":
            missed = int(counter.get("missed"))
            covered = int(counter.get("covered"))
            print(f"| **Total** | **{percent(missed, covered)}** | {covered}/{missed + covered} |")


if __name__ == "__main__":
    main()
