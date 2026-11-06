package net.ruleta.client;

import com.mojang.blaze3d.shaders.FogShape;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.level.material.FogType;
import net.ruleta.Efectos;
import net.ruleta.client.escenas.MomentoNutria;
import net.ruleta.compat.SubscribeEvent;
import net.ruleta.compat.client.ClientPlayerNetworkEvent;
import net.ruleta.compat.client.RenderGuiEvent;
import net.ruleta.compat.client.RenderGuiOverlayEvent;
import net.ruleta.compat.client.ViewportEvent;

/** «Mis Ojos»: miopía (niebla a 12 bloques) y daltonismo (post-efecto), y ocultar los corazones. */
public final class MisOjos {
	private static final ResourceLocation FILTRO = ResourceLocation.fromNamespaceAndPath("ruleta", "shaders/post/daltonismo.json");
	private static final float VISTA_MIOPE = 12.0F;
	private static final Fundido niebla = new Fundido(1.0F, 3.0F);
	private static float nieblaK;
	private static final FiltroPantalla FILTRO_DALTONICO = new FiltroPantalla(FILTRO);
	private static int ancho;
	private static int alto;
	private static boolean fallo;

	private MisOjos() {
	}

	private static boolean tiene(Holder<MobEffect> efecto) {
		LocalPlayer jugador = Minecraft.getInstance().player;
		return jugador != null && jugador.hasEffect(efecto);
	}

	private static boolean miope() {
		return tiene(Efectos.MIOPIA.holder());
	}

	private static boolean daltonico() {
		return tiene(Efectos.DALTONISMO.holder());
	}

	@SubscribeEvent
	public static void alPonerNiebla(ViewportEvent.RenderFog e) {
		if (nieblaK <= 0.0F || e.getType() != FogType.NONE)
			return;
		float lejos = e.getFarPlaneDistance() + (Math.min(e.getFarPlaneDistance(), VISTA_MIOPE) - e.getFarPlaneDistance()) * nieblaK;
		float cerca = e.getNearPlaneDistance() * (1.0F - nieblaK);
		e.setNearPlaneDistance(cerca);
		e.setFarPlaneDistance(lejos);
		e.setFogShape(FogShape.SPHERE);
		e.setCanceled(true);
	}

	@SubscribeEvent
	public static void alDibujarGui(RenderGuiEvent.Pre e) {
		Minecraft mc = Minecraft.getInstance();
		MomentoNutria.filtro(e.getPartialTick());
		nieblaK = niebla.avanzar(mc.level != null && miope());
		if (mc.level == null || !daltonico()) {
			cerrar();
			return;
		}
		if (FILTRO_DALTONICO.preparar()) {
			FILTRO_DALTONICO.fijar("Tiempo", (float) ((double) System.nanoTime() / 1.0E9D % 3600.0D));
			FILTRO_DALTONICO.fijar("Fuerza", 1.0F);
			FILTRO_DALTONICO.pasar(e.getPartialTick());
		}
	}

	@SubscribeEvent
	public static void alDibujarCapa(RenderGuiOverlayEvent.Pre e) {
		if (e.getOverlay() == RenderGuiOverlayEvent.Capa.PLAYER_HEALTH && (miope() || daltonico() || MomentosCliente.vidaOculta()))
			e.setCanceled(true);
	}

	@SubscribeEvent
	public static void alSalir(ClientPlayerNetworkEvent.LoggingOut e) {
		niebla.cero();
		nieblaK = 0.0F;
		FILTRO_DALTONICO.reiniciar();
	}

	private static void cerrar() {
		FILTRO_DALTONICO.cerrar();
	}
}
