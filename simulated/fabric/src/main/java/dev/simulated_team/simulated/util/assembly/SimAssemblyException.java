package dev.simulated_team.simulated.util.assembly;

import com.simibubi.create.content.contraptions.AssemblyException;
import dev.simulated_team.simulated.data.SimLang;
import dev.simulated_team.simulated.service.SimConfigService;

public class SimAssemblyException {

    public static AssemblyException structureTooLarge() {
        return new AssemblyException("structureTooLarge", SimConfigService.INSTANCE.server().assembly.maxBlocksMoved.get());
    }

    public static AssemblyException couldNotAlign() {
        return new AssemblyException(SimLang.translate("gui.assembly.exception.couldNotAlign").component());
    }

    public static AssemblyException outOfWorld() {
        return new AssemblyException(SimLang.translate("gui.assembly.exception.outOfWorld").component());
    }

    public static AssemblyException tooFarFromGround() {
        return new AssemblyException(SimLang.translate("gui.assembly.exception.tooFarFromGround").component());
    }

    public static AssemblyException tooFast() {
        return new AssemblyException(SimLang.translate("gui.assembly.exception.tooFast").component());
    }

    /**
     * Port-only. Assembling into a sub-level needs the physics engine, which
     * lands in V2; until then the refusal is reported to the player the same way
     * every other assembly refusal is.
     */
    public static AssemblyException noPhysicsEngine() {
        return new AssemblyException(SimLang.translate("gui.assembly.exception.noPhysicsEngine").component());
    }

    public static AssemblyException sameSubLevel() {
        return new AssemblyException(SimLang.translate("gui.assembly.exception.sameSubLevel").component());
    }
}
