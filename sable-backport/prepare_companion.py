#!/usr/bin/env python3
from pathlib import Path
import sys

if len(sys.argv) != 2:
    raise SystemExit("usage: prepare_companion.py <sable-companion checkout>")

root = Path(sys.argv[1]).resolve()
versions = root / "libs.versions.toml"
text = versions.read_text(encoding="utf-8")
replacements = {
    'java-version = "21"': 'java-version = "17"',
    'minecraft = "1.21.1"': 'minecraft = "1.20.1"',
    'loom = "1.16.+"': 'loom = "1.8.13"',
}
for old, new in replacements.items():
    if old not in text:
        raise SystemExit(f"expected version line not found: {old}")
    text = text.replace(old, new)
versions.write_text(text, encoding="utf-8")

# Give the experimental artifact a distinct version so it can never be
# confused with RyanHCode's official release when installed/published locally.
properties = root / "gradle.properties"
text = properties.read_text(encoding="utf-8")
if "version = 1.6.0" not in text:
    raise SystemExit("unexpected Sable Companion version")
text = text.replace("version = 1.6.0", "version = 1.6.0-fabric-1.20.1-homestead.1")
properties.write_text(text, encoding="utf-8")

print("Prepared Sable Companion for Java 17 / Minecraft 1.20.1")
