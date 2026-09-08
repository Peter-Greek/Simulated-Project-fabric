#!/usr/bin/env python3
"""Write simulated.mixins.json from the mixin classes actually present.

NeoForge decides where a mixin may apply from the mixin's own config too, but
upstream keeps one list; Fabric wants the client-only ones in a separate
`client` block, because a dedicated server never loads their targets and
listing one there is a startup warning at best and a hard failure at worst.

Side is decided from the `@Mixin` target, resolved through the file's imports:
anything under `net.minecraft.client`, or in one of the client-only packages of
Create, Ponder and Catnip listed below, is client. Run this after adding,
removing or renaming a mixin:

    python tools/gen_mixin_config.py
"""

import io
import json
import os
import re
import sys

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ROOT = os.path.join(REPO, "simulated", "fabric", "src", "main", "java",
                    "dev", "simulated_team", "simulated", "mixin")
OUT = os.path.join(REPO, "simulated", "fabric", "src", "main", "resources",
                   "simulated.mixins.json")

# Mod classes that only exist on the client. Vanilla needs no list: its client
# classes all live under net.minecraft.client.
CLIENT_HINTS = (
    "net.minecraft.client",
    "com.simibubi.create.content.equipment.extendoGrip.ExtendoGripRenderHandler",
    "com.simibubi.create.content.equipment.goggles.GoggleOverlayRenderer",
    "com.simibubi.create.content.redstone.displayLink.DisplayLinkScreen",
    "com.simibubi.create.foundation.gui",
    "com.simibubi.create.foundation.item.TooltipHelper",
    "com.simibubi.create.foundation.ponder",
    "net.createmod.ponder",
)


def targets(text):
    """Every class named by the file's @Mixin, in fully-qualified form."""
    match = re.search(r"@Mixin\((.*?)\)\s*(?:public|abstract|final|class|interface)",
                      text, re.S)
    if not match:
        match = re.search(r"@Mixin\(([^)]*)\)", text, re.S)
    blob = match.group(1) if match else ""

    found = []
    # @Mixin(targets = "a.b.C$D") is already qualified.
    found += re.findall(r'"([\w.$]+)"', blob)

    # @Mixin(Foo.class) and @Mixin(Foo.Bar.class): resolve the outermost simple
    # name through the imports, and keep any nested part attached.
    for reference in re.findall(r"([\w.]+)\.class", blob):
        parts = reference.split(".")
        outer = parts[0]
        qualified = re.search(r"^import ([\w.]*\." + outer + r");", text, re.M)
        if qualified:
            found.append(".".join([qualified.group(1)] + parts[1:]))
        elif reference.startswith("net.") or reference.startswith("com."):
            found.append(reference)
        else:
            found.append(reference)
    return found


def main():
    client, common, unresolved = [], [], []
    for root, _, names in os.walk(ROOT):
        for name in sorted(names):
            if not name.endswith(".java"):
                continue
            path = os.path.join(root, name)
            rel = os.path.relpath(path, ROOT).replace(os.sep, ".")[:-len(".java")]
            text = io.open(path, encoding="utf-8").read()

            resolved = targets(text)
            if not resolved:
                unresolved.append(rel)
            if any(target.startswith(hint) for target in resolved for hint in CLIENT_HINTS):
                client.append(rel)
            else:
                common.append(rel)

    config = {
        "required": True,
        "package": "dev.simulated_team.simulated.mixin",
        "compatibilityLevel": "JAVA_17",
        "minVersion": "0.8",
        "client": client,
        "mixins": common,
        "injectors": {"defaultRequire": 1},
    }

    io.open(OUT, "w", encoding="utf-8", newline="\n").write(json.dumps(config, indent=2) + "\n")
    print("client: %d  common: %d" % (len(client), len(common)))
    for rel in unresolved:
        print("  no @Mixin target found: " + rel, file=sys.stderr)
    return 1 if unresolved else 0


if __name__ == "__main__":
    sys.exit(main())
