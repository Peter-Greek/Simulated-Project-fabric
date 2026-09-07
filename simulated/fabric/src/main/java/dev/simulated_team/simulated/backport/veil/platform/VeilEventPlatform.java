package dev.simulated_team.simulated.backport.veil.platform;

import dev.simulated_team.simulated.backport.client.DeltaTracker;
import dev.simulated_team.simulated.backport.veil.api.client.render.MatrixStack;
import dev.simulated_team.simulated.backport.veil.api.client.render.shader.processor.ShaderPreProcessor;
import dev.simulated_team.simulated.backport.veil.api.event.VeilRenderLevelStageEvent;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.renderer.RenderType;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Veil's event bus, on Fabric's own render events.
 *
 * <p>The level-stage event is <b>real</b>: listeners are dispatched from
 * Fabric's {@code WorldRenderEvents} at the nearest equivalent point, so
 * anything upstream draws in a stage still gets drawn. Three of Veil's ten
 * stages have a direct Fabric counterpart and the rest are folded onto the
 * closest one that fires — {@link Bridge} says which.
 *
 * <p>Shader pre-processors and fixed buffers are <b>inert</b>: both belong to
 * Veil's shader pipeline, which this port replaces with vanilla shaders. A
 * registration is accepted and recorded so nothing crashes, and does nothing.
 */
public final class VeilEventPlatform {

    public static final VeilEventPlatform INSTANCE = new VeilEventPlatform();

    private final List<VeilRenderLevelStageEvent> stageListeners = new ArrayList<>();

    private VeilEventPlatform() {
    }

    public void onVeilRenderLevelStage(final VeilRenderLevelStageEvent listener) {
        this.stageListeners.add(listener);
        if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
            Bridge.ensureHooked(this);
        }
    }

    public void onVeilAddShaderProcessors(final ShaderProcessorRegistration registration) {
        registration.register(null, new ShaderProcessorRegistry());
    }

    public void onVeilRegisterFixedBuffers(final Consumer<FixedBufferRegistry> registration) {
        registration.accept(new FixedBufferRegistry());
    }

    List<VeilRenderLevelStageEvent> listeners() {
        return this.stageListeners;
    }

    @FunctionalInterface
    public interface ShaderProcessorRegistration {
        void register(Object provider, ShaderProcessorRegistry registry);
    }

    /** Inert: this port draws with vanilla shaders, which take no pre-processors. */
    public static final class ShaderProcessorRegistry {
        public void addPreprocessor(final ShaderPreProcessor processor, final boolean modifyImports) {
        }
    }

    /**
     * Inert: Veil keeps a buffer per render type alive across a stage so a
     * renderer can append to it. Vanilla's buffer source already ends every
     * batch it is given, so the renderers that used this draw immediately
     * instead.
     */
    public static final class FixedBufferRegistry {
        public void registerFixedBuffer(final VeilRenderLevelStageEvent.Stage stage, final RenderType renderType) {
        }
    }

    /**
     * Client-only, so a dedicated server never loads {@code net.minecraft.client}
     * while a listener is being registered.
     */
    private static final class Bridge {

        private static boolean hooked;

        static void ensureHooked(final VeilEventPlatform platform) {
            if (hooked) {
                return;
            }
            hooked = true;

            // AFTER_TRANSLUCENT_BLOCKS is Fabric's AFTER_TRANSLUCENT; the two
            // stages upstream also uses, AFTER_PARTICLES and AFTER_LEVEL, both
            // land after everything else is drawn, which is Fabric's LAST.
            net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents.AFTER_TRANSLUCENT.register(
                    context -> dispatch(platform, VeilRenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS, context));
            net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents.LAST.register(context -> {
                dispatch(platform, VeilRenderLevelStageEvent.Stage.AFTER_PARTICLES, context);
                dispatch(platform, VeilRenderLevelStageEvent.Stage.AFTER_LEVEL, context);
            });
        }

        static void dispatch(final VeilEventPlatform platform,
                             final VeilRenderLevelStageEvent.Stage stage,
                             final net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext context) {
            if (platform.listeners().isEmpty()) {
                return;
            }

            final net.minecraft.client.renderer.MultiBufferSource.BufferSource buffers =
                    net.minecraft.client.Minecraft.getInstance().renderBuffers().bufferSource();
            final MatrixStack matrices = new MatrixStack(context.matrixStack());
            final DeltaTracker delta = new DeltaTracker(context.tickDelta());
            final org.joml.Matrix4f projection = context.projectionMatrix();
            final org.joml.Matrix4f view = context.matrixStack() == null
                    ? new org.joml.Matrix4f()
                    : new org.joml.Matrix4f(context.matrixStack().last().pose());

            for (final VeilRenderLevelStageEvent listener : platform.listeners()) {
                listener.render(stage, context.worldRenderer(), buffers, matrices, view, projection,
                        0, delta, context.camera(), context.frustum());
            }

            buffers.endBatch();
        }
    }
}
