package net.cloudruleta.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.cloudruleta.ReviilPayload;
import net.cloudruleta.client.eclipse.EclipseCliente;
import net.cloudruleta.client.escenas.AnimacionMuerte;
import net.cloudruleta.client.escenas.MomentoNutria;
import net.cloudruleta.client.phora.PhoraCliente;
import net.cloudruleta.client.reviil.AnimacionReviil;
import net.cloudruleta.client.reviil.CieloReviil;
import net.cloudruleta.client.cloudruleta.RuletaHud;

/** Reparte los mensajes ReviilPayload del servidor y dibuja temporizador, aviso y coordenadas. */
public final class HudCliente {
	private static final SoundEvent CAMPANITA = SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("cloudruleta", "notificacion_mensaje"));

	private HudCliente() {
	}

	public static void recibir(ReviilPayload p) {
		boolean poner = p.valor() >= 0;
		switch (p.pieza()) {
			case "temporizador" -> TemporizadorHud.poner(p.valor());
			case "aviso" -> {
				if (poner)
					AvisoHud.poner(p.titulo(), p.aviso());
				else
					AvisoHud.quitar();
			}
			case "mensaje" -> Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(CAMPANITA, 1.0F, 1.0F));
			case "cielo" -> CieloReviil.recibir(p.valor());
			case "musica_reviil" -> CieloReviil.musicaSinCielo();
			case "reviil_activo" -> {
				LluviaRoja.poner(p.valor() == 1);
				if (p.valor() == 0)
					CieloReviil.recibir(-1);
			}
			case "animacion" -> {
				if (OpcionesCliente.animReviil)
					AnimacionReviil.empezar(p.valor() == 2);
				else if (p.valor() == 2)
					CieloReviil.temaAdelantado();
			}
			case "muerte" -> {
				if (OpcionesCliente.animMuerte) {
					ChatRojo.esperar(p.titulo());
					AnimacionMuerte.empezar(p.titulo());
				}
			}
			case "nutria" -> {
				if (OpcionesCliente.nutria)
					MomentoNutria.empezar();
			}
			case "abrir_cambios" -> Minecraft.getInstance().setScreen(new PantallaCambiosActivos());
			case "eclipse" -> EclipseCliente.recibir(p.valor(), p.titulo().getString());
			case "phora" -> PhoraCliente.recibir(p.valor());
			case "phora_voz" -> PhoraCliente.voz();
			case "impacto" -> PhoraCliente.impacto(p.valor(), p.titulo().getString());
			case "momentos" -> MomentosCliente.poner(p.titulo().getString(), p.aviso().getString());
			case "parar" -> parar();
			default -> {
			}
		}
	}

	public static void parar() {
		RuletaHud.parar();
		AnimacionReviil.parar();
		CieloReviil.pararMusicaSola();
		MomentoNutria.parar();
		TemporizadorHud.poner(-1);
		AvisoHud.quitar();
	}

	public static void limpiar() {
		AnimacionReviil.parar();
		MomentoNutria.parar();
		AnimacionMuerte.parar();
		EclipseCliente.limpiar();
		PhoraCliente.limpiar();
		MomentosCliente.limpiar();
		TemporizadorHud.limpiar();
		AvisoHud.limpiar();
	}

	static boolean tapado() {
		return RuletaHud.girando();
	}

	public static void render(GuiGraphics ctx) {
		Font tr = Minecraft.getInstance().font;
		if (Minecraft.getInstance().player != null && !Minecraft.getInstance().options.hideGui)
			AvisoHud.coordenadas(ctx, tr);
		TemporizadorHud.render(ctx, tr);
		AvisoHud.render(ctx, tr);
	}
}
