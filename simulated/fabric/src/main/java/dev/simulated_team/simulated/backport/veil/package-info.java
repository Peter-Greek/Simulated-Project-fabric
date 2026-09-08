/**
 * A replacement for Veil, mirroring its package layout.
 *
 * <h2>Why this exists</h2>
 *
 * Upstream leans on Veil for registries, render events, colour, and a deferred
 * rendering stack. Veil's 1.20.1 line is an unrelated {@code 1.0.0.x} series
 * that carries 15 of the 28 types upstream imports and none of its networking,
 * and Homestead does not ship it. The recorded decision in FABRIC_PORT_PLAN.md
 * is to <em>replace</em> Veil rather than backport it, so this package supplies
 * the surface Simulated uses out of Fabric API and vanilla.
 *
 * <h2>What is real and what is not</h2>
 *
 * <ul>
 *   <li><b>Real.</b> {@code platform.registry} registers into Fabric's own
 *       registries. {@code api.client.color} is plain ARGB. {@code api.event}
 *       and {@code platform.VeilEventPlatform} run off Fabric's
 *       {@code WorldRenderEvents}. {@code api.CodecReloadListener} is a Fabric
 *       reload listener.</li>
 *   <li><b>Reduced.</b> {@code VeilRenderBridge.shaderState} hands back a
 *       vanilla shader rather than a Veil-compiled one: the geometry and colours
 *       are upstream's, the bespoke shader effects are not. Replacing those
 *       shaders is scheduled with the rest of the renderer work.</li>
 *   <li><b>Inert.</b> The framebuffer, post-processing and shader-program types
 *       exist so upstream's renderers compile; they do nothing. Every feature
 *       that depends on one is recorded in FABRIC_PORT_PLAN.md rather than left
 *       to be discovered in game.</li>
 * </ul>
 */
package dev.simulated_team.simulated.backport.veil;
