#!/usr/bin/env python3
"""Close vertices that 1.21 closes for you.

1.21's {@code VertexConsumer} finishes a vertex when the builder chain ends;
1.20.1 needs an explicit {@code endVertex()}. Upstream therefore never calls it,
and a ported renderer would emit nothing at all — silently, since the code
compiles either way once the method names are mapped back.

So: every statement that starts a vertex gets {@code .endVertex()} appended.
Statements that already close, or that hand the consumer to something else
rather than finishing the vertex inline, are reported and left alone.

    python tools/end_vertex.py [files...]
"""

import io
import os
import sys

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DST = os.path.join(REPO, "simulated", "fabric", "src", "main", "java")


def statement_end(text, start):
    """Index of the ';' ending the statement that begins at `start`."""
    depth = 0
    i = start
    while i < len(text):
        char = text[i]
        if char in "([{":
            depth += 1
        elif char in ")]}":
            depth -= 1
        elif char == ";" and depth <= 0:
            return i
        elif char == '"':
            i += 1
            while i < len(text) and text[i] != '"':
                i += 2 if text[i] == "\\" else 1
        i += 1
    return -1


def fix(text):
    out = text
    added = 0
    skipped = []
    index = 0
    while True:
        found = out.find(".vertex(", index)
        if found < 0:
            break
        end = statement_end(out, found)
        if end < 0:
            break
        statement = out[found:end]
        index = end + 1
        if ".endVertex()" in statement:
            continue
        # A chain that ends in the middle of an assignment is not a finished
        # vertex; leave it for a human.
        line_start = out.rfind("\n", 0, found) + 1
        if "=" in out[line_start:found] and "==" not in out[line_start:found]:
            skipped.append(out[line_start:end].strip())
            continue
        out = out[:end] + ".endVertex()" + out[end:]
        index += len(".endVertex()")
        added += 1
    return out, added, skipped


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
    total = 0
    for path in collect(sys.argv[1:]):
        with open(path, encoding="utf-8") as handle:
            text = handle.read()
        if ".vertex(" not in text:
            continue
        result, added, skipped = fix(text)
        if added:
            io.open(path, "w", encoding="utf-8", newline="\n").write(result)
            rel = os.path.relpath(path, DST).replace(os.sep, "/")
            print("%-70s +%d" % (rel, added))
            total += added
        for statement in skipped:
            print("  review: " + statement.replace("\n", " ")[:120])
    print("%d vertices closed" % total)


if __name__ == "__main__":
    main()
