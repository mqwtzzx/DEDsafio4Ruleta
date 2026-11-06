package net.ruleta;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.ruleta.agujero.AgujeroPayload;
import net.ruleta.bunker.BunkerPayload;

/**
 * Red del mod (antes un SimpleChannel de Forge, protocolo "6"). Cada mensaje es un payload de Fabric
 * con su propio identificador ruleta:<nombre>. Los mensajes al cliente se reparten en el lado cliente
 * (RuletaClient registra los receptores y fija {@link #cliente}).
 */
public final class Red {
	public static final String PROTOCOLO = "6";

	/** Lo que hace el cliente con cada mensaje recibido (lo fija el código de cliente). */
	public interface ManejadorCliente {
		void recibir(Object mensaje);
	}

	public static volatile ManejadorCliente cliente = m -> {
	};

	private Red() {}

	static void registrar() {
		// servidor → cliente
		PayloadTypeRegistry.playS2C().register(RuletaPayload.TYPE, RuletaPayload.STREAM_CODEC);
		PayloadTypeRegistry.playS2C().register(ReviilPayload.TYPE, ReviilPayload.STREAM_CODEC);
		PayloadTypeRegistry.playS2C().register(CambiosPayload.TYPE, CambiosPayload.STREAM_CODEC);
		PayloadTypeRegistry.playS2C().register(AgujeroPayload.TYPE, AgujeroPayload.STREAM_CODEC);
		PayloadTypeRegistry.playS2C().register(OndaPayload.TYPE, OndaPayload.STREAM_CODEC);
		PayloadTypeRegistry.playS2C().register(BunkerPayload.TYPE, BunkerPayload.STREAM_CODEC);
		// cliente → servidor (Fabric ejecuta los receptores en el hilo del servidor, como consumerMainThread)
		PayloadTypeRegistry.playC2S().register(LanzarPayload.TYPE, LanzarPayload.STREAM_CODEC);
		PayloadTypeRegistry.playC2S().register(MomentoPayload.TYPE, MomentoPayload.STREAM_CODEC);
		PayloadTypeRegistry.playC2S().register(DesactivarPayload.TYPE, DesactivarPayload.STREAM_CODEC);
		ServerPlayNetworking.registerGlobalReceiver(LanzarPayload.TYPE, (m, ctx) -> Ruleta.lanzar(ctx.player(), m));
		ServerPlayNetworking.registerGlobalReceiver(MomentoPayload.TYPE, (m, ctx) -> MomentosReviil.recibir(ctx.player(), m));
		ServerPlayNetworking.registerGlobalReceiver(DesactivarPayload.TYPE, (m, ctx) -> Ruleta.desactivarCambio(ctx.player(), m.id()));
	}

	public static void enviar(ServerPlayer jugador, Object mensaje) {
		if (mensaje instanceof CustomPacketPayload p && ServerPlayNetworking.canSend(jugador, p.type()))
			ServerPlayNetworking.send(jugador, p);
	}

	public static void enviarATodos(MinecraftServer server, Object mensaje) {
		if (server == null)
			return;
		for (ServerPlayer p : server.getPlayerList().getPlayers())
			enviar(p, mensaje);
	}
}
