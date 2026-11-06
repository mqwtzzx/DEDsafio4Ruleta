package net.ruleta.compat.event;

import net.minecraft.server.level.ServerPlayer;
import net.ruleta.compat.Event;

/** ServerChatEvent de Forge: un jugador envía un mensaje de chat (antes de difundirlo). */
public final class ServerChatEvent extends Event {
	private final ServerPlayer player;
	private final String raw;

	public ServerChatEvent(ServerPlayer player, String raw) {
		this.player = player;
		this.raw = raw;
	}

	public ServerPlayer getPlayer() {
		return player;
	}

	public String getRawText() {
		return raw;
	}
}
