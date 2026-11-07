package net.cloudruleta.compat.event;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.cloudruleta.compat.Event;

/** RegisterCommandsEvent de Forge (lo publica CommandRegistrationCallback de Fabric API). */
public final class RegisterCommandsEvent extends Event {
	private final CommandDispatcher<CommandSourceStack> dispatcher;
	private final CommandBuildContext context;

	public RegisterCommandsEvent(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext context) {
		this.dispatcher = dispatcher;
		this.context = context;
	}

	public CommandDispatcher<CommandSourceStack> getDispatcher() {
		return dispatcher;
	}

	public CommandBuildContext getBuildContext() {
		return context;
	}
}
