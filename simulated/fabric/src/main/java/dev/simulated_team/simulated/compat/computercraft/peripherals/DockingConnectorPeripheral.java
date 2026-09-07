package dev.simulated_team.simulated.compat.computercraft.peripherals;

import dan200.computercraft.api.lua.LuaFunction;
import dev.simulated_team.simulated.backport.physics.Sable;
import dev.simulated_team.simulated.backport.physics.api.SubLevelHelper;
import dev.simulated_team.simulated.backport.physics.sublevel.SubLevel;
import dev.simulated_team.simulated.content.blocks.docking_connector.DockingConnectorBlockEntity;

public class DockingConnectorPeripheral extends SimPeripheral<DockingConnectorBlockEntity>{
    public DockingConnectorPeripheral(final DockingConnectorBlockEntity blockEntity) {
        super(blockEntity);
    }

    @Override
    public String getType() {
        return "docking_connector";
    }

    @LuaFunction
    public String getConnectedName() {
        if (this.blockEntity.otherConnectorPosition != null) {
            final SubLevel subLevel = Sable.HELPER.getContaining(this.blockEntity.getLevel(), this.blockEntity.otherConnectorPosition);
            if (subLevel != null && subLevel.getName() != null) {
                return subLevel.getName();
            }
        }

        return "";
    }
}
