package dev.simulated_team.simulated.mixin_interface;

import net.minecraft.client.renderer.texture.SpriteTicker;

/**
 * Reaches the ticker a sprite created, so an animated creative-tab icon can be
 * paused.
 *
 * <p>Upstream types this as {@code SpriteContents.Ticker}. That class is private
 * on this stack's mappings and cannot be named from source, so it is carried as
 * the {@link SpriteTicker} it implements — the same object, and every use here
 * goes through {@link dev.simulated_team.simulated.mixin_interface.TickerExtension}
 * anyway.
 */
public interface SpriteContentsExtension {

    SpriteTicker simulated$getTicker();

    void simulated$setTicker(SpriteTicker ticker);
}
