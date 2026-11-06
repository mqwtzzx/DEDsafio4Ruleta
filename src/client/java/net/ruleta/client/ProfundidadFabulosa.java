package net.ruleta.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import net.minecraft.client.Minecraft;
import net.ruleta.compat.EventPriority;
import net.ruleta.compat.SubscribeEvent;
import net.ruleta.compat.client.RenderLevelStageEvent;

/**
 * Con gráficos «Fabulosos», guarda la profundidad tras los bloques translúcidos y la restaura al final, para que
 * lo que el mod dibuja al final del mundo se oculte bien detrás del terreno.
 */
public final class ProfundidadFabulosa {
	private static RenderTarget copia;
	private static boolean guardada;

	private ProfundidadFabulosa() {
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public static void alDibujar(RenderLevelStageEvent e) {
		if (!Minecraft.useShaderTransparency()) {
			guardada = false;
			return;
		}
		Minecraft mc = Minecraft.getInstance();
		RenderTarget principal = mc.getMainRenderTarget();
		if (e.getStage() == RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
			if (copia == null)
				copia = new TextureTarget(principal.width, principal.height, true, Minecraft.ON_OSX);
			else if (copia.width != principal.width || copia.height != principal.height)
				copia.resize(principal.width, principal.height, Minecraft.ON_OSX);
			copia.copyDepthFrom(principal);
			principal.bindWrite(false);
			guardada = true;
		} else if (e.getStage() == RenderLevelStageEvent.Stage.AFTER_LEVEL && guardada) {
			principal.copyDepthFrom(copia);
			principal.bindWrite(false);
			guardada = false;
		}
	}
}
