#!/usr/bin/env python3
"""Bridge upstream's split block-use methods back onto 1.20.1's single one.

1.20.5 split block interaction in two: {@code useItemOn} runs with an item in
hand and returns an {@code ItemInteractionResult}; {@code useWithoutItem} runs
empty-handed and returns an {@code InteractionResult}. 1.20.1 has one method,
{@code use}, called for both.

Rather than fold each block's two bodies into one by hand — 24 blocks, and the
two bodies often share nothing — this keeps upstream's methods exactly as
written, drops their now-wrong {@code @Override}, and adds the {@code use}
override that dispatches between them. A block that only declares one of the
two gets a bridge that calls it and falls through for the other case, which is
what vanilla does.

    python tools/bridge_block_use.py [files...]

With no arguments every ported block is scanned. Files that already carry a
bridge are left alone, so re-running is safe.
"""

import io
import os
import re
import sys

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DST = os.path.join(REPO, "simulated", "fabric", "src", "main", "java")

MARKER = "// 1.20.1 has one block-use method"

ITEM_ON = re.compile(
    r"( *)@Override\r?\n( *(?:protected|public)[^\n]*?ItemInteractionResult useItemOn\()")
WITHOUT = re.compile(
    r"( *)@Override\r?\n( *(?:protected|public)[^\n]*?InteractionResult useWithoutItem\()")

BRIDGE_BOTH = """
    {marker}. Upstream's two are kept as
    // written above and this dispatches to them: an item in hand runs the item
    // pass, and an empty hand — or an item pass that declined — runs the
    // empty-hand pass, which is the order vanilla uses.
    @Override
    public InteractionResult use(final BlockState state, final Level level, final BlockPos pos,
                                 final Player player, final InteractionHand hand,
                                 final BlockHitResult hitResult) {{
        final ItemStack stack = player.getItemInHand(hand);
        if (!stack.isEmpty()) {{
            final ItemInteractionResult itemResult =
                    this.useItemOn(stack, state, level, pos, player, hand, hitResult);
            if (!itemResult.shouldRunDefault()) {{
                return itemResult.result();
            }}
        }}

        return this.useWithoutItem(state, level, pos, player, hitResult);
    }}
"""

BRIDGE_ITEM_ONLY = """
    {marker}, so this dispatches into
    // upstream's item pass. An empty hand, or an item pass that declined, falls
    // through to the superclass as vanilla does.
    @Override
    public InteractionResult use(final BlockState state, final Level level, final BlockPos pos,
                                 final Player player, final InteractionHand hand,
                                 final BlockHitResult hitResult) {{
        final ItemStack stack = player.getItemInHand(hand);
        if (!stack.isEmpty()) {{
            final ItemInteractionResult itemResult =
                    this.useItemOn(stack, state, level, pos, player, hand, hitResult);
            if (!itemResult.shouldRunDefault()) {{
                return itemResult.result();
            }}
        }}

        return super.use(state, level, pos, player, hand, hitResult);
    }}
"""

BRIDGE_PLAIN_ONLY = """
    {marker}, so this dispatches into
    // upstream's empty-hand pass for every interaction.
    @Override
    public InteractionResult use(final BlockState state, final Level level, final BlockPos pos,
                                 final Player player, final InteractionHand hand,
                                 final BlockHitResult hitResult) {{
        return this.useWithoutItem(state, level, pos, player, hitResult);
    }}
"""

NEEDED_IMPORTS = [
    "dev.simulated_team.simulated.backport.world.ItemInteractionResult",
    "net.minecraft.core.BlockPos",
    "net.minecraft.world.InteractionHand",
    "net.minecraft.world.InteractionResult",
    "net.minecraft.world.entity.player.Player",
    "net.minecraft.world.item.ItemStack",
    "net.minecraft.world.level.Level",
    "net.minecraft.world.level.block.state.BlockState",
    "net.minecraft.world.phys.BlockHitResult",
]


def add_imports(text):
    lines = text.split("\n")
    last = max(i for i, line in enumerate(lines) if line.startswith("import "))
    for fqcn in NEEDED_IMPORTS:
        if ("import " + fqcn + ";") not in text and ("." + fqcn.rsplit(".", 1)[1]) not in "\n".join(
                line for line in lines if line.startswith("import ")):
            lines.insert(last + 1, "import " + fqcn + ";")
            text = "\n".join(lines)
            lines = text.split("\n")
            last = max(i for i, line in enumerate(lines) if line.startswith("import "))
    return "\n".join(lines)


def bridge(text):
    has_item = ITEM_ON.search(text) is not None
    has_plain = WITHOUT.search(text) is not None
    if not has_item and not has_plain:
        return None

    text = ITEM_ON.sub(lambda m: m.group(2), text)
    text = WITHOUT.sub(lambda m: m.group(2), text)

    if has_item and has_plain:
        body = BRIDGE_BOTH
    elif has_item:
        body = BRIDGE_ITEM_ONLY
    else:
        body = BRIDGE_PLAIN_ONLY

    text = add_imports(text)

    # Insert before the class's final closing brace.
    end = text.rstrip().rfind("\n}")
    return text[:end] + "\n" + body.format(marker=MARKER) + text[end:]


def collect(paths):
    if paths:
        return paths
    found = []
    for root, _, names in os.walk(DST):
        for name in sorted(names):
            if name.endswith(".java"):
                found.append(os.path.join(root, name))
    return found


def main():
    changed = 0
    for path in collect(sys.argv[1:]):
        with open(path, encoding="utf-8") as handle:
            text = handle.read()
        if MARKER in text:
            continue
        result = bridge(text)
        if result is None:
            continue
        io.open(path, "w", encoding="utf-8", newline="\n").write(result)
        print("bridged " + os.path.relpath(path, DST).replace(os.sep, "/"))
        changed += 1
    print("%d files bridged" % changed)


if __name__ == "__main__":
    main()
