package dev.simulated_team.simulated.backport.physics.api.command;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import dev.simulated_team.simulated.backport.physics.sublevel.ServerSubLevel;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;

/**
 * The {@code <sub-level>} command argument. There are no sub-levels to name, so
 * parsing always fails with a plain message rather than pretending to resolve
 * one.
 */
public class SubLevelArgumentType implements ArgumentType<String> {

    private static final SimpleCommandExceptionType NO_SUB_LEVELS =
            new SimpleCommandExceptionType(Component.literal("No sub-levels exist on this build"));

    public static SubLevelArgumentType subLevel() {
        return new SubLevelArgumentType();
    }

    public static SubLevelArgumentType subLevels() {
        return new SubLevelArgumentType();
    }

    public static ServerSubLevel getSubLevel(final CommandContext<CommandSourceStack> context, final String name)
            throws CommandSyntaxException {
        throw NO_SUB_LEVELS.create();
    }

    public static java.util.Collection<ServerSubLevel> getSubLevels(
            final CommandContext<CommandSourceStack> context, final String name) throws CommandSyntaxException {
        throw NO_SUB_LEVELS.create();
    }

    @Override
    public String parse(final StringReader reader) throws CommandSyntaxException {
        throw NO_SUB_LEVELS.createWithContext(reader);
    }
}
