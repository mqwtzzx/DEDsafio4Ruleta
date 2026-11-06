package net.ruleta.compat.client;

import net.ruleta.compat.Event;

/** ClientPlayerNetworkEvent de Forge (ClientPlayConnectionEvents JOIN / DISCONNECT de Fabric API). */
public abstract class ClientPlayerNetworkEvent extends Event {
	public static final class LoggingIn extends ClientPlayerNetworkEvent {
	}

	public static final class LoggingOut extends ClientPlayerNetworkEvent {
	}
}
