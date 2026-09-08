#!/usr/bin/env python3
"""Rewrite the Java 21 SequencedCollection calls javac rejected.

{@code List.getFirst()} and {@code getLast()} arrived with Java 21's
SequencedCollection. Mojang's {@code Pair} has had {@code getFirst()} for years
and is unrelated, so a blind rewrite would break it. This one is driven by the
compiler: only the exact call sites javac reported as missing are touched.

    python tools/fix_sequenced_calls.py <javac error log>
"""

import io
import re
import sys
from collections import defaultdict

BS = chr(92)


def receiver(line, end):
    """The expression a chained call at `end` is invoked on."""
    depth = 0
    i = end - 1
    while i >= 0:
        char = line[i]
        if char in ")]":
            depth += 1
        elif char in "([":
            if depth == 0:
                break
            depth -= 1
        elif depth == 0 and not (char.isalnum() or char in "_.$"):
            break
        i -= 1
    return line[i + 1:end]


def main():
    log = sys.argv[1] if len(sys.argv) > 1 else "build-errors.txt"
    lines = io.open(log, encoding="utf-8", errors="replace").read().split("\n")

    targets = defaultdict(set)
    for i, line in enumerate(lines):
        match = re.match(r"^(.*?):(\d+): error: cannot find symbol", line.replace(BS, "/"))
        if not match:
            continue
        window = " ".join(lines[i + 1:i + 4])
        if "method getFirst()" in window:
            targets[match.group(1)].add((int(match.group(2)), "getFirst"))
        elif "method getLast()" in window:
            targets[match.group(1)].add((int(match.group(2)), "getLast"))

    total = 0
    for path, sites in sorted(targets.items()):
        text = io.open(path, encoding="utf-8").read()
        source = text.split("\n")
        for number, which in sorted(sites, reverse=True):
            line = source[number - 1]
            call = "." + which + "()"
            at = line.find(call)
            if at < 0:
                print("  review: %s:%d has no %s" % (path, number, call))
                continue
            if which == "getFirst":
                source[number - 1] = line[:at] + ".get(0)" + line[at + len(call):]
            else:
                target = receiver(line, at)
                source[number - 1] = (line[:at] + ".get(" + target + ".size() - 1)"
                                      + line[at + len(call):])
            total += 1
        io.open(path, "w", encoding="utf-8", newline="\n").write("\n".join(source))
        print("fixed " + path)
    print("%d call sites rewritten" % total)


if __name__ == "__main__":
    main()
