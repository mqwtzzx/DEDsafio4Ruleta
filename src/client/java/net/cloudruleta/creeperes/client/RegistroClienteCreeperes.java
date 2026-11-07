package net.cloudruleta.creeperes.client;

import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.resources.ResourceLocation;
import net.cloudruleta.creeperes.Creeperes;
import net.cloudruleta.creeperrojo.client.CreeperRojoRenderer;

/** Antes: suscriptor del bus MOD (RegisterRenderers). Lo llama RuletaClient. */
public final class RegistroClienteCreeperes {
	private static final ResourceLocation TEXTURA_CESPED = ResourceLocation.fromNamespaceAndPath("cloudruleta", "textures/entity/creeper_cesped.png");
	private static final float ESCALA_CESPED = 0.5F;

	private RegistroClienteCreeperes() {
	}

	public static void registrar() {
		EntityRendererRegistry.register(Creeperes.CESPED.get(), contexto -> new CreeperRojoRenderer<>(contexto, TEXTURA_CESPED, ESCALA_CESPED));
	}
}
