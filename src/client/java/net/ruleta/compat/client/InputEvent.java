package net.ruleta.compat.client;

import net.minecraft.client.KeyMapping;
import net.minecraft.world.InteractionHand;
import net.ruleta.compat.Event;

/**
 * InputEvent.InteractionKeyMappingTriggered de Forge. Lo publica mixin.client.MinecraftMixin al principio de cada mano
 * en Minecraft.startUseItem (botón 1), como Forge; si se cancela, startUseItem termina (y la mano se balancea si
 * shouldSwingHand()).
 */
public abstract class InputEvent extends Event {
	public static final class InteractionKeyMappingTriggered extends InputEvent {
		private final int button;
		private final KeyMapping keyMapping;
		private final InteractionHand hand;
		private boolean handSwing = true;

		public InteractionKeyMappingTriggered(int button, KeyMapping keyMapping, InteractionHand hand) {
			this.button = button;
			this.keyMapping = keyMapping;
			this.hand = hand;
		}

		@Override
		public boolean isCancelable() {
			return true;
		}

		public void setSwingHand(boolean value) {
			this.handSwing = value;
		}

		public boolean shouldSwingHand() {
			return this.handSwing;
		}

		public InteractionHand getHand() {
			return this.hand;
		}

		public boolean isAttack() {
			return this.button == 0;
		}

		public boolean isUseItem() {
			return this.button == 1;
		}

		public boolean isPickBlock() {
			return this.button == 2;
		}

		public KeyMapping getKeyMapping() {
			return this.keyMapping;
		}
	}
}
