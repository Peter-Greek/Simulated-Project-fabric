#!/usr/bin/env python3
"""Turn a Sable backport compile log into a readable error summary.

usage: summarise_errors.py <compile log> [label]

Prints GitHub-flavoured Markdown on stdout, so CI can append it to
$GITHUB_STEP_SUMMARY. A raw total is close to useless on its own -- the useful
question is which *kind* of gap the errors come from, because the three kinds
have wildly different costs. So the summary splits them:

  vanilla    an API that moved or was renamed between 1.20.1 and 1.21.1
  veil       a Veil API absent from Veil's 1.20.1 line (1.0.0.x)
  optional   a third-party mod integration, droppable without losing physics
  other      everything else, including cascades from the above
"""

from __future__ import annotations

import collections
import pathlib
import re
import sys

ERROR_LINE = re.compile(r"\.java:\d+: error:")
LOCATION = re.compile(r"dev/ryanhcode/sable/(.*?)\.java:\d+: error:")
MISSING_PACKAGE = re.compile(r"error: package (\S+) does not exist")

# Package prefixes belonging to optional third-party integrations. None of these
# is required for physics; the port can ship without any of them.
OPTIONAL_PREFIXES = (
    "com.github.exopandora",       # Shoulder Surfing
    "net.caffeinemc",              # Sodium
    "net.irisshaders",             # Iris
    "snownee.jade",                # Jade
    "dan200.computercraft",        # CC: Tweaked
    "net.mehvahdjukaar",           # Moonlight, Vista
    "io.github.mortuusars",        # Exposure
    "toni.sodiumextras",
)


def classify(package: str) -> str:
    if package.startswith(OPTIONAL_PREFIXES):
        return "optional"
    if package.startswith("foundry.veil") or package.startswith("io.github.ocelot"):
        return "veil"
    if package.startswith("net.minecraft"):
        return "vanilla"
    return "other"


def main() -> int:
    log = pathlib.Path(sys.argv[1])
    label = sys.argv[2] if len(sys.argv) > 2 else log.name
    if not log.is_file():
        print(f"### {label}\n\nNo compile log at `{log}`.")
        return 0

    text = log.read_text(encoding="utf-8", errors="replace")
    lines = text.splitlines()

    errors = [line for line in lines if ERROR_LINE.search(line)]
    reported = re.search(r"^(\d+) errors?$", text, re.M)
    total = int(reported.group(1)) if reported else len(errors)

    areas: collections.Counter[str] = collections.Counter()
    for line in errors:
        match = LOCATION.search(line.replace("\\", "/"))
        if not match:
            areas["<outside the sable package>"] += 1
            continue
        parts = match.group(1).split("/")
        areas["/".join(parts[:2]) if len(parts) > 1 else parts[0]] += 1

    packages: collections.Counter[str] = collections.Counter()
    for line in lines:
        match = MISSING_PACKAGE.search(line)
        if match:
            packages[match.group(1)] += 1

    kinds: collections.Counter[str] = collections.Counter()
    for package, count in packages.items():
        kinds[classify(package)] += count

    print(f"### {label}")
    print()
    print(f"**{total} errors** across {len({a for a in areas})} areas.")
    print()

    if kinds:
        print("| Missing-package errors by kind | Count |")
        print("| --- | ---: |")
        for kind in ("vanilla", "veil", "optional", "other"):
            if kinds[kind]:
                print(f"| {kind} | {kinds[kind]} |")
        print()

    print("<details><summary>Errors by package area</summary>")
    print()
    print("| Area | Errors |")
    print("| --- | ---: |")
    for area, count in areas.most_common(30):
        print(f"| `{area}` | {count} |")
    print()
    print("</details>")
    print()

    if packages:
        print("<details><summary>Missing packages</summary>")
        print()
        print("| Package | Kind | Errors |")
        print("| --- | --- | ---: |")
        for package, count in packages.most_common(40):
            print(f"| `{package}` | {classify(package)} | {count} |")
        print()
        print("A row with no dots in it is a nested-class reference whose outer")
        print("type failed to import; the cause is counted against that import.")
        print()
        print("</details>")

    return 0


if __name__ == "__main__":
    raise SystemExit(main())
