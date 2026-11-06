package net.ruleta.compat.client;

import net.minecraft.client.gui.GuiGraphics;
import net.ruleta.compat.Event;

/**
 * RenderGuiOverlayEvent.Pre de Forge. Solo se usaba la capa PLAYER_HEALTH (los corazones), que en 1.21.1
 * corresponde a Gui.renderHearts; cancelarlo oculta los corazones.
 */
public final class RenderGuiOverlayEvent {
	private RenderGuiOverlayEvent() {
	}

	public enum Capa {
		PLAYER_HEALTH
	}

	public static final class Pre extends Event {
		private final GuiGraphics guiGraphics;
		private final Capa overlay;

		public Pre(GuiGraphics guiGraphics, Capa overlay) {
			this.guiGraphics = guiGraphics;
			this.overlay = overlay;
		}

		@Override
		public boolean isCancelable() {
			return true;
		}

		public GuiGraphics getGuiGraphics() {
			return guiGraphics;
		}

		public Capa getOverlay() {
			return overlay;
		}
	}
}
