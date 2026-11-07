package net.cloudruleta.client.reviil;

import com.mojang.brigadier.context.CommandContext;

import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.network.chat.Component;
import net.cloudruleta.client.OpcionesCliente;
import net.cloudruleta.compat.SubscribeEvent;
import net.cloudruleta.compat.client.RegisterClientCommandsEvent;

/** /cieloreviil [on|off]: ver u ocultar el cielo del Momento Reviil solo en este cliente. */
public final class ComandoCieloReviil {
	private ComandoCieloReviil() {
	}

	@SubscribeEvent
	public static void alRegistrar(RegisterClientCommandsEvent e) {
		e.getDispatcher().register(ClientCommandManager.literal("cieloreviil")
				.executes(c -> poner(c, !OpcionesCliente.reviilCielo))
				.then(ClientCommandManager.literal("on").executes(c -> poner(c, true)))
				.then(ClientCommandManager.literal("off").executes(c -> poner(c, false))));
	}

	private static int poner(CommandContext<FabricClientCommandSource> c, boolean ver) {
		OpcionesCliente.reviilCielo = ver;
		OpcionesCliente.guardar();
		String msg = ver
				? "Cielo del Momento Reviil: ACTIVADO (se ve normal)."
				: "Cielo del Momento Reviil: DESACTIVADO en tu cliente (no se dibuja el cielo, la grieta ni la cabeza). Los demás lo ven normal.";
		// antes CommandSourceStack.sendSuccess en el cliente: en Fabric el mensaje va al chat local
		c.getSource().sendFeedback(Component.literal(msg));
		return 1;
	}
}
