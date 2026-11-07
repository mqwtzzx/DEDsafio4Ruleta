package net.cloudruleta.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.cloudruleta_supernova.SupernovaNet;
import com.cloudruleta_supernova.client.ClientSetup;
import com.cloudruleta_supernova.client.ScreenEffectHandler;
import com.cloudruleta_supernova.client.SupernovaDebugCommand;
import com.cloudruleta_supernova.client.SupernovaGhosts;
import com.cloudruleta_supernova.client.SupernovaRenderHandler;
import com.cloudruleta_supernova.client.SupernovaSounds;

import java.io.IOException;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.CoreShaderRegistrationCallback;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.cloudruleta.CambiosPayload;
import net.cloudruleta.OndaPayload;
import net.cloudruleta.Red;
import net.cloudruleta.ReviilPayload;
import net.cloudruleta.RuletaPayload;
import net.cloudruleta.agujero.AgujeroNegro;
import net.cloudruleta.agujero.AgujeroPayload;
import net.cloudruleta.bunker.BunkerPayload;
import net.cloudruleta.client.agujero.AgujeroCliente;
import net.cloudruleta.client.agujero.CieloBake;
import net.cloudruleta.client.bunker.BunkerMarcador;
import net.cloudruleta.client.eclipse.EclipseCliente;
import net.cloudruleta.client.eclipse.EfectosEclipse;
import net.cloudruleta.client.escenas.AnimacionMuerte;
import net.cloudruleta.client.escenas.MomentoNutria;
import net.cloudruleta.client.linterna.LinternaCliente;
import net.cloudruleta.client.mapa.MapaCliente;
import net.cloudruleta.client.mapa.MapaDatos;
import net.cloudruleta.client.phora.PhoraCliente;
import net.cloudruleta.client.reviil.AnimacionReviil;
import net.cloudruleta.client.reviil.CieloReviil;
import net.cloudruleta.client.reviil.ComandoCieloReviil;
import net.cloudruleta.client.reviil.EventosReviil;
import net.cloudruleta.client.cloudruleta.RuletaHud;
import net.cloudruleta.compat.Bus;
import net.cloudruleta.compat.SubscribeEvent;
import net.cloudruleta.compat.client.ClientPlayerNetworkEvent;
import net.cloudruleta.compat.client.DimensionEfectos;
import net.cloudruleta.compat.client.Formatos;
import net.cloudruleta.compat.client.PuenteCliente;
import net.cloudruleta.compat.client.RenderGuiEvent;
import net.cloudruleta.compat.client.ViewportEvent;
import net.cloudruleta.compat.event.TickEvent;
import net.cloudruleta.creeperes.client.RegistroClienteCreeperes;
import net.cloudruleta.creeperrojo.client.PantallaAzul;
import net.cloudruleta.creeperrojo.client.RegistroClienteCreeperRojo;
import net.cloudruleta.herobrine.client.RegistroClienteHerobrine;
import net.cloudruleta.nuclear.client.ClientEvents;
import net.cloudruleta.nuclear.client.ClientTotemHooks;
import net.cloudruleta.nuclear.client.RadiationSoundHandler;
import net.cloudruleta.nuclear.network.TotemActivatePacket;
import net.cloudruleta.phora.PhoraMod;
import net.cloudruleta.puerta.client.PuertaBunkerCliente;

/**
 * Punto de entrada del cliente (fabric.mod.json → "client"). Reúne lo que en Forge hacían los suscriptores
 * del bus MOD del cliente (FMLClientSetupEvent, RegisterKeyMappingsEvent, RegisterDimensionSpecialEffectsEvent,
 * EntityRenderersEvent, RegisterShadersEvent y los RegisterLayerDefinitions de cada parte) y registra en el
 * {@link Bus} todas las clases que estaban suscritas al bus FORGE del cliente.
 */
public final class RuletaClient implements ClientModInitializer {
	public RuletaClient() {
	}

	@Override
	public void onInitializeClient() {
		// --- antes ModBus.alIniciarCliente (FMLClientSetupEvent) ---
		OpcionesCliente.leer();
		Red.cliente = RuletaClient::recibir;
		registrarReceptores();

		// --- antes ModBus.alRegistrarTeclas (RegisterKeyMappingsEvent) ---
		KeyBindingHelper.registerKeyBinding(AvisoHud.OCULTAR);
		KeyBindingHelper.registerKeyBinding(LanzadorRuleta.ABRIR);
		KeyBindingHelper.registerKeyBinding(MapaCliente.TECLA);

		// --- antes ModBus.alRegistrarEfectos (RegisterDimensionSpecialEffectsEvent): sustituye los efectos del Overworld.
		// DimensionRenderingRegistry de Fabric no reemplaza un efecto ya existente (putIfAbsent), por eso se usa el
		// mismo mapa que lee DimensionSpecialEffects.forType a través de un accessor.
		DimensionEfectos.poner(BuiltinDimensionTypes.OVERWORLD_EFFECTS, new EfectosEclipse());

		// --- antes ModBus.alRegistrarDibujos y AgujeroCliente.Registro (EntityRenderersEvent.RegisterRenderers) ---
		EntityRendererRegistry.register(PhoraMod.METEORITO.get(), NoopRenderer::new);
		EntityRendererRegistry.register(AgujeroNegro.TIPO.get(), NoopRenderer::new);

		// --- resto de suscriptores del bus MOD del cliente ---
		ClientSetup.registrar();
		RegistroClienteCreeperes.registrar();
		RegistroClienteCreeperRojo.registrar();
		RegistroClienteHerobrine.registrar();
		ClientEvents.registrar();
		PuertaBunkerCliente.registrar();

		// --- antes ModBus.alRegistrarShaders (RegisterShadersEvent) ---
		CoreShaderRegistrationCallback.EVENT.register(ctx -> {
			try {
				ctx.register(id("cloudruleta_hd"), DefaultVertexFormat.POSITION_TEX_COLOR, RuletaHud::shader);
				ctx.register(id("cielo_reviil"), DefaultVertexFormat.POSITION, CieloReviil::shader);
				ctx.register(id("linterna_haz"), DefaultVertexFormat.POSITION, LinternaCliente::shaderHaz);
				ctx.register(id("linterna_negro"), DefaultVertexFormat.POSITION, LinternaCliente::shaderNegro);
				ctx.register(id("nutria_arco"), DefaultVertexFormat.POSITION_TEX, MomentoNutria::shader);
				ctx.register(id("agujero_cielo"), DefaultVertexFormat.POSITION, AgujeroCliente::shader);
				ctx.register(id("agujero_objeto"), DefaultVertexFormat.POSITION_TEX, AgujeroCliente::shaderObjeto);
				ctx.register(id("agujero_bake"), DefaultVertexFormat.POSITION, CieloBake::shader);
				ctx.register(id("eclipse"), DefaultVertexFormat.POSITION, EclipseCliente::shader);
				ctx.register(id("phora_cielo"), DefaultVertexFormat.POSITION, PhoraCliente::shaderCielo);
				// 1.21.1 ya no trae DefaultVertexFormat.POSITION_COLOR_TEX: Formatos.POSITION_COLOR_TEX es el mismo formato
				ctx.register(id("phora"), Formatos.POSITION_COLOR_TEX, PhoraCliente::shader);
			} catch (IOException ex) {
				throw new RuntimeException("No se pudieron cargar los shaders de la cloudruleta", ex);
			}
		});

		// --- eventos del juego: callbacks de Fabric API → bus estilo Forge (los mixins de cliente publican el resto) ---
		PuenteCliente.registrar();

		// --- suscriptores del bus FORGE del cliente (@Mod.EventBusSubscriber(value = Dist.CLIENT)) ---
		Bus.register(Juego.class);
		Bus.register(ChatRojo.class);
		Bus.register(LluviaRoja.class);
		Bus.register(MisOjos.class);
		Bus.register(OndaExplosion.class);
		Bus.register(PantallaHud.Comando.class);
		Bus.register(ProfundidadFabulosa.class);
		Bus.register(VidaNombre.class);
		Bus.register(AgujeroCliente.class);
		Bus.register(BunkerMarcador.class);
		Bus.register(EclipseCliente.class);
		Bus.register(LinternaCliente.class);
		Bus.register(MapaCliente.class);
		Bus.register(MapaDatos.class);
		Bus.register(PhoraCliente.class);
		Bus.register(ComandoCieloReviil.class);
		Bus.register(EventosReviil.class);
		Bus.register(PantallaAzul.Control.class);
		Bus.register(RadiationSoundHandler.class);
		Bus.register(ScreenEffectHandler.class);
		Bus.register(SupernovaDebugCommand.class);
		Bus.register(SupernovaGhosts.class);
		Bus.register(SupernovaRenderHandler.class);
		Bus.register(SupernovaSounds.class);
	}

	private static ResourceLocation id(String nombre) {
		return ResourceLocation.fromNamespaceAndPath("cloudruleta", nombre);
	}

	/**
	 * Receptores de los mensajes servidor → cliente. Fabric los ejecuta en el hilo del cliente, igual que
	 * consumerMainThread / enqueueWork en Forge.
	 */
	private static void registrarReceptores() {
		ClientPlayNetworking.registerGlobalReceiver(RuletaPayload.TYPE, (m, ctx) -> Red.cliente.recibir(m));
		ClientPlayNetworking.registerGlobalReceiver(ReviilPayload.TYPE, (m, ctx) -> Red.cliente.recibir(m));
		ClientPlayNetworking.registerGlobalReceiver(CambiosPayload.TYPE, (m, ctx) -> Red.cliente.recibir(m));
		ClientPlayNetworking.registerGlobalReceiver(AgujeroPayload.TYPE, (m, ctx) -> Red.cliente.recibir(m));
		ClientPlayNetworking.registerGlobalReceiver(OndaPayload.TYPE, (m, ctx) -> Red.cliente.recibir(m));
		// en el original: DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> BunkerMarcador.recibir(m))
		ClientPlayNetworking.registerGlobalReceiver(BunkerPayload.TYPE, (m, ctx) -> BunkerMarcador.recibir(m));
		// SupernovaNet.SyncMessage.handle → SupernovaGhosts.apply(m)
		ClientPlayNetworking.registerGlobalReceiver(SupernovaNet.SyncMessage.TYPE, (m, ctx) -> SupernovaGhosts.apply(m));
		// TotemActivatePacket.handle → ClientTotemHooks.showActivation(stack)
		ClientPlayNetworking.registerGlobalReceiver(TotemActivatePacket.TYPE, (m, ctx) -> ClientTotemHooks.showActivation(m.stack()));
	}

	static void recibir(Object mensaje) {
		if (mensaje instanceof RuletaPayload p) {
			RuletaHud.empezar(p);
		} else if (mensaje instanceof ReviilPayload p) {
			HudCliente.recibir(p);
		} else if (mensaje instanceof CambiosPayload p) {
			CambiosCliente.poner(p.activos());
		} else if (mensaje instanceof AgujeroPayload p) {
			AgujeroCliente.recibir(p);
		} else if (mensaje instanceof OndaPayload p) {
			OndaExplosion.agregar(p);
		}
	}

	/** Antes RuletaClient.Juego, suscrito al bus FORGE del cliente. */
	public static final class Juego {
		private Juego() {
		}

		@SubscribeEvent
		public static void alDibujarHud(RenderGuiEvent.Post e) {
			EfectosCliente.tinte(e.getGuiGraphics(), e.getPartialTick());
			AnimacionReviil.dibujar(e.getGuiGraphics());
			MomentoNutria.dibujar(e.getGuiGraphics());
			AnimacionMuerte.dibujar(e.getGuiGraphics());
			HudCliente.render(e.getGuiGraphics());
			RuletaHud.dibujar(e.getGuiGraphics());
		}

		@SubscribeEvent
		public static void alMoverCamara(ViewportEvent.ComputeCameraAngles e) {
			EfectosCliente.camara(e);
		}

		@SubscribeEvent
		public static void alTick(TickEvent.ClientTickEvent e) {
			if (e.phase != TickEvent.Phase.END)
				return;
			while (AvisoHud.OCULTAR.consumeClick())
				AvisoHud.alternar();
			LanzadorRuleta.tecla(Minecraft.getInstance());
		}

		@SubscribeEvent
		public static void alEntrar(ClientPlayerNetworkEvent.LoggingIn e) {
			Minecraft.getInstance().execute(RuletaHud::precargar);
		}

		@SubscribeEvent
		public static void alSalir(ClientPlayerNetworkEvent.LoggingOut e) {
			RuletaHud.parar();
			CambiosCliente.poner("");
			HudCliente.limpiar();
			EfectosCliente.limpiarTinte();
		}
	}
}
