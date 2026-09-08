package dev.simulated_team.simulated.data.advancements;

import com.google.gson.JsonObject;
import net.minecraft.advancements.critereon.DeserializationContext;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Supplier;

/**
 * A trigger that simply fires. Upstream's {@code codec()} is
 * {@link #createInstance}, the 1.20.1 shape — see
 * {@link SimulatedCriterionTriggerBase}.
 */
public class SimpleSimulatedTrigger extends SimulatedCriterionTriggerBase<SimpleSimulatedTrigger.Instance> {

    public SimpleSimulatedTrigger(final ResourceLocation id) {
        super(id);
    }

    public void trigger(final ServerPlayer player) {
        super.trigger(player, null);
    }

    public Instance instance() {
        return new Instance(this.getId());
    }

    @Override
    public @NotNull Instance createInstance(final @NotNull JsonObject json,
                                            final @NotNull DeserializationContext context) {
        return this.instance();
    }

    public static class Instance extends SimulatedCriterionTriggerBase.Instance {

        public Instance(final ResourceLocation id) {
            super(id);
        }

        @Override
        protected boolean test(@Nullable final List<Supplier<Object>> suppliers) {
            return true;
        }
    }
}
