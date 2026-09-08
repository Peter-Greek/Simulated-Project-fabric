#!/usr/bin/env python3
"""Drop the HolderLookup.Provider parameter 1.20.5 added to NBT and clipboard hooks.

1.20.5 threaded a registry lookup through every serialisation path so that
registry references could be written by id. On 1.20.1 those methods take no
lookup, so a ported override has to lose the parameter — and every call to
{@code super} has to lose the argument with it.

The parameter is removed rather than replaced: on 1.20.1 the codecs that would
have used it take the registry access from the level instead, so nothing in the
body needs it. A body that does reference the parameter is reported and left
alone.

    python tools/drop_registries_param.py [files...]
"""

import io
import os
import re
import sys

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DST = os.path.join(REPO, "simulated", "fabric", "src", "main", "java")

# Declarations: "(CompoundTag tag, HolderLookup.Provider registries, boolean x)"
DECL = re.compile(
    r",\s*(?:final\s+)?HolderLookup\.(?:@\w+\s+)?Provider\s+(\w+)")
# The clipboard hooks put the lookup first instead.
DECL_FIRST = re.compile(
    r"\(\s*(?:final\s+)?HolderLookup\.(?:@\w+\s+)?Provider\s+(\w+)\s*,\s*")

# Calls: super.read(tag, registries, clientPacket)
CALLS = re.compile(
    r"\b(super|this)\.(read|write|writeSafe|readSafe|writeToClipboard|readFromClipboard)\(([^()]*)\)")


def strip_declarations(text):
    names = set()

    def take(match):
        names.add(match.group(1))
        return ""

    def take_first(match):
        names.add(match.group(1))
        return "("

    text = DECL.sub(take, text)
    text = DECL_FIRST.sub(take_first, text)
    return text, names


def strip_calls(text, names):
    def fix(match):
        args = [arg.strip() for arg in match.group(3).split(",") if arg.strip()]
        kept = [arg for arg in args if arg not in names]
        return "%s.%s(%s)" % (match.group(1), match.group(2), ", ".join(kept))

    return CALLS.sub(fix, text)


def collect(paths):
    if paths:
        return paths
    found = []
    for root, _, names in os.walk(DST):
        if "backport" in root.replace(os.sep, "/"):
            continue
        for name in sorted(names):
            if name.endswith(".java"):
                found.append(os.path.join(root, name))
    return found


def main():
    changed = 0
    for path in collect(sys.argv[1:]):
        with open(path, encoding="utf-8") as handle:
            text = handle.read()
        if "HolderLookup" not in text:
            continue
        stripped, names = strip_declarations(text)
        if not names:
            continue
        stripped = strip_calls(stripped, names)
        rel = os.path.relpath(path, DST).replace(os.sep, "/")
        leftover = [name for name in names if re.search(r"\b" + name + r"\b", stripped)]
        if leftover:
            print("  review: %s still uses %s" % (rel, ", ".join(sorted(leftover))))
        if "HolderLookup" not in stripped:
            stripped = re.sub(r"^import net\.minecraft\.core\.HolderLookup;\r?\n", "",
                              stripped, flags=re.M)
        io.open(path, "w", encoding="utf-8", newline="\n").write(stripped)
        print("stripped " + rel)
        changed += 1
    print("%d files stripped" % changed)


if __name__ == "__main__":
    main()
