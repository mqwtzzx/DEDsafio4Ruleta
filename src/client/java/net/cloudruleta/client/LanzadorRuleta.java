package net.cloudruleta.client;

import java.util.Arrays;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.sounds.SoundEvents;
import net.cloudruleta.LanzarPayload;
import net.cloudruleta.Mensajes;
import net.cloudruleta.RuletaColor;
import net.cloudruleta.client.cloudruleta.RuletaHud;

/** Selector circular de las 8 cloudruletas (tecla G, solo operadores). */
public class LanzadorRuleta extends Screen {
	/** Se registra con KeyBindingHelper en RuletaClient. */
	public static final KeyMapping ABRIR = new KeyMapping("key.cloudruleta.lanzador", InputConstants.Type.KEYSYM, 71, "key.categories.cloudruleta");
	private static final float RADIO = 214.0F;
	private static final float CRECE = 1.12F;
	private static final int Y_BOTON = 50;
	private final float[] escala = new float[8];
	private final float[] luz = new float[8];
	private long ultimo = Util.getMillis();
	private float cx;
	private float cy;
	private float s = 1.0F;
	private int hover = -1;

	public LanzadorRuleta() {
		super(Component.literal("Lanzar cloudruleta"));
		Arrays.fill(this.escala, 1.0F);
	}

	public static void tecla(Minecraft mc) {
		while (ABRIR.consumeClick()) {
			if (mc.player == null || mc.screen != null)
				continue;
			if (!mc.player.hasPermissions(2)) {
				mc.player.displayClientMessage(Component.literal("El lanzador de la cloudruleta es sólo para operadores."), true);
			} else if (!servidorConMod()) {
				mc.player.displayClientMessage(Component.literal("Este servidor no tiene el mod de la cloudruleta."), true);
			} else {
				mc.setScreen(new LanzadorRuleta());
			}
		}
	}

	/** Antes Red.CANAL.isRemotePresent: en Fabric, si el servidor acepta el paquete de lanzar. */
	private static boolean servidorConMod() {
		Minecraft mc = Minecraft.getInstance();
		return mc.getConnection() != null && ClientPlayNetworking.canSend(LanzarPayload.TYPE);
	}

	@Override
	protected void init() {
		Fondo.poner();
		this.addRenderableWidget(Button.builder(Component.literal("DETENER RULETA"), b -> {
			RedCliente.enviar(new LanzarPayload(-1, 1, false, false, "", "", "", ""));
			this.onClose();
		}).bounds(this.width / 2 - 65, this.height - 50, 130, 20).build());
	}

	@Override
	public void removed() {
		Fondo.quitar();
	}

	private void calcular() {
		float libre = Math.max(60.0F, (float) (this.height - Y_BOTON) - 13.0F - 3.0F - 26.0F);
		float radio = Math.min(libre / 2.0F, (float) this.width * 0.34F * CRECE);
		this.s = radio / CRECE / RADIO;
		this.cx = (float) this.width / 2.0F;
		this.cy = 26.0F + radio;
	}

	private int elegir(double mx, double my) {
		double dx = mx - (double) this.cx;
		double dy = my - (double) this.cy;
		double d = Math.hypot(dx, dy);
		if (d >= 4.0D && d <= (double) (RADIO * this.s * CRECE)) {
			double a = Math.toDegrees(Math.atan2(dx, -dy));
			return Math.floorMod((int) Math.floor((a + 22.5D) / 45.0D), 8);
		}
		return -1;
	}

	/** Screen.render de 1.21.1 ya pinta el fondo: se deja vacío y se pinta como en 1.20.1 en render(). */
	@Override
	public void renderBackground(GuiGraphics g, int mx, int my, float pt) {
	}

	@Override
	public void render(GuiGraphics ctx, int mx, int my, float pt) {
		Fondo.oscurecer(this, ctx);
		this.calcular();
		this.hover = this.elegir((double) mx, (double) my);
		long ahora = Util.getMillis();
		float dt = Math.min(0.1F, (float) (ahora - this.ultimo) / 1000.0F);
		this.ultimo = ahora;

		for (int i = 0; i < 8; i++) {
			float objetivo = i == this.hover ? CRECE : 1.0F;
			this.escala[i] = this.escala[i] + (objetivo - this.escala[i]) * Math.min(1.0F, dt * 16.0F);
			float brillo = i == this.hover ? 1.0F : 0.0F;
			this.luz[i] = this.luz[i] + (brillo - this.luz[i]) * Math.min(1.0F, dt * 12.0F);
		}

		PoseStack pose = ctx.pose();
		MultiBufferSource.BufferSource vcp = ctx.bufferSource();

		// primero las casillas en reposo y después las resaltadas, para que queden por encima
		for (int pase = 0; pase < 2; pase++) {
			for (int i = 0; i < 8; i++) {
				boolean resaltada = this.escala[i] > 1.02F || this.luz[i] > 0.01F;
				if (resaltada ? pase != 1 : pase != 0)
					continue;
				pose.pushPose();
				pose.translate(this.cx, this.cy, 0.0F);
				pose.scale(this.s, this.s, 1.0F);
				pose.mulPose(Axis.ZP.rotationDegrees((float) i * 45.0F));
				pose.scale(this.escala[i], this.escala[i], 1.0F);
				RuletaHud.casillaLanzador(pose.last().pose(), i, this.luz[i]);
				pose.popPose();
			}
		}

		ctx.flush();
		ctx.drawCenteredString(this.font, Component.literal("Elige una cloudruleta").withStyle(ChatFormatting.BOLD), this.width / 2, 12, 16777215);
		if (this.hover >= 0) {
			RuletaColor c = RuletaColor.byIndex(this.hover);
			String nombre = Mensajes.encabezado(c);
			nombre = nombre.endsWith(":") ? nombre.substring(0, nombre.length() - 1) : nombre;
			Component etiqueta = Component.literal(nombre).withStyle(Style.EMPTY.withBold(true).withColor(c.rgb));
			ctx.drawCenteredString(this.font, etiqueta, this.width / 2, this.height - Y_BOTON - 13, 16777215);
		}

		super.render(ctx, mx, my, pt);
	}

	@Override
	public boolean mouseClicked(double mx, double my, int boton) {
		if (super.mouseClicked(mx, my, boton))
			return true;
		this.calcular();
		int i = this.elegir(mx, my);
		if (boton == 0 && i >= 0) {
			Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
			Minecraft.getInstance().setScreen(new LanzadorFormulario(RuletaColor.byIndex(i)));
			return true;
		}
		return false;
	}

	@Override
	public boolean keyPressed(int key, int scan, int mods) {
		if (ABRIR.matches(key, scan)) {
			this.onClose();
			return true;
		}
		return super.keyPressed(key, scan, mods);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
