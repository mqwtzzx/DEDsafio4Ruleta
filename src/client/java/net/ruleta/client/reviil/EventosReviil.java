package net.ruleta.client.reviil;

import org.joml.Matrix4f;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexSorting;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.world.level.material.FogType;
import net.ruleta.client.agujero.AgujeroCliente;
import net.ruleta.compat.SubscribeEvent;
import net.ruleta.compat.client.ClientPlayerNetworkEvent;
import net.ruleta.compat.client.RenderLevelStageEvent;
import net.ruleta.compat.client.ViewportEvent;
import net.ruleta.compat.event.TickEvent;

/** Cielo del Momento Reviil: se dibuja al final del mundo y tiñe la niebla. */
public final class EventosReviil {
	private EventosReviil() {
	}

	@SubscribeEvent
	public static void alDibujarMundo(RenderLevelStageEvent e) {
		if (e.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL || !CieloReviil.visible() || AgujeroCliente.tapaCielo())
			return;

		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null || mc.level.effects().skyType() != DimensionSpecialEffects.SkyType.NORMAL)
			return;

		FogType dentro = e.getCamera().getFluidInCamera();
		if (dentro == FogType.LAVA || dentro == FogType.POWDER_SNOW)
			return;

		Camera camara = e.getCamera();
		Matrix4f vista = new Matrix4f().rotateX(camara.getXRot() * 0.017453292F).rotateY((camara.getYRot() + 180.0F) * 0.017453292F);
		Matrix4f proyeccion = new Matrix4f(e.getProjectionMatrix());
		float lejos = (float) mc.options.getEffectiveRenderDistance() * 16.0F * 4.0F;

		Matrix4f proyeccionAntes = new Matrix4f(RenderSystem.getProjectionMatrix());
		VertexSorting ordenAntes = RenderSystem.getVertexSorting();
		mc.getMainRenderTarget().bindWrite(true);
		RenderSystem.setProjectionMatrix(proyeccion, VertexSorting.DISTANCE_TO_ORIGIN);
		RenderSystem.depthFunc(515);
		try {
			CieloReviil.dibujar(vista, lejos);
		} finally {
			RenderSystem.depthMask(true);
			RenderSystem.enableDepthTest();
			RenderSystem.enableCull();
			RenderSystem.disableBlend();
			RenderSystem.defaultBlendFunc();
			RenderSystem.setProjectionMatrix(proyeccionAntes, ordenAntes);
		}
	}

	@SubscribeEvent
	public static void alColorearNiebla(ViewportEvent.ComputeFogColor e) {
		float[] c = CieloReviil.niebla(e.getRed(), e.getGreen(), e.getBlue());
		if (c != null) {
			e.setRed(c[0]);
			e.setGreen(c[1]);
			e.setBlue(c[2]);
		}
	}

	@SubscribeEvent
	public static void alTick(TickEvent.ClientTickEvent e) {
		if (e.phase == TickEvent.Phase.END)
			CieloReviil.tick();
	}

	@SubscribeEvent
	public static void alSalir(ClientPlayerNetworkEvent.LoggingOut e) {
		CieloReviil.limpiar();
	}
}
