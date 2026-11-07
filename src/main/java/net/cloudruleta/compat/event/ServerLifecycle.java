package net.cloudruleta.compat.event;

import net.minecraft.server.MinecraftServer;
import net.cloudruleta.compat.Event;

/** ServerStoppingEvent / ServerStoppedEvent de Forge. */
public final class ServerLifecycle {
	private ServerLifecycle() {}

	public static final class ServerStoppingEvent extends Event {
		private final MinecraftServer server;

		public ServerStoppingEvent(MinecraftServer server) {
			this.server = server;
		}

		public MinecraftServer getServer() {
			return server;
		}
	}

	public static final class ServerStoppedEvent extends Event {
		private final MinecraftServer server;

		public ServerStoppedEvent(MinecraftServer server) {
			this.server = server;
		}

		public MinecraftServer getServer() {
			return server;
		}
	}
}
