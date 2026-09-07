#!/usr/bin/env python3
"""Copy upstream 1.21.1 sources into the Fabric 1.20.1 module, mechanically.

The port keeps upstream's package names, so a ported file is the upstream file
with a fixed set of substitutions applied. Doing those by script rather than by
hand keeps them consistent across ~400 files and makes it obvious, per file,
what still needed a human.

    python tools/port_upstream.py <path under simulated/common/src/main/java> ...

Paths may be files or directories, given relative to that source root or to the
repository. Existing ported files are left alone unless --force is passed, so a
re-run never clobbers hand-fixed work.
"""

import argparse
import os
import re
import sys

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC = os.path.join(REPO, "simulated", "common", "src", "main", "java")
DST = os.path.join(REPO, "simulated", "fabric", "src", "main", "java")

BACKPORT = "dev.simulated_team.simulated.backport.net"

# Sable is not on this stack until V2, and Veil's non-networking API is being
# replaced rather than backported. Both are mirrored package-for-package under
# backport/, so porting them is a prefix swap and V2 undoes it the same way.
PREFIX_MAP = [
    ("dev.ryanhcode.sable.", "dev.simulated_team.simulated.backport.physics."),
    ("foundry.veil.", "dev.simulated_team.simulated.backport.veil."),
]

# Whole-import replacements: the type exists here under a different name.
IMPORT_MAP = {
    "net.minecraft.network.codec.StreamCodec": BACKPORT + ".StreamCodec",
    "net.minecraft.network.codec.ByteBufCodecs": BACKPORT + ".ByteBufCodecs",
    "net.minecraft.network.RegistryFriendlyByteBuf": BACKPORT + ".RegistryFriendlyByteBuf",
    "net.minecraft.network.protocol.common.custom.CustomPacketPayload":
        BACKPORT + ".CustomPacketPayload",
    "foundry.veil.api.network.VeilPacketManager": BACKPORT + ".VeilPacketManager",
    "foundry.veil.api.network.handler.PacketContext": BACKPORT + ".PacketContext",
    "foundry.veil.api.network.handler.ServerPacketContext": BACKPORT + ".ServerPacketContext",
    "foundry.veil.api.network.handler.ClientPacketContext": BACKPORT + ".ClientPacketContext",
    "net.minecraft.world.ItemInteractionResult":
        "dev.simulated_team.simulated.backport.world.ItemInteractionResult",
    "net.minecraft.client.DeltaTracker": "dev.simulated_team.simulated.backport.client.DeltaTracker",
    "net.neoforged.neoforge.common.ModConfigSpec": "net.minecraftforge.common.ForgeConfigSpec",
    "org.jspecify.annotations.NonNull": "org.jetbrains.annotations.NotNull",
    "org.jspecify.annotations.Nullable": "org.jetbrains.annotations.Nullable",
}

# Plain text replacements applied to the whole file.
TEXT_SUBS = [
    (r"\bModConfigSpec\b", "ForgeConfigSpec"),
    (r"@NonNull\b", "@NotNull"),
    (r"\bResourceLocation\.fromNamespaceAndPath\(", "new ResourceLocation("),
    (r"\bResourceLocation\.withDefaultNamespace\(", "new ResourceLocation("),
]

# Stream codecs vanilla hangs off its own types from 1.20.5 onward. A shim
# cannot add a field to a vanilla class, so these point at SimCodecs instead.
CODEC_FIELDS = {
    r"\bBlockPos\.STREAM_CODEC\b": "SimCodecs.BLOCK_POS",
    r"\bUUIDUtil\.STREAM_CODEC\b": "SimCodecs.UUID_CODEC",
    r"\bDirection\.STREAM_CODEC\b": "SimCodecs.DIRECTION",
    r"\bResourceLocation\.STREAM_CODEC\b": "SimCodecs.RESOURCE_LOCATION",
}

# Files whose Fabric copy was rewritten by hand far enough that re-porting them
# would throw the rewrite away. --force skips these; delete the entry, and the
# note in the file's javadoc, if upstream ever converges on 1.20.1's shapes.
HAND_PORTED = [
    "dev/simulated_team/simulated/mixin/end_sea/LevelRendererMixin.java",
    "dev/simulated_team/simulated/mixin/hold_interaction/GoggleOverlayRendererMixin.java",
    "dev/simulated_team/simulated/mixin/lodestone_compat/CompassItemMixin.java",
    "dev/simulated_team/simulated/mixin/search_alias/SessionSearchTreesMixin.java",
    "dev/simulated_team/simulated/mixin/world_presets/PrimaryLevelDataMixin.java",
]

# Things this script cannot fix, reported per file so nothing is ported blind.
MANUAL = [
    (r"\bnet\.neoforged\b", "NeoForge API"),
    (r"\bDataComponents\b|\bSimDataComponents\b", "data components (1.20.5+)"),
    (r"\bItemInteractionResult\b", "ItemInteractionResult (1.20.5+)"),
    (r"\buseWithoutItem\b", "useWithoutItem (1.20.5+)"),
    (r"\.getFirst\(\)|\.getLast\(\)", "SequencedCollection (Java 21)"),
    (r"\bAdvancementHolder\b|\bAdvancementType\b", "advancement API (1.20.2+)"),
    (r"\bDeltaTracker\b", "DeltaTracker (1.21)"),
    (r"\bHolderLookup\.Provider\b", "HolderLookup.Provider in registry paths"),
    (r"\bdev\.ryanhcode\.sable\b", "Sable"),
    (r"\bfoundry\.veil\b", "Veil"),
]


def add_import(text, fqcn):
    if "import " + fqcn + ";" in text:
        return text
    lines = text.split("\n")
    last = max(i for i, line in enumerate(lines) if line.startswith("import "))
    lines.insert(last + 1, "import " + fqcn + ";")
    return "\n".join(lines)


def port(text):
    for old, new in IMPORT_MAP.items():
        text = text.replace("import " + old + ";", "import " + new + ";")
    for old, new in PREFIX_MAP:
        text = re.sub(r"^import (static )?" + re.escape(old),
                      lambda m: "import " + (m.group(1) or "") + new, text, flags=re.M)
    for pattern, new in TEXT_SUBS:
        text = re.sub(pattern, new, text)
    for pattern, new in CODEC_FIELDS.items():
        if re.search(pattern, text):
            text = re.sub(pattern, new, text)
            text = add_import(text, BACKPORT + ".SimCodecs")
    return text


def review(text):
    return sorted({label for pattern, label in MANUAL if re.search(pattern, text)})


def resolve(arg):
    for base in (SRC, REPO, os.getcwd()):
        candidate = os.path.normpath(os.path.join(base, arg))
        if os.path.exists(candidate):
            return candidate
    sys.exit("no such path: " + arg)


def collect(path):
    if os.path.isfile(path):
        return [path]
    found = []
    for root, _, names in os.walk(path):
        found += [os.path.join(root, n) for n in sorted(names) if n.endswith(".java")]
    return found


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("paths", nargs="+")
    parser.add_argument("--force", action="store_true",
                        help="overwrite files already present in the Fabric module")
    parser.add_argument("--skip-physics", action="store_true",
                        help="leave files that import Sable or Veil for V2")
    args = parser.parse_args()

    for arg in args.paths:
        for source in collect(resolve(arg)):
            rel = os.path.relpath(source, SRC)
            target = os.path.join(DST, rel)
            slug = rel.replace(os.sep, "/")
            if os.path.exists(target) and not args.force:
                print("skip   " + slug + "  (already ported)")
                continue
            if slug in HAND_PORTED:
                print("keep   " + slug + "  (hand-ported; see its javadoc)")
                continue
            with open(source, encoding="utf-8") as handle:
                text = handle.read()
            # Veil's networking is shimmed above, so only its other packages,
            # and Sable itself, mark a file as needing the physics stack.
            if args.skip_physics and re.search(
                    r"^import (dev\.ryanhcode\.sable|foundry\.veil"
                    r"(?!\.api\.network))", text, re.M):
                print("defer  " + rel.replace(os.sep, "/") + "  (physics; V2)")
                continue
            ported = port(text)
            os.makedirs(os.path.dirname(target), exist_ok=True)
            with open(target, "w", encoding="utf-8", newline="\n") as handle:
                handle.write(ported)
            notes = review(ported)
            flag = "  <- " + ", ".join(notes) if notes else ""
            print("port   " + rel.replace(os.sep, "/") + flag)


if __name__ == "__main__":
    main()
