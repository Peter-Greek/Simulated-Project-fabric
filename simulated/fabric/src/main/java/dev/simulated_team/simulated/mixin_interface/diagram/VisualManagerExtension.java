package dev.simulated_team.simulated.mixin_interface.diagram;

import dev.simulated_team.simulated.backport.physics.neoforge.mixinhelper.compatibility.flywheel.SubLevelEmbedding;
import dev.simulated_team.simulated.backport.physics.sublevel.ClientSubLevel;

public interface VisualManagerExtension {

    SubLevelEmbedding sable$getBEEmbeddingInfo(ClientSubLevel subLevel);
}
