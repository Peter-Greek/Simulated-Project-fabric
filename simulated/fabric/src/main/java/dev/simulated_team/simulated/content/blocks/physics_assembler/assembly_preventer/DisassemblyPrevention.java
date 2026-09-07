package dev.simulated_team.simulated.content.blocks.physics_assembler.assembly_preventer;

import com.simibubi.create.content.contraptions.AssemblyException;
import dev.simulated_team.simulated.backport.physics.Sable;
import dev.simulated_team.simulated.backport.physics.api.SubLevelHelper;
import dev.simulated_team.simulated.backport.physics.sublevel.ServerSubLevel;
import dev.simulated_team.simulated.backport.physics.sublevel.SubLevel;
import dev.simulated_team.simulated.content.blocks.physics_assembler.PhysicsAssemblerBlockEntity;
import dev.simulated_team.simulated.data.SimLang;
import dev.simulated_team.simulated.mixin_interface.assembly_preventer.PrimaryAssemblerExtension;
import dev.simulated_team.simulated.service.SimConfigService;
import net.createmod.catnip.lang.LangBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

public class DisassemblyPrevention {
    private static final LangBuilder ERR = SimLang.builder().translate("prevent_disassembly");

    /**
     * Attempts to get a sublevel, and check it for a primary assembler, throwing if one is found and the given pos it not it.
     *
     * @param level   The level to check in.
     * @param toCheck The blockpos to check
     * @throws AssemblyException Whenever the given block position is not the primary assembler
     */
    public static boolean checkSubLevelForPrimary(final Level level, final BlockPos toCheck) throws AssemblyException {
        if (!SimConfigService.INSTANCE.server().assembly.primaryDisassembly.get() || (level == null & toCheck == null)) {
            return true;
        }

        final SubLevel subLevel = Sable.HELPER.getContaining(level, toCheck);
        if (subLevel instanceof final ServerSubLevel ssl) {
            final BlockPos primary = ((PrimaryAssemblerExtension) ssl).simulated$getPrimaryAssembler();

            if (primary != null) {
                if (level.getBlockEntity(primary) instanceof final PhysicsAssemblerBlockEntity psbe) {
                    if (!toCheck.equals(primary) || !psbe.isPrimaryAssembler()) { //if the checking position
                        throw new AssemblyException(ERR.component());
                    }
                }
            }
        }

        return true;
    }
}
