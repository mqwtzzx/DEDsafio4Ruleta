package net.ruleta.nuclear.client;

import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.ruleta.nuclear.NuclearCreeperMod;

/** Antes: suscriptor del bus MOD (RegisterLayerDefinitions / RegisterRenderers). Lo llama RuletaClient. */
public class ClientEvents {
	public static void registrar() {
		EntityModelLayerRegistry.registerModelLayer(NuclearCreeperModel.LAYER_LOCATION, NuclearCreeperModel::createBodyLayer);
		EntityRendererRegistry.register(NuclearCreeperMod.NUCLEAR_CREEPER.get(), NuclearCreeperRenderer::new);
	}
}
