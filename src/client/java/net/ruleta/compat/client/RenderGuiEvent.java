package net.ruleta.compat.client;

import net.minecraft.client.gui.GuiGraphics;
import net.ruleta.compat.Event;

/** RenderGuiEvent de Forge: Pre al empezar Gui.render, Post al terminar. */
public abstract class RenderGuiEvent extends Event {
	private final GuiGraphics guiGraphics;
	private final float partialTick;

	protected RenderGuiEvent(GuiGraphics guiGraphics, float partialTick) {
		this.guiGraphics = guiGraphics;
		this.partialTick = partialTick;
	}

	public GuiGraphics getGuiGraphics() {
		return guiGraphics;
	}

	public float getPartialTick() {
		return partialTick;
	}

	public static final class Pre extends RenderGuiEvent {
		public Pre(GuiGraphics g, float pt) {
			super(g, pt);
		}

		/** Como en Forge: cancelarlo evita que se dibuje el HUD (y Post no se lanza). */
		@Override
		public boolean isCancelable() {
			return true;
		}
	}

	public static final class Post extends RenderGuiEvent {
		public Post(GuiGraphics g, float pt) {
			super(g, pt);
		}
	}
}
