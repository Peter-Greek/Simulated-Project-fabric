package dev.simulated_team.simulated.mixin.search_alias;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.simulated_team.simulated.client.SearchAlias;
import dev.simulated_team.simulated.mixin_interface.tooltip_flag.TooltipFlagExtension;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;

/**
 * Adds Simulated's search aliases to the creative menu's search index, so
 * searching a term listed in {@code search_alias} finds the item.
 *
 * <p><b>Ported by hand, not by {@code tools/port_upstream.py}.</b> Upstream
 * mixes into {@code SessionSearchTrees}, which does not exist before 1.20.2; on
 * 1.20.1 the creative name-and-tooltip index is built by {@code Minecraft}
 * itself, in the lambda {@code createSearchTrees} hands to
 * {@code FullTextSearchTree}. That lambda has no source name to target, so this
 * targets its compiled name — {@code method_1485}, which is an intermediary
 * name and so is the same in the development and runtime namespaces.
 *
 * <p>It also does the job of upstream's second mixin, marking the flag as a
 * creative search, so {@link TooltipFlagExtension} means the same thing here as
 * it does upstream.
 */
@Mixin(Minecraft.class)
public class SessionSearchTreesMixin {

    @WrapOperation(method = "method_1485",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/item/ItemStack;getTooltipLines("
                            + "Lnet/minecraft/world/entity/player/Player;"
                            + "Lnet/minecraft/world/item/TooltipFlag;)Ljava/util/List;"))
    private static List<Component> simulated$getTooltipLines(final ItemStack instance,
                                                             @Nullable final Player player,
                                                             final TooltipFlag flag,
                                                             final Operation<List<Component>> original) {
        ((TooltipFlagExtension) (Object) flag).simulated$setCreativeSearch(true);

        final List<Component> tooltipLines = original.call(instance, player, flag);
        tooltipLines.addAll(SearchAlias.getAliases(instance).stream().map(Component::literal).toList());
        return tooltipLines;
    }
}
