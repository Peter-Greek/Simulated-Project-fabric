package dev.simulated_team.simulated.index;

import com.simibubi.create.content.processing.sequenced.SequencedAssemblyItem;
import com.tterrag.registrate.util.entry.ItemEntry;
import dev.simulated_team.simulated.Simulated;
import dev.simulated_team.simulated.registrate.SimulatedRegistrate;
import dev.simulated_team.simulated.registrate.simulated_tab.CreativeTabItemTransforms;
import net.minecraft.world.item.Item;

public class SimItems {
    private static final SimulatedRegistrate REGISTRATE = Simulated.getRegistrate();

    public static final ItemEntry<Item> GYRO_MECHANISM = ingredient("gyroscopic_mechanism");

    public static final ItemEntry<SequencedAssemblyItem> INCOMPLETE_GYRO_MECHANISM =
            REGISTRATE.item("incomplete_gyroscopic_mechanism", SequencedAssemblyItem::new)
                    .transform(CreativeTabItemTransforms.VisibilityType.INVISIBLE.applyItem())
                    .register();

    public static final ItemEntry<Item> ENGINE_ASSEMBLY = ingredient("engine_assembly");

    public static final ItemEntry<SequencedAssemblyItem> INCOMPLETE_ENGINE_ASSEMBLY =
            REGISTRATE.item("incomplete_engine_assembly", SequencedAssemblyItem::new)
                    .transform(CreativeTabItemTransforms.VisibilityType.INVISIBLE.applyItem())
                    .register();

    private static ItemEntry<Item> ingredient(final String name) {
        return REGISTRATE.item(name, Item::new)
                .register();
    }

    public static void register() {
    }
}
