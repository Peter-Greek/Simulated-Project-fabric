package dev.simulated_team.simulated.network.packets.linked_typewriter;

import dev.simulated_team.simulated.Simulated;
import dev.simulated_team.simulated.content.blocks.redstone.linked_typewriter.screen.LinkedTypewriterMenuCommon;
import dev.simulated_team.simulated.backport.net.ServerPacketContext;
import dev.simulated_team.simulated.backport.net.RegistryFriendlyByteBuf;
import dev.simulated_team.simulated.backport.net.StreamCodec;
import dev.simulated_team.simulated.backport.net.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import dev.simulated_team.simulated.backport.net.SimCodecs;

public record TypewriterMenuModifySlots(ItemStack first, ItemStack second) implements CustomPacketPayload {

    public static Type<TypewriterMenuModifySlots> TYPE = new Type<>(Simulated.path("entry_modify"));

    public static StreamCodec<RegistryFriendlyByteBuf, TypewriterMenuModifySlots> CODEC = StreamCodec.composite(
            SimCodecs.ITEM_STACK, TypewriterMenuModifySlots::first,
            SimCodecs.ITEM_STACK, TypewriterMenuModifySlots::second,
            TypewriterMenuModifySlots::new
    );

    public void handle(final ServerPacketContext context) {
        final ServerPlayer player = context.player();

        if (player.containerMenu instanceof final LinkedTypewriterMenuCommon menu && menu.stillValid(player)) {
            final ItemStack firstCopy = this.first.copy();
            final ItemStack secondCopy = this.second.copy();
            firstCopy.setCount(1);
            secondCopy.setCount(1);
            menu.ghostInventory.setStackInSlot(0, firstCopy);
            menu.ghostInventory.setStackInSlot(1, secondCopy);
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
