package net.cloudruleta.puerta.client;

import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.cloudruleta.puerta.PuertaBunker;

/** Antes: suscriptor del bus MOD (RegisterRenderers). Lo llama RuletaClient. */
public final class PuertaBunkerCliente {
	private PuertaBunkerCliente() {
	}

	@SuppressWarnings("deprecation")
	public static void registrar() {
		BlockEntityRendererRegistry.register(PuertaBunker.BE.get(), PuertaBunkerRenderer::new);
	}
}
