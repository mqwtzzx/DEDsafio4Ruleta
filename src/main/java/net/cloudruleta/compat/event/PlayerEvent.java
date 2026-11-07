package net.cloudruleta.compat.event;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.cloudruleta.compat.Event;

/** Eventos de jugador de Forge (entrar, salir, reaparecer, cambiar de dimensión, clonar, nombre). */
public abstract class PlayerEvent extends Event {
	private final Player player;

	protected PlayerEvent(Player player) {
		this.player = player;
	}

	public Player getEntity() {
		return player;
	}

	public static final class PlayerLoggedInEvent extends PlayerEvent {
		public PlayerLoggedInEvent(Player player) {
			super(player);
		}
	}

	public static final class PlayerLoggedOutEvent extends PlayerEvent {
		public PlayerLoggedOutEvent(Player player) {
			super(player);
		}
	}

	public static final class PlayerRespawnEvent extends PlayerEvent {
		private final boolean endConquered;

		public PlayerRespawnEvent(Player player, boolean endConquered) {
			super(player);
			this.endConquered = endConquered;
		}

		public boolean isEndConquered() {
			return endConquered;
		}
	}

	public static final class PlayerChangedDimensionEvent extends PlayerEvent {
		public PlayerChangedDimensionEvent(Player player) {
			super(player);
		}
	}

	/** Al copiar los datos del jugador muerto (o que vuelve del End) al nuevo. */
	public static final class Clone extends PlayerEvent {
		private final Player original;
		private final boolean wasDeath;

		public Clone(Player player, Player original, boolean wasDeath) {
			super(player);
			this.original = original;
			this.wasDeath = wasDeath;
		}

		public Player getOriginal() {
			return original;
		}

		public boolean isWasDeath() {
			return wasDeath;
		}
	}

	/** Nombre que se muestra del jugador (chat, lista…): se puede cambiar. */
	public static final class NameFormat extends PlayerEvent {
		private final Component username;
		private Component displayname;

		public NameFormat(Player player, Component username) {
			super(player);
			this.username = username;
			this.displayname = username;
		}

		public Component getUsername() {
			return username;
		}

		public Component getDisplayname() {
			return displayname;
		}

		public void setDisplayname(Component displayname) {
			this.displayname = displayname;
		}
	}
}
