package net.ruleta.nuclear.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;

/**
 * Antes un SimpleChannel de Forge «nuclearcreeper:main» (protocolo "1") con un solo mensaje. En Fabric el mensaje
 * es un payload propio; el receptor del cliente lo registra ClientTotemHooks.
 */
public final class ModNetwork {
	private static final String PROTOCOL = "1";

	private ModNetwork() {
	}

	public static void register() {
		PayloadTypeRegistry.playS2C().register(TotemActivatePacket.TYPE, TotemActivatePacket.STREAM_CODEC);
	}

	/** Antes CHANNEL.send(PacketDistributor.PLAYER.with(() -> jugador), mensaje). */
	public static void enviar(ServerPlayer jugador, TotemActivatePacket mensaje) {
		if (ServerPlayNetworking.canSend(jugador, TotemActivatePacket.TYPE))
			ServerPlayNetworking.send(jugador, mensaje);
	}
}
