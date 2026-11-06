package com.supernova.client;

import com.mojang.brigadier.context.CommandContext;

import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.network.chat.Component;
import net.ruleta.compat.SubscribeEvent;
import net.ruleta.compat.client.RegisterClientCommandsEvent;

public class SupernovaDebugCommand {
	@SubscribeEvent
	public static void onRegister(RegisterClientCommandsEvent e) {
		e.getDispatcher().register(ClientCommandManager.literal("supernovadebug")
				.executes(SupernovaDebugCommand::status)
				.then(ClientCommandManager.literal("flip").executes(c -> toggle(c, "flip")))
				.then(ClientCommandManager.literal("nocull").executes(c -> toggle(c, "nocull")))
				.then(ClientCommandManager.literal("nodepth").executes(c -> toggle(c, "nodepth")))
				.then(ClientCommandManager.literal("magenta").executes(c -> toggle(c, "magenta")))
				.then(ClientCommandManager.literal("nochain").executes(c -> toggle(c, "nochain"))));
	}

	private static int toggle(CommandContext<FabricClientCommandSource> c, String what) {
		switch (what) {
			case "flip" -> SupernovaRenderHandler.dbgFlip = !SupernovaRenderHandler.dbgFlip;
			case "nocull" -> SupernovaRenderHandler.dbgNoCull = !SupernovaRenderHandler.dbgNoCull;
			case "nodepth" -> SupernovaRenderHandler.dbgNoDepth = !SupernovaRenderHandler.dbgNoDepth;
			case "magenta" -> SupernovaRenderHandler.dbgMagenta = !SupernovaRenderHandler.dbgMagenta;
			case "nochain" -> SupernovaRenderHandler.dbgNoChain = !SupernovaRenderHandler.dbgNoChain;
			default -> {
			}
		}
		return status(c);
	}

	private static int status(CommandContext<FabricClientCommandSource> c) {
		String msg = "flip=" + SupernovaRenderHandler.dbgFlip + "  nocull=" + SupernovaRenderHandler.dbgNoCull + "  nodepth=" + SupernovaRenderHandler.dbgNoDepth
				+ "  magenta=" + SupernovaRenderHandler.dbgMagenta + "  nochain=" + SupernovaRenderHandler.dbgNoChain;
		// antes CommandSourceStack.sendSuccess en el cliente: en Fabric el mensaje va al chat local
		c.getSource().sendFeedback(Component.literal(msg));
		return 1;
	}
}
