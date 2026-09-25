#!/usr/bin/env python3
"""Increment versionCode and the patch component of versionName.

Used by the Dependabot automation workflow so that automated dependency
PRs honour the repo rule that every commit bumps the app version
(app/build.gradle.kts). Prints the new version for the commit message.
"""
import pathlib
import re

path = pathlib.Path(__file__).resolve().parent.parent / "app" / "build.gradle.kts"
text = path.read_text()

code_match = re.search(r"versionCode = (\d+)", text)
name_match = re.search(r'versionName = "(\d+)\.(\d+)\.(\d+)"', text)
if not code_match or not name_match:
    raise SystemExit("Could not find versionCode/versionName in app/build.gradle.kts")

new_code = int(code_match.group(1)) + 1
major, minor, patch = name_match.groups()
new_name = f"{major}.{minor}.{int(patch) + 1}"

text = text.replace(code_match.group(0), f"versionCode = {new_code}", 1)
text = text.replace(name_match.group(0), f'versionName = "{new_name}"', 1)
path.write_text(text)

print(f"{new_code} ({new_name})")
