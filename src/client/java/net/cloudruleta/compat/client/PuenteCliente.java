package net.cloudruleta.compat.client;

import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientChunkEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.Minecraft;
import net.cloudruleta.compat.Bus;
import net.cloudruleta.compat.event.TickEvent;

/**
 * Lado cliente del puente entre Fabric API y el bus de eventos estilo Forge. Lo que Fabric API no ofrece
 * (etapas del dibujo del mundo, cámara, niebla, HUD, nombres, clic de usar, sonidos, mapa de luz, cielo y nubes)
 * lo publican los mixins de net.cloudruleta.mixin.client en los mismos puntos donde Forge 1.20.1 lanzaba el evento.
 */
public final class PuenteCliente {
	private PuenteCliente() {
	}

	public static void registrar() {
		// TickEvent.ClientTickEvent: START al empezar Minecraft.tick y END al acabar (igual que Forge)
		ClientTickEvents.START_CLIENT_TICK.register(mc -> Bus.post(new TickEvent.ClientTickEvent(TickEvent.Phase.START)));
		ClientTickEvents.END_CLIENT_TICK.register(mc -> Bus.post(new TickEvent.ClientTickEvent(TickEvent.Phase.END)));

		// ClientPlayerNetworkEvent.LoggingIn: al terminar el inicio de sesión en el mundo (jugador ya creado)
		ClientPlayConnectionEvents.JOIN.register((handler, sender, mc) -> Bus.post(new ClientPlayerNetworkEvent.LoggingIn()));
		// ClientPlayerNetworkEvent.LoggingOut: al desconectarse. Los manejadores solo limpian estado del cliente;
		// se ejecutan en el hilo del cliente por si Fabric avisa desde el hilo de red.
		ClientPlayConnectionEvents.DISCONNECT.register((handler, mc) -> enHiloCliente(mc, () -> Bus.post(new ClientPlayerNetworkEvent.LoggingOut())));

		// ClientChatReceivedEvent: el mod solo cambiaba mensajes del sistema (muertes, entradas y salidas)
		ClientReceiveMessageEvents.MODIFY_GAME.register((mensaje, encima) -> {
			ClientChatReceivedEvent e = new ClientChatReceivedEvent(mensaje, true, encima);
			Bus.post(e);
			return e.getMessage() != null ? e.getMessage() : mensaje;
		});

		// RegisterClientCommandsEvent: comandos que solo existen en el cliente
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, contexto) -> Bus.post(new RegisterClientCommandsEvent(dispatcher, contexto)));

		// ChunkEvent.Load (solo los del nivel del cliente)
		ClientChunkEvents.CHUNK_LOAD.register((nivel, chunk) -> Bus.post(new ChunkEvent.Load(nivel, chunk)));
	}

	private static void enHiloCliente(Minecraft mc, Runnable r) {
		if (mc.isSameThread())
			r.run();
		else
			mc.execute(r);
	}
}
