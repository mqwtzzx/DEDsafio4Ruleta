package net.cloudruleta.compat.client;

import net.minecraft.network.chat.Component;
import net.cloudruleta.compat.Event;

/**
 * ClientChatReceivedEvent de Forge. En Fabric solo se pueden cambiar los mensajes del sistema
 * (ClientReceiveMessageEvents.MODIFY_GAME), que son los únicos que el mod modificaba (isSystem() siempre es true).
 */
public final class ClientChatReceivedEvent extends Event {
	private Component message;
	private final boolean system;
	private final boolean overlay;

	public ClientChatReceivedEvent(Component message, boolean system, boolean overlay) {
		this.message = message;
		this.system = system;
		this.overlay = overlay;
	}

	public Component getMessage() {
		return message;
	}

	public void setMessage(Component message) {
		this.message = message;
	}

	public boolean isSystem() {
		return system;
	}

	public boolean isOverlay() {
		return overlay;
	}
}
