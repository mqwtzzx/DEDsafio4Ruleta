package net.cloudruleta.compat.event;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Player;
import net.cloudruleta.compat.Event;

/** Ticks de servidor, jugador y cliente (como TickEvent de Forge): se publican las fases START y END. */
public abstract class TickEvent extends Event {
	public enum Phase {
		START, END
	}

	public final Phase phase;

	protected TickEvent(Phase phase) {
		this.phase = phase;
	}

	public static final class ServerTickEvent extends TickEvent {
		private final MinecraftServer server;

		public ServerTickEvent(Phase phase, MinecraftServer server) {
			super(phase);
			this.server = server;
		}

		public MinecraftServer getServer() {
			return server;
		}
	}

	public static final class PlayerTickEvent extends TickEvent {
		public final Player player;

		public PlayerTickEvent(Phase phase, Player player) {
			super(phase);
			this.player = player;
		}
	}

	public static final class ClientTickEvent extends TickEvent {
		public ClientTickEvent(Phase phase) {
			super(phase);
		}
	}
}
