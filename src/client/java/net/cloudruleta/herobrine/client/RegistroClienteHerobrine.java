package net.cloudruleta.herobrine.client;

import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.cloudruleta.herobrine.Herobrine;

/** Antes: suscriptor del bus MOD (RegisterRenderers). Lo llama RuletaClient. */
public final class RegistroClienteHerobrine {
	private RegistroClienteHerobrine() {
	}

	public static void registrar() {
		EntityRendererRegistry.register(Herobrine.HEROBRINE.get(), HerobrineRenderer::new);
	}
}
