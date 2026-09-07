package dev.simulated_team.simulated.backport.world;

import net.minecraft.world.InteractionResult;

/**
 * Stand-in for {@code net.minecraft.world.ItemInteractionResult} (1.20.5+).
 *
 * <p>1.20.5 split block interaction into an item-in-hand pass and an empty-hand
 * pass, and gave the item pass its own result type so it could say "I did
 * nothing, run the empty-hand pass". On 1.20.1 there is one {@code use} method
 * and one {@link InteractionResult}, so ported blocks keep upstream's two
 * methods and a small bridge dispatches between them — see
 * {@code tools/port_upstream.py} and the ported blocks themselves.
 */
public enum ItemInteractionResult {
    SUCCESS,
    CONSUME,
    CONSUME_PARTIAL,
    PASS_TO_DEFAULT_BLOCK_INTERACTION,
    SKIP_DEFAULT_BLOCK_INTERACTION,
    FAIL;

    public InteractionResult result() {
        return switch (this) {
            case SUCCESS -> InteractionResult.SUCCESS;
            case CONSUME -> InteractionResult.CONSUME;
            case CONSUME_PARTIAL -> InteractionResult.CONSUME_PARTIAL;
            case FAIL -> InteractionResult.FAIL;
            // Neither of these is a refusal: vanilla falls through to the
            // empty-hand pass, which on 1.20.1 the caller has already run.
            case PASS_TO_DEFAULT_BLOCK_INTERACTION, SKIP_DEFAULT_BLOCK_INTERACTION -> InteractionResult.PASS;
        };
    }

    /** Whether the empty-hand pass should still run after this result. */
    public boolean shouldRunDefault() {
        return this == PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    /**
     * Wraps a 1.20.1 result. Create's placement helpers still return one, and
     * upstream's item pass expects this type.
     */
    public static ItemInteractionResult from(final InteractionResult result) {
        return switch (result) {
            case SUCCESS -> SUCCESS;
            case CONSUME -> CONSUME;
            case CONSUME_PARTIAL -> CONSUME_PARTIAL;
            case FAIL -> FAIL;
            case PASS -> PASS_TO_DEFAULT_BLOCK_INTERACTION;
        };
    }

    public static ItemInteractionResult sidedSuccess(final boolean clientSide) {
        return clientSide ? SUCCESS : CONSUME;
    }
}
