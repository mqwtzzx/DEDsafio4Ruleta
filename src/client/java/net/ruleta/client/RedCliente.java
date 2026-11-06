package net.ruleta.client;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Envío cliente → servidor (antes Red.CANAL.sendToServer de Forge). */
public final class RedCliente {
	private RedCliente() {
	}

	public static void enviar(CustomPacketPayload mensaje) {
		// Forge exigía el canal en ambos lados para conectar; aquí se comprueba antes de enviar.
		if (ClientPlayNetworking.canSend(mensaje.type()))
			ClientPlayNetworking.send(mensaje);
	}
}
