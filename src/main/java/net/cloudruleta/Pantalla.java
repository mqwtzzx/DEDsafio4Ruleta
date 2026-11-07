package net.cloudruleta;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.UUID;

/** Aviso fijo en pantalla y temporizador que ven los jugadores (y que se reenvían al entrar). */
final class Pantalla {
	static final String REVIIL = "reviil";
	static final String NUTRIA = "nutria";
	static String aviso;
	static Component avisoTitulo;
	static Component avisoTexto;
	static String temporizador;

	private Pantalla() {
	}

	static void ponerAviso(MinecraftServer server, List<UUID> para, String color, Component titulo, Component texto) {
		ReviilPayload poner = new ReviilPayload("aviso", 1, titulo, texto);
		if (para == null) {
			aviso = color;
			avisoTitulo = titulo;
			avisoTexto = texto;
			Red.enviarATodos(server, poner);
		} else {
			for (UUID id : para) {
				ServerPlayer p = server.getPlayerList().getPlayer(id);
				if (p != null)
					Red.enviar(p, poner);
			}
		}
	}

	static void alEntrar(ServerPlayer p) {
		if (aviso != null && avisoTitulo != null)
			Red.enviar(p, new ReviilPayload("aviso", 1, avisoTitulo, avisoTexto));
	}

	static void sacarAviso(MinecraftServer server, List<UUID> para) {
		if (aviso == null)
			return;
		ReviilPayload quitar = new ReviilPayload("aviso", -1, Component.empty(), Component.empty());
		if (para == null) {
			aviso = null;
			avisoTitulo = null;
			avisoTexto = null;
			Red.enviarATodos(server, quitar);
		} else {
			for (UUID id : para) {
				ServerPlayer p = server.getPlayerList().getPlayer(id);
				if (p != null)
					Red.enviar(p, quitar);
			}
		}
	}
}
