package dev.simulated_team.simulated.data.advancements;

import com.google.common.collect.Maps;
import com.google.gson.JsonObject;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.advancements.CriterionTrigger;
import net.minecraft.advancements.CriterionTriggerInstance;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.SerializationContext;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Create's trigger base, on the 1.20.1 advancement API.
 *
 * <p>1.20.2 moved criterion triggers from JSON serialisation to codecs and
 * dropped the trigger's own id in favour of a registry key. Here a trigger still
 * carries its id and still deserialises from JSON, so
 * {@link #createInstance} replaces upstream's {@code codec()}. Everything above
 * it — how a trigger fires and how listeners are tracked — is upstream's,
 * unchanged.
 */
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public abstract class SimulatedCriterionTriggerBase<T extends SimulatedCriterionTriggerBase.Instance>
        implements CriterionTrigger<T> {

    private final ResourceLocation id;
    protected final Map<PlayerAdvancements, Set<Listener<T>>> listeners = Maps.newHashMap();

    public SimulatedCriterionTriggerBase(final ResourceLocation id) {
        this.id = id;
    }

    @Override
    public void addPlayerListener(final PlayerAdvancements pPlayerAdvancements, final Listener<T> pListener) {
        final Set<Listener<T>> playerListeners =
                this.listeners.computeIfAbsent(pPlayerAdvancements, k -> new HashSet<>());
        playerListeners.add(pListener);
    }

    @Override
    public void removePlayerListener(final PlayerAdvancements pPlayerAdvancements, final Listener<T> pListener) {
        final Set<Listener<T>> playerListeners = this.listeners.get(pPlayerAdvancements);
        if (playerListeners != null) {
            playerListeners.remove(pListener);
            if (playerListeners.isEmpty()) {
                this.listeners.remove(pPlayerAdvancements);
            }
        }
    }

    @Override
    public void removePlayerListeners(final PlayerAdvancements pPlayerAdvancements) {
        this.listeners.remove(pPlayerAdvancements);
    }

    @Override
    public ResourceLocation getId() {
        return this.id;
    }

    protected void trigger(final ServerPlayer player, @Nullable final List<Supplier<Object>> suppliers) {
        final PlayerAdvancements playerAdvancements = player.getAdvancements();
        final Set<Listener<T>> playerListeners = this.listeners.get(playerAdvancements);
        if (playerListeners != null) {
            final List<Listener<T>> list = new LinkedList<>();

            for (final Listener<T> listener : playerListeners) {
                if (listener.getTriggerInstance().test(suppliers)) {
                    list.add(listener);
                }
            }

            list.forEach(listener -> listener.run(playerAdvancements));
        }
    }

    public abstract static class Instance implements CriterionTriggerInstance {

        private final ResourceLocation id;

        public Instance(final ResourceLocation id) {
            this.id = id;
        }

        public ResourceLocation getId() {
            return this.id;
        }

        @Override
        public ResourceLocation getCriterion() {
            return this.id;
        }

        @Override
        public JsonObject serializeToJson(final SerializationContext context) {
            return new JsonObject();
        }

        /**
         * 1.20.1's {@code CriterionTriggerInstance} carries the player predicate
         * on the instance. These triggers have none — they fire for the player
         * that caused them — so it is always the empty predicate.
         */
        public ContextAwarePredicate getPlayerPredicate() {
            return ContextAwarePredicate.ANY;
        }

        protected abstract boolean test(@Nullable List<Supplier<Object>> suppliers);
    }
}
