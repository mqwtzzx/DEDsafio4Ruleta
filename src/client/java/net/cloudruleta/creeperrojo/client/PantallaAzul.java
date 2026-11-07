package net.cloudruleta.creeperrojo.client;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.cloudruleta.Efectos;
import net.cloudruleta.compat.SubscribeEvent;
import net.cloudruleta.compat.client.PlaySoundEvent;
import net.cloudruleta.compat.event.TickEvent;
import net.cloudruleta.creeperrojo.CreeperRojoEntity;

/** «Pantallazo azul» del creeper rojo: pantalla que no se puede cerrar mientras dura el efecto. */
public class PantallaAzul extends Screen {
	private static final int AZUL = -16746281;
	private static final int BLANCO = -1;
	private static final ResourceLocation FUENTE = ResourceLocation.fromNamespaceAndPath("cloudruleta", "bsod");
	private static final ResourceLocation FUENTE_CARA = ResourceLocation.fromNamespaceAndPath("cloudruleta", "bsod_cara");
	private static final float TAM_FUENTE = 11.0F;
	private static final float TAM_FUENTE_CARA = 40.0F;
	private static final ResourceLocation QR = ResourceLocation.fromNamespaceAndPath("cloudruleta", "textures/gui/bsod_qr.png");
	private static final int LADO_QR = 31;

	public PantallaAzul() {
		super(Component.literal("Error fatal"));
	}

	static float progreso() {
		LocalPlayer jugador = Minecraft.getInstance().player;
		if (jugador == null)
			return 1.0F;
		MobEffectInstance efecto = jugador.getEffect(Efectos.PANTALLA_AZUL.holder());
		if (efecto == null)
			return 1.0F;
		return Mth.clamp(1.0F - (float) efecto.getDuration() / 400.0F, 0.0F, 1.0F);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public boolean shouldCloseOnEsc() {
		return false;
	}

	@Override
	public boolean keyPressed(int tecla, int scan, int mods) {
		return true;
	}

	@Override
	public boolean mouseClicked(double mx, double my, int boton) {
		return true;
	}

	/** Como en el original, no llama a super.render (sin fondo ni widgets). */
	@Override
	public void render(GuiGraphics g, int mx, int my, float parcial) {
		int w = this.width;
		int h = this.height;
		g.fill(0, 0, w, h, AZUL);
		float u = Math.min((float) h, (float) w * 9.0F / 16.0F);
		float x = 0.137F * u;
		float tam = 0.033F * u;
		this.texto(g, FUENTE_CARA, TAM_FUENTE_CARA, ":(", x, 0.085F * u, 0.11F * u);
		this.texto(g, FUENTE, TAM_FUENTE, "Tu dispositivo se ha encontrado con un problema y necesita reiniciarse. Solo estamos", x, 0.326F * u, tam);
		this.texto(g, FUENTE, TAM_FUENTE, "recopilando información del error, y luego reiniciaremos por ti.", x, 0.366F * u, tam);
		int pct = Math.min(100, (int) (progreso() * 100.0F));
		this.texto(g, FUENTE, TAM_FUENTE, pct + "% completado", x, 0.435F * u, tam);
		this.texto(g, FUENTE, TAM_FUENTE, "Para más información sobre este problema y posibles soluciones, visita", x, 0.505F * u, tam);
		this.texto(g, FUENTE, TAM_FUENTE, "https://www.windows.com/stopcode", x, 0.546F * u, tam);
		int lado = Math.round(0.19F * u);
		g.blit(QR, Math.round(x), Math.round(0.61F * u), lado, lado, 0.0F, 0.0F, LADO_QR, LADO_QR, LADO_QR, LADO_QR);
		float xSoporte = x + (float) lado + 0.036F * u;
		this.texto(g, FUENTE, TAM_FUENTE, "Si llamas a soporte, dales esta información:", xSoporte, 0.635F * u, tam);
		this.texto(g, FUENTE, TAM_FUENTE, "Código de detención: CRITICAL_PROCESS_DIED", xSoporte, 0.68F * u, tam);
	}

	private void texto(GuiGraphics g, ResourceLocation fuente, float tamFuente, String linea, float x, float y, float alto) {
		float escala = alto / tamFuente;
		g.pose().pushPose();
		g.pose().translate(x, y, 0.0F);
		g.pose().scale(escala, escala, 1.0F);
		g.drawString(this.font, Component.literal(linea).withStyle(Style.EMPTY.withFont(fuente)), 0, 0, BLANCO, false);
		g.pose().popPose();
	}

	/** Abre/cierra la pantalla según el efecto y silencia el resto de sonidos mientras está. */
	public static final class Control {
		private static final SoundEvent PANTALLAZO = SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("cloudruleta", "creeper_rojo.pantallazo"));
		private static boolean silenciado;
		private static int sinExplosion;
		private static final List<double[]> ROJOS = new ArrayList<>();

		private static boolean esDeRojo(SoundInstance s) {
			for (double[] r : ROJOS) {
				double dx = s.getX() - r[0];
				double dy = s.getY() - r[1];
				double dz = s.getZ() - r[2];
				if (dx * dx + dy * dy + dz * dz < 36.0D)
					return true;
			}
			return false;
		}

		private Control() {
		}

		@SubscribeEvent
		public static void alTerminarTick(TickEvent.ClientTickEvent evento) {
			if (evento.phase != TickEvent.Phase.END)
				return;
			Minecraft mc = Minecraft.getInstance();
			if (sinExplosion > 0)
				sinExplosion--;

			if (mc.level != null) {
				long ahora = mc.level.getGameTime();
				ROJOS.removeIf(r -> ahora - (long) r[3] > 100L);
				for (Entity en : mc.level.entitiesForRendering()) {
					if (en instanceof CreeperRojoEntity c && c.getSwelling(1.0F) > 0.2F)
						ROJOS.add(new double[]{c.getX(), c.getY(), c.getZ(), (double) ahora});
				}
			}

			LocalPlayer jugador = mc.player;
			boolean activa = jugador != null && jugador.isAlive() && jugador.hasEffect(Efectos.PANTALLA_AZUL.holder());
			if (activa) {
				if (!silenciado) {
					// 1.21.1: SoundEvents.GENERIC_EXPLODE es un Holder
					mc.getSoundManager().stop(SoundEvents.GENERIC_EXPLODE.value().getLocation(), null);
					mc.getSoundManager().pause();
					silenciado = true;
					mc.getSoundManager().play(SimpleSoundInstance.forUI(PANTALLAZO, 1.0F, 1.0F));
				}
				if (!(mc.screen instanceof PantallaAzul))
					mc.setScreen(new PantallaAzul());
			} else {
				if (mc.screen instanceof PantallaAzul)
					mc.setScreen(null);
				if (silenciado) {
					silenciado = false;
					mc.getSoundManager().stop(SoundEvents.GENERIC_EXPLODE.value().getLocation(), null);
					mc.getSoundManager().resume();
					sinExplosion = 40;
				}
			}
		}

		@SubscribeEvent
		public static void alSonar(PlaySoundEvent evento) {
			if (evento.getSound() == null)
				return;
			if (!silenciado) {
				boolean explosion = evento.getSound().getLocation().equals(SoundEvents.GENERIC_EXPLODE.value().getLocation());
				if (explosion && (sinExplosion > 0 || esDeRojo(evento.getSound())))
					evento.setSound(null);
				return;
			}
			if (!evento.getSound().getLocation().equals(PANTALLAZO.getLocation()))
				evento.setSound(null);
		}
	}
}
