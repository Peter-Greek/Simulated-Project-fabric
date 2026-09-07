package dev.simulated_team.simulated.fabric.service;

import dev.simulated_team.simulated.content.blocks.redstone.linked_typewriter.LinkedTypewriterBlockEntity;
import dev.simulated_team.simulated.content.blocks.redstone.linked_typewriter.screen.LinkedTypewriterMenuCommon;
import dev.simulated_team.simulated.content.linked_typewriter.LinkedTypewriterMenuImpl;
import dev.simulated_team.simulated.service.SimMenuService;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;

import java.util.function.Consumer;

public class FabricSimMenuService implements SimMenuService {

    @Override
    @SuppressWarnings("unchecked")
    public <T extends LinkedTypewriterMenuCommon> T getLoaderLinkedTypewriter(
            final MenuType<?> type, final int id, final Inventory inv, final FriendlyByteBuf extraData) {
        return (T) new LinkedTypewriterMenuImpl(type, id, inv, extraData);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends LinkedTypewriterMenuCommon> T getLoaderLinkedTypewriter(
            final MenuType<?> type, final int id, final Inventory inv, final LinkedTypewriterBlockEntity be) {
        return (T) new LinkedTypewriterMenuImpl(type, id, inv, be);
    }

    /**
     * NeoForge's {@code openMenu} takes the extra-data writer directly; on Fabric
     * the provider carries it, so the provider is wrapped here.
     */
    @Override
    public void openScreen(final ServerPlayer player, final MenuProvider factory,
                           final Consumer<FriendlyByteBuf> extraDataWriter) {
        player.openMenu(new ExtendedScreenHandlerFactory() {
            @Override
            public void writeScreenOpeningData(final ServerPlayer serverPlayer, final FriendlyByteBuf buf) {
                extraDataWriter.accept(buf);
            }

            @Override
            public Component getDisplayName() {
                return factory.getDisplayName();
            }

            @Override
            public AbstractContainerMenu createMenu(final int id, final Inventory inventory, final Player menuPlayer) {
                return factory.createMenu(id, inventory, menuPlayer);
            }
        });
    }
}
