#!/usr/bin/env python3
"""Strip `remap = false` from the ported mixins.

An earlier pass added `remap = false` to every mixin whose target is another
mod's class — Create, Ponder, Flywheel, Catnip, Porting Lib — on the theory that
the annotation processor fails when it cannot find that class in the mappings.
It does not: it emits `Unable to locate obfuscation mapping` as a *warning* and
carries on, and the flag is actively harmful.

The flag turns off refmap generation for the whole mixin, including the
`@At` targets inside it that *are* vanilla. Those targets then ship in the
named namespace while the game runs in intermediary, so the injection matches
in a dev run and silently fails to match in the modpack — a crash the
development environment cannot reproduce.

With the flag gone the processor writes the right thing for both halves: the
vanilla target is remapped, and the mod's own members are emitted with
intermediary descriptors.

    python tools/remap_mixins.py
"""

import io
import os
import re

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DST = os.path.join(REPO, "simulated", "fabric", "src", "main", "java",
                   "dev", "simulated_team", "simulated")

# @Mixin(value = X.class, remap = false) reads better back as @Mixin(X.class).
COLLAPSE = re.compile(r"@Mixin\(\s*value\s*=\s*([^,)]+?)\s*,\s*remap\s*=\s*false\s*\)", re.S)
DROP_TRAILING = re.compile(r",\s*remap\s*=\s*false\s*(?=[,)])")
DROP_LEADING = re.compile(r"remap\s*=\s*false\s*,\s*")


def main():
    changed = 0
    for root, _, names in os.walk(DST):
        for name in sorted(names):
            if not name.endswith(".java"):
                continue
            path = os.path.join(root, name)
            text = io.open(path, encoding="utf-8").read()
            if "remap = false" not in text:
                continue
            updated = COLLAPSE.sub(lambda m: "@Mixin(" + m.group(1) + ")", text)
            updated = DROP_TRAILING.sub("", updated)
            updated = DROP_LEADING.sub("", updated)
            if updated == text:
                print("left   " + os.path.relpath(path, DST).replace(os.sep, "/"))
                continue
            io.open(path, "w", encoding="utf-8", newline="\n").write(updated)
            print("remap  " + os.path.relpath(path, DST).replace(os.sep, "/"))
            changed += 1
    print("%d mixins un-flagged" % changed)


if __name__ == "__main__":
    main()
