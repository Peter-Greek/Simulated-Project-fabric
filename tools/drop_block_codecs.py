#!/usr/bin/env python3
"""Remove the block/fluid codec overrides 1.20.5 introduced.

1.20.5 gave every {@code BlockBehaviour} a {@code MapCodec} so blocks could be
described in data, and made {@code codec()} abstract. 1.20.1 has no such method,
so the overrides upstream writes have nothing to override and
{@code simpleCodec} does not exist to build them with.

Nothing reads these on 1.20.1 — blocks are registered in code — so each override
is removed with its annotation.

    python tools/drop_block_codecs.py [files...]
"""

import io
import os
import re
import sys

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DST = os.path.join(REPO, "simulated", "fabric", "src", "main", "java")

# "@Override\n    public MapCodec<Foo> codec() {\n ... \n    }\n"
METHOD = re.compile(
    r"[ \t]*@Override\r?\n"
    r"[ \t]*(?:public|protected)[^\n]*\bMapCodec<[^\n]*>\s+(?:codec|getCodec|getStreamCodec)\(\)[^\n]*\{"
    r"(?:[^{}]|\{[^{}]*\})*\}\r?\n",
    re.M)


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
        if "MapCodec" not in text:
            continue
        result, count = METHOD.subn("", text)
        if not count:
            continue
        if "MapCodec" not in result:
            result = re.sub(r"^import com\.mojang\.serialization\.MapCodec;\r?\n", "",
                            result, flags=re.M)
        io.open(path, "w", encoding="utf-8", newline="\n").write(result)
        print("%-70s -%d" % (os.path.relpath(path, DST).replace(os.sep, "/"), count))
        total += count
    print("%d codec overrides removed" % total)


if __name__ == "__main__":
    main()
