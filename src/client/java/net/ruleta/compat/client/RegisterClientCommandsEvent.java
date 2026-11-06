package net.ruleta.compat.client;

import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.commands.CommandBuildContext;
import net.ruleta.compat.Event;

/**
 * RegisterClientCommandsEvent de Forge (ClientCommandRegistrationCallback de Fabric API). En Fabric los comandos
 * de cliente usan FabricClientCommandSource y ClientCommandManager.literal/argument en lugar de Commands.
 */
public final class RegisterClientCommandsEvent extends Event {
	private final CommandDispatcher<FabricClientCommandSource> dispatcher;
	private final CommandBuildContext context;

	public RegisterClientCommandsEvent(CommandDispatcher<FabricClientCommandSource> dispatcher, CommandBuildContext context) {
		this.dispatcher = dispatcher;
		this.context = context;
	}

	public CommandDispatcher<FabricClientCommandSource> getDispatcher() {
		return dispatcher;
	}

	public CommandBuildContext getBuildContext() {
		return context;
	}
}
