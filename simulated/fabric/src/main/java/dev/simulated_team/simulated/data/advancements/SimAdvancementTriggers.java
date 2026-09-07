package dev.simulated_team.simulated.data.advancements;

import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.resources.ResourceLocation;

import java.util.LinkedList;
import java.util.List;

public class SimAdvancementTriggers {
    private static final List<SimulatedCriterionTriggerBase<?>> TRIGGERS = new LinkedList<>();

    public static SimpleSimulatedTrigger addSimple(final String modid, final String id) {
        return add(new SimpleSimulatedTrigger(new ResourceLocation(modid,id)));
    }

    private static <T extends SimulatedCriterionTriggerBase<?>> T add(final T instance) {
        TRIGGERS.add(instance);
        return instance;
    }

    /**
     * 1.20.2 moved triggers into a registry; on 1.20.1 they are registered with
     * {@link CriteriaTriggers}, which keys them by the id the trigger carries.
     */
    public static void register() {
        TRIGGERS.forEach(CriteriaTriggers::register);
    }

}