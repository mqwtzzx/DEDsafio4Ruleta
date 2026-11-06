package net.ruleta.mixin.client;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;
import net.ruleta.compat.Bus;
import net.ruleta.compat.client.RenderGuiEvent;
import net.ruleta.compat.client.RenderGuiOverlayEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * RenderGuiEvent.Pre / Post y RenderGuiOverlayEvent.Pre(PLAYER_HEALTH) de Forge 1.20.1.
 * <p>
 * En 1.20.1 GameRenderer solo llamaba a Gui.render si {@code !hideGui || screen != null}, y ForgeGui.render lanzaba
 * Pre (cancelable) al principio y Post al final. En 1.21.1 Gui.render se llama siempre y cada capa mira hideGui, así
 * que los eventos se lanzan con la misma condición que tenía 1.20.1. La capa PLAYER_HEALTH de Forge dibujaba los
 * corazones: en 1.21.1 es Gui.renderHearts.
 */
@Mixin(Gui.class)
public abstract class GuiMixin {
	@Shadow
	@Final
	private Minecraft minecraft;

	@Unique
	private boolean ruleta$conEventos;

	@Inject(method = "render", at = @At("HEAD"), cancellable = true)
	private void ruleta$antes(GuiGraphics guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
		ruleta$conEventos = !this.minecraft.options.hideGui || this.minecraft.screen != null;
		if (ruleta$conEventos && Bus.post(new RenderGuiEvent.Pre(guiGraphics, deltaTracker.getGameTimeDeltaPartialTick(false)))) {
			ruleta$conEventos = false;
			ci.cancel();
		}
	}

	@Inject(method = "render", at = @At("RETURN"))
	private void ruleta$despues(GuiGraphics guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
		if (ruleta$conEventos)
			Bus.post(new RenderGuiEvent.Post(guiGraphics, deltaTracker.getGameTimeDeltaPartialTick(false)));
		ruleta$conEventos = false;
	}

	@Inject(method = "renderHearts", at = @At("HEAD"), cancellable = true)
	private void ruleta$corazones(GuiGraphics guiGraphics, Player player, int x, int y, int height, int offsetHeartIndex, float maxHealth,
								  int currentHealth, int displayHealth, int absorptionAmount, boolean renderHighlight, CallbackInfo ci) {
		if (Bus.post(new RenderGuiOverlayEvent.Pre(guiGraphics, RenderGuiOverlayEvent.Capa.PLAYER_HEALTH)))
			ci.cancel();
	}
}
