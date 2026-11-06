package net.ruleta.creeperrojo.client;

import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.ruleta.creeperrojo.CreeperRojo;

/** Antes: suscriptor del bus MOD (RegisterLayerDefinitions / RegisterRenderers). Lo llama RuletaClient. */
public final class RegistroClienteCreeperRojo {
	private RegistroClienteCreeperRojo() {
	}

	public static void registrar() {
		EntityModelLayerRegistry.registerModelLayer(CreeperRojoModel.CAPA, CreeperRojoModel::crearCapa);
		EntityRendererRegistry.register(CreeperRojo.TIPO.get(), contexto -> new CreeperRojoRenderer<>(contexto));
	}
}
