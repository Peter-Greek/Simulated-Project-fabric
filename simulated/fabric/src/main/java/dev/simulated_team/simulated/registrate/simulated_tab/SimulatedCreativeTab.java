package dev.simulated_team.simulated.registrate.simulated_tab;

import dev.simulated_team.simulated.client.sections.SimulatedSection;
import dev.simulated_team.simulated.index.SimResourceManagers;
import dev.simulated_team.simulated.registrate.SimulatedRegistrate;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Section ordering, visibility and row padding for the Simulated creative tab.
 *
 * <p>Upstream also draws a banner per section over the tab background. That
 * needs {@code GuiGraphics#blitSprite} and {@code Minecraft#getGuiSprites},
 * both 1.20.2+, so the banner render is not ported here — the section grouping,
 * ordering and blank-row separation below are what the item list depends on.
 * Recorded in FABRIC_PORT_PLAN.md as a V1 deviation.
 */
public class SimulatedCreativeTab {
    private static final int ITEMS_PER_ROW = 9;

    public static final Map<ResourceLocation, Integer> SECTION_Y_VALUES = new HashMap<>();
    private static final List<Integer> SECTION_ITEM_COUNTS = new ArrayList<>();

    public static void processItems(final Consumer<ItemStack> displayItems, final Consumer<ItemStack> searchItems) {
        final Map<SimulatedSection, List<ItemStack>> sectionMap = new HashMap<>();

        for (final Supplier<Item> entry : SimulatedRegistrate.TAB_ITEMS) {
            final Item item = entry.get();
            final ItemStack stack = item.getDefaultInstance();

            final ResourceLocation sectionId = SimulatedRegistrate.sectionOf(item);
            if (sectionId == null)
                continue;

            final SimulatedSection section = SimResourceManagers.SIMULATED_SECTION.get(sectionId);
            if (section == null)
                continue;

            sectionMap.computeIfAbsent(section, s -> new LinkedList<>()).add(stack);
        }

        SECTION_Y_VALUES.clear();
        SECTION_ITEM_COUNTS.clear();

        int y = 0;
        final List<SimulatedSection> sectionKeys = sectionMap.keySet().stream().sorted().toList();
        for (final SimulatedSection key : sectionKeys) {

            int itemCount = 0;
            final List<ItemStack> sectionItems = sectionMap.get(key);

            for (ItemStack item : sectionItems) {
                item = CreativeTabItemTransforms.applyTransform(item);

                if (CreativeTabItemTransforms.VisibilityType.SEARCH_ONLY.has(item.getItem())) {
                    searchItems.accept(item);
                } else if (!CreativeTabItemTransforms.VisibilityType.INVISIBLE.has(item.getItem())) {
                    displayItems.accept(item);
                    searchItems.accept(item);
                    itemCount++;
                }
            }

            final ResourceLocation id = SimResourceManagers.SIMULATED_SECTION.getId(key);
            SECTION_Y_VALUES.put(id, y);
            SECTION_ITEM_COUNTS.add(itemCount);
            final int rowCount = -Math.floorDiv(-itemCount, ITEMS_PER_ROW);
            y += rowCount + 1;
        }
    }

    public static void padMenuItems(final List<ItemStack> items) {
        if (SECTION_ITEM_COUNTS.isEmpty())
            return;

        int expectedItemCount = 0;
        for (final int sectionItemCount : SECTION_ITEM_COUNTS) {
            expectedItemCount += sectionItemCount;
        }

        if (items.size() != expectedItemCount)
            return;

        final List<ItemStack> padded = new ArrayList<>();
        addEmptySlots(padded, ITEMS_PER_ROW);

        int itemIndex = 0;
        for (int sectionIndex = 0; sectionIndex < SECTION_ITEM_COUNTS.size(); sectionIndex++) {
            final int sectionItemCount = SECTION_ITEM_COUNTS.get(sectionIndex);
            final int nextItemIndex = itemIndex + sectionItemCount;
            padded.addAll(items.subList(itemIndex, nextItemIndex));
            itemIndex = nextItemIndex;

            if (sectionIndex < SECTION_ITEM_COUNTS.size() - 1) {
                final int slotsToFinishRow = (ITEMS_PER_ROW - sectionItemCount % ITEMS_PER_ROW) % ITEMS_PER_ROW;
                addEmptySlots(padded, slotsToFinishRow + ITEMS_PER_ROW);
            }
        }

        items.clear();
        items.addAll(padded);
    }

    private static void addEmptySlots(final List<ItemStack> items, final int count) {
        for (int i = 0; i < count; i++) {
            items.add(ItemStack.EMPTY);
        }
    }
}
