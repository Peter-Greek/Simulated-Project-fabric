#!/usr/bin/env python3
"""Mark the individual injectors the mixin processor cannot map.

`tools/remap_mixins.py` takes `remap = false` off every mixin, because the flag
suppresses refmap generation for the *whole* class and so quietly breaks the
vanilla `@At` targets inside it. What is left is a much smaller problem: a
handful of injectors whose target method belongs to another mod and shares its
simple name with a Minecraft method, which the processor reports as

    error: Unable to locate obfuscation mapping for @Inject target remove

Those names are the mod's own and read the same in both namespaces, so they
need no refmap entry — but the processor will not compile until it is told so.
This adds `remap = false` to exactly those injectors, one compile at a time,
leaving the rest of the mixin remapped.

Before adding the flag, check by hand that the injector's `@At` does not target
a Minecraft member; if it does, the flag would break it and the injector needs
restructuring instead. The script prints each `@At` target it is about to
silence so that check is possible.

    python tools/mixin_remap_exceptions.py
"""

import io
import os
import re
import subprocess
import sys

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
GRADLE = os.path.join(REPO, "gradlew.bat" if os.name == "nt" else "gradlew")
ERROR = re.compile(r"^(.*\.java):(\d+): error: Unable to locate obfuscation mapping "
                   r"for @(\w+) target (.+)$")


def compile_once():
    proc = subprocess.run([GRADLE, ":simulated:fabric:compileJava"],
                          cwd=REPO, capture_output=True, text=True)
    return proc.stdout + proc.stderr


def annotation_span(text, start):
    """The character range of the annotation whose first '(' follows `start`."""
    open_at = text.index("(", start)
    depth = 0
    for index in range(open_at, len(text)):
        if text[index] == "(":
            depth += 1
        elif text[index] == ")":
            depth -= 1
            if depth == 0:
                return open_at, index
    raise ValueError("unbalanced annotation at offset %d" % start)


def mark(path, line_no):
    text = io.open(path, encoding="utf-8").read()
    offset = 0
    for _ in range(line_no - 1):
        offset = text.index("\n", offset) + 1
    open_at, close_at = annotation_span(text, offset)

    body = text[open_at + 1:close_at]
    if "remap" in body:
        return False
    for target in re.findall(r'target\s*=\s*"([^"]*)"', body):
        if "net/minecraft/" in target:
            print("  !! silences a Minecraft target: " + target)
    updated = text[:close_at] + ", remap = false" + text[close_at:]
    io.open(path, "w", encoding="utf-8", newline="\n").write(updated)
    return True


def main():
    for attempt in range(1, 11):
        output = compile_once()
        found = []
        for raw in output.splitlines():
            match = ERROR.match(raw.strip())
            if match:
                found.append((match.group(1), int(match.group(2)),
                              match.group(3), match.group(4)))
        if not found:
            print("clean after %d compile(s)" % attempt)
            return 0

        # One file at a time, bottom up, so earlier line numbers stay valid.
        for path, line_no, kind, target in sorted(set(found), key=lambda f: (f[0], -f[1])):
            print("@%s %s  (%s:%d)" % (kind, target, os.path.relpath(path, REPO), line_no))
            mark(path, line_no)

    print("still failing after 10 rounds", file=sys.stderr)
    return 1


if __name__ == "__main__":
    sys.exit(main())
