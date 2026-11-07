package net.cloudruleta;

import net.fabricmc.api.ModInitializer;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.AABB;
import net.cloudruleta.agujero.AgujeroNegro;
import net.cloudruleta.agujero.ComandoAgujero;
import net.cloudruleta.bunker.Bunkers;
import net.cloudruleta.compat.Bus;
import net.cloudruleta.compat.EventPriority;
import net.cloudruleta.compat.Puente;
import net.cloudruleta.compat.SubscribeEvent;
import net.cloudruleta.compat.event.EntityEvents;
import net.cloudruleta.compat.event.LivingEvents;
import net.cloudruleta.compat.event.PlayerEvent;
import net.cloudruleta.compat.event.RegisterCommandsEvent;
import net.cloudruleta.compat.event.ServerChatEvent;
import net.cloudruleta.compat.event.ServerLifecycle.ServerStoppedEvent;
import net.cloudruleta.compat.event.TickEvent;
import net.cloudruleta.creeperes.Creeperes;
import net.cloudruleta.creeperes.MobsNuevos;
import net.cloudruleta.creeperrojo.CreeperRojo;
import net.cloudruleta.herobrine.Herobrine;
import net.cloudruleta.herobrine.HerobrineAparicion;
import net.cloudruleta.nuclear.HeartEvents;
import net.cloudruleta.nuclear.ModEvents;
import net.cloudruleta.nuclear.NuclearCreeperMod;
import net.cloudruleta.nuclear.NuclearEvents;
import net.cloudruleta.nuclear.TotemEvents;
import net.cloudruleta.objetos.Luces;
import net.cloudruleta.objetos.Objetos;
import net.cloudruleta.phora.MomentoPhora;
import net.cloudruleta.phora.PhoraMod;
import net.cloudruleta.puerta.PuertaBunker;
import com.cloudruleta_supernova.SupernovaAjustes;
import com.cloudruleta_supernova.SupernovaCommand;
import com.cloudruleta_supernova.SupernovaEvents;
import com.cloudruleta_supernova.SupernovaMod;
import com.cloudruleta_supernova.SupernovaRomper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Clase principal del mod (antes @Mod("cloudruleta") de Forge). Registra todo al iniciar y engancha los
 * manejadores de eventos originales al bus del puente Forge → Fabric.
 */
public class Ruleta implements ModInitializer {
	public static final String MOD_ID = "cloudruleta";
	public static final int PERMISSION_LEVEL = 2;
	static final String TEXTO_PHORA = "Los phoras han llegado, ponte a salvo!.";
	private static final int COLOR_NOMBRE = 43520;
	private static final Map<ServerPlayer, Long> CHAT_VERDE = new ConcurrentHashMap<>();
	private static final long TICKS_CHAT_VERDE = 10L;
	private static long tickChat;
	private static final Map<UUID, Map<Item, Integer>> INVENTARIO_ANTES = new HashMap<>();
	private static final Map<UUID, Long> ULTIMO_AVISO_TOTEM = new HashMap<>();
	public static final Logger LOG = LoggerFactory.getLogger(MOD_ID);
	static final int TICKS_RULETA = Math.round(286.0F);
	private static final float DANO_MAGMA = 1.0E8F;
	private static DificultadDatos cambios;
	private static final TextColor COLOR_TOTEM = TextColor.fromRgb(16777045);

	public static ResourceLocation id(String path) {
		return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
	}

	@Override
	public void onInitialize() {
		// registros (en Forge: DeferredRegister + bus del mod)
		Efectos.init();
		NuclearCreeperMod.init();
		SupernovaMod.init();
		PhoraMod.init();
		PuertaBunker.init();
		PestanaRuleta.init();
		CreeperRojo.init();
		Creeperes.init();
		AgujeroNegro.init();
		Herobrine.init();
		Objetos.init();
		Red.registrar();
		// manejadores (en Forge: MinecraftForge.EVENT_BUS.register y @Mod.EventBusSubscriber)
		Bus.register(Eventos.class);
		Bus.register(CambiosEfectos.class);
		Bus.addListener(RegisterCommandsEvent.class, Ruleta::alRegistrarComandos);
		for (Class<?> c : List.of(Muertes.class, CreeperRojo.Eventos.class, Luces.class, MobsNuevos.class, TotemEvents.class, HeartEvents.class,
				NuclearEvents.class, Bunkers.class, MomentosBunker.class, MomentosReviil.class, Sanciones.class, OndasExplosion.class,
				ComandoTemporizador.class, ComandoEclipse.class, HerobrineAparicion.class, MomentoPhora.class, ComandoAgujero.class,
				SupernovaAjustes.class, SupernovaRomper.class, SupernovaEvents.class, SupernovaCommand.class))
			Bus.register(c);
		ModEvents.registrar();
		// eventos de Fabric API y mixins → bus
		Puente.registrarComun();
	}

	public static void girar(MinecraftServer server, RuletaColor color, String quien, boolean mini) {
		girar(server, color, quien, mini, null);
	}

	static void girar(MinecraftServer server, RuletaColor color, String quien, boolean mini, List<ServerPlayer> destino) {
		ThreadLocalRandom rnd = ThreadLocalRandom.current();
		float startAngle = rnd.nextFloat() * 360.0F;
		float offset = (rnd.nextFloat() * 2.0F - 1.0F) * 15.0F;
		RuletaPayload p = new RuletaPayload(color.ordinal(), startAngle, 0, offset, quien, mini);
		if (destino == null)
			Red.enviarATodos(server, p);
		else
			for (ServerPlayer j : destino)
				Red.enviar(j, p);
	}

	private static String limpio(String s) {
		if (s == null)
			return null;
		String r = s.replace("\r", "").trim();
		return r.isEmpty() ? null : r;
	}

	/** Lo que pide el formulario del lanzador (cliente → servidor). */
	public static void lanzar(ServerPlayer jugador, LanzarPayload m) {
		if (jugador == null || m == null)
			return;
		if (!jugador.hasPermissions(PERMISSION_LEVEL)) {
			jugador.sendSystemMessage(Component.literal("Solo los operadores pueden lanzar la cloudruleta."));
			return;
		}
		MinecraftServer server = jugador.getServer();
		if (server == null)
			return;
		if (m.color() == -1) {
			parar(server);
			jugador.sendSystemMessage(Component.literal("Ruleta parada."));
			return;
		}
		if (m.color() < 0 || m.color() >= RuletaColor.VALUES.length)
			return;
		RuletaColor color = RuletaColor.byIndex(m.color());
		if (Mensajes.ocupado()) {
			jugador.sendSystemMessage(Component.literal(Mensajes.nutriaEnCurso()
					? "Hay un Momento Nutria en proceso: espera a que termine o pulsa DETENER RULETA."
					: "Ya hay una cloudruleta girando: espera a que termine o pulsa DETENER RULETA.").withStyle(ChatFormatting.RED));
			return;
		}
		if (color == RuletaColor.ROSA && MomentoReviil.enMarcha() && (MomentoReviil.enMarchaConCielo() || !MomentosReviil.soloLuna())) {
			jugador.sendSystemMessage(Component.literal("No se puede lanzar el Momento Nutria con un Momento Reviil en marcha (solo con la Maldición de la Luna).").withStyle(ChatFormatting.RED));
			return;
		}
		if (color == RuletaColor.ROSA && MomentoPhora.enMarcha()) {
			jugador.sendSystemMessage(Component.literal("No se puede lanzar el Momento Nutria con el Momento Phora en curso: espera a que se termine.").withStyle(ChatFormatting.RED));
			return;
		}
		String titulo = limpio(m.titulo());
		String texto = limpio(m.mensaje());
		String tiempo = limpio(m.tiempo());
		boolean esRoja = color == RuletaColor.ROJO;
		boolean phora = esRoja && m.phora();
		boolean reviil = !phora && (m.reviil() || esRoja && m.cielo());
		if (phora && MomentoPhora.enMarcha()) {
			jugador.sendSystemMessage(Component.literal("Ya hay un Momento Phora en curso: no se puede lanzar otro hasta que se termine.").withStyle(ChatFormatting.RED));
			return;
		}
		if ((phora || esRoja && reviil && (m.para() == null || m.para().isBlank())) && ComandoEclipse.enMarcha()) {
			jugador.sendSystemMessage(Component.literal("No se puede lanzar un Momento Reviil ni un Momento Phora con un eclipse en curso: espera a que se termine.").withStyle(ChatFormatting.RED));
			return;
		}
		if (phora && MomentoReviil.enMarcha()) {
			jugador.sendSystemMessage(Component.literal("No se puede lanzar el Momento Phora con un Momento Reviil en curso: espera a que se termine.").withStyle(ChatFormatting.RED));
			return;
		}
		if (esRoja && reviil && !phora && (m.para() == null || m.para().isBlank()) && MomentoPhora.enMarcha()) {
			jugador.sendSystemMessage(Component.literal("No se puede lanzar un Momento Reviil con el Momento Phora en curso: espera a que se termine.").withStyle(ChatFormatting.RED));
			return;
		}
		if (esRoja && !reviil && !phora)
			tiempo = null;
		if (esRoja && reviil && MomentosReviil.activo(Momentos.ECLIPSE))
			tiempo = null;
		List<Cambios.Entrada> nuevos = new ArrayList<>();
		if (color == RuletaColor.NARANJA || color == RuletaColor.AMARILLO) {
			List<Cambios.Entrada> pedidos = Cambios.elegidos(m.cambios());
			if (!pedidos.isEmpty()) {
				Cambios.Entrada elegido = pedidos.get(0);
				if (cambios(server).activo(elegido.id())) {
					jugador.sendSystemMessage(Component.literal(color == RuletaColor.AMARILLO
							? "Ese mob ya está activo y es permanente."
							: "Ese cambio de dificultad ya está activo y es permanente."));
					return;
				}
				if (elegido.id().equals(Mensajes.cambioPendiente())) {
					jugador.sendSystemMessage(Component.literal("Ese cambio de dificultad ya está lanzado: se activa cuando salga el anuncio en el chat."));
					return;
				}
				nuevos.add(elegido);
			}
		}
		if (texto == null && !nuevos.isEmpty())
			texto = Cambios.unirTextos(nuevos);
		boolean paraUno = m.para() != null && !m.para().isBlank();
		if (paraUno && !nuevos.isEmpty()) {
			jugador.sendSystemMessage(Component.literal("Los cambios de dificultad y los mobs nuevos son para todos: quita el PARA."));
			return;
		}
		if (esRoja && reviil && !paraUno) {
			MomentosReviil.antesDeLanzar(jugador);
			texto = MomentosBunker.conCoordenadas(texto);
		}
		if (esRoja && reviil && !paraUno && texto == null) {
			String[] momentos = MomentosReviil.textoArmados();
			if (momentos != null)
				texto = momentos[1];
		}
		if (titulo == null && !nuevos.isEmpty() && color == RuletaColor.NARANJA)
			titulo = "Cambios de dificultad";
		int segundos = -1;
		if (tiempo != null) {
			segundos = Tiempo.leer(tiempo);
			if (segundos <= 0 || segundos > 86400) {
				jugador.sendSystemMessage(Component.literal("Temporizador no válido: " + tiempo + ". Usa 59:59 o hasta 24:00:00."));
				return;
			}
		}
		List<ServerPlayer> destino = null;
		boolean conSancion = color == RuletaColor.MORADO && m.sancion() != 0;
		boolean paraTodos = color == RuletaColor.ROSA || color == RuletaColor.NARANJA || color == RuletaColor.AMARILLO;
		if (!conSancion && !paraTodos && m.para() != null && !m.para().isBlank()) {
			ServerPlayer elegido = server.getPlayerList().getPlayerByName(m.para());
			if (elegido == null) {
				jugador.sendSystemMessage(Component.literal("El jugador " + m.para() + " no está conectado."));
				return;
			}
			destino = List.of(elegido);
		}
		Runnable sancion = null;
		if (color == RuletaColor.MORADO && m.sancion() != 0) {
			ServerPlayer obj = m.objetivo() == null ? null : server.getPlayerList().getPlayerByName(m.objetivo());
			if (obj == null) {
				jugador.sendSystemMessage(Component.literal("Elige un jugador conectado para la sanción."));
				return;
			}
			if (Sanciones.protegido(server, obj)) {
				jugador.sendSystemMessage(Component.literal("No se puede sancionar a " + obj.getGameProfile().getName() + ": es operador de nivel 3 o 4."));
				return;
			}
			long segs = Sanciones.leerTiempo(m.tiempoBan());
			if (m.sancion() == Sanciones.BAN && segs <= 0L) {
				jugador.sendSystemMessage(Component.literal("Tiempo de ban no válido: usa minutos (30 o 30m) u horas (2h, 1h30m)."));
				return;
			}
			int tipoSancion = m.sancion();
			String quienSanciona = jugador.getGameProfile().getName();
			sancion = () -> Sanciones.programar(obj, tipoSancion, segs, quienSanciona);
		}
		boolean mini = m.tipo() == 0;
		String idCambio = nuevos.isEmpty() ? null : nuevos.get(0).id();
		Runnable alEnviar = nuevos.isEmpty() ? null : () -> activarCambios(server, nuevos);
		Runnable antesCielo = null;
		boolean eclipseNuevo = esRoja && reviil && !paraUno && MomentosReviil.activo(Momentos.ECLIPSE) && !MomentoReviil.enMarcha();
		if (esRoja && reviil && !paraUno) {
			int duracion = segundos;
			boolean conCielo = (m.cielo() || MomentosReviil.hayArmados()) && !MomentosReviil.sinCielo();
			if (conCielo) {
				antesCielo = () -> MomentoReviil.empezar(server, duracion, true);
				alEnviar = () -> MomentosReviil.lanzarElegidos(server);
			} else {
				alEnviar = () -> {
					MomentoReviil.empezar(server, duracion, false, !eclipseNuevo);
					MomentosReviil.lanzarElegidos(server);
				};
			}
		}
		if (phora) {
			int duracion = segundos;
			alEnviar = () -> MomentoPhora.empezar(server, duracion);
			antesCielo = null;
			if (texto == null)
				texto = TEXTO_PHORA;
		}
		if (sancion != null) {
			Runnable antes = alEnviar;
			Runnable s2 = sancion;
			alEnviar = antes == null ? s2 : () -> {
				antes.run();
				s2.run();
			};
		}
		boolean conAnimacion = !phora && esRoja && (m.animacion() || reviil && !paraUno && MomentosReviil.hayArmados());
		girar(server, color, jugador.getGameProfile().getName(), mini, destino);
		Mensajes.programar(color, jugador.getGameProfile().getName(), titulo, texto, segundos, destino, idCambio, alEnviar, conAnimacion, false);
		if (conAnimacion && esRoja && reviil && !paraUno && !MomentoReviil.enMarcha())
			Mensajes.marcarTema(eclipseNuevo);
		if (antesCielo != null)
			Mensajes.antesDelAnuncio(antesCielo);
		if (phora)
			Mensajes.marcarPhora();
	}

	private static void activarCambios(MinecraftServer server, List<Cambios.Entrada> nuevos) {
		DificultadDatos d = cambios(server);
		for (Cambios.Entrada nuevo : nuevos) {
			d.activar(nuevo.id());
			CambiosEfectos.alActivar(server, nuevo.id());
		}
		d.setDirty();
		Red.enviarATodos(server, new CambiosPayload(d.csv()));
	}

	public static void alRegistrarComandos(RegisterCommandsEvent e) {
		e.getDispatcher().register(Commands.literal("totem")
				.requires(s -> s.hasPermission(2))
				.then(Commands.literal("mensaje")
						.then(Commands.literal("on").executes(c -> mensajeTotem(c.getSource(), true)))
						.then(Commands.literal("off").executes(c -> mensajeTotem(c.getSource(), false)))));
		e.getDispatcher().register(Commands.literal("cambios")
				.requires(s -> s.hasPermission(2))
				.executes(c -> {
					ServerPlayer p = c.getSource().getPlayerOrException();
					Red.enviar(p, new CambiosPayload(cambios(p.getServer()).csv()));
					Red.enviar(p, new ReviilPayload("abrir_cambios", 1, Component.empty(), Component.empty()));
					return 1;
				}));
	}

	private static int mensajeTotem(CommandSourceStack fuente, boolean on) {
		DificultadDatos d = cambios(fuente.getServer());
		d.sinMensajeTotem = !on;
		d.setDirty();
		fuente.sendSuccess(() -> Component.literal(on ? "Mensaje de los tótems: ACTIVADO." : "Mensaje de los tótems: DESACTIVADO."), true);
		return 1;
	}

	static void desactivarCambio(ServerPlayer jugador, String id) {
		if (jugador == null || jugador.getServer() == null || !jugador.hasPermissions(PERMISSION_LEVEL))
			return;
		MinecraftServer server = jugador.getServer();
		DificultadDatos d = cambios(server);
		if (!d.activo(id))
			return;
		d.desactivar(id);
		d.setDirty();
		if (Cambios.ID_SIN_REGENERACION.equals(id))
			server.getGameRules().getRule(GameRules.RULE_NATURAL_REGENERATION).set(true, server);
		Red.enviarATodos(server, new CambiosPayload(d.csv()));
		Cambios.Entrada e = Cambios.porId(id);
		jugador.sendSystemMessage(Component.literal("Desactivado: " + (e == null ? id : e.titulo())).withStyle(ChatFormatting.YELLOW));
	}

	private static void parar(MinecraftServer server) {
		Mensajes.parar(server);
		MomentoPhora.terminar(server);
		MomentoReviil.terminar(server);
		if (MomentosReviil.hayArmados())
			MomentosReviil.alTerminar(server);
		Pantalla.sacarAviso(server, null);
		Red.enviarATodos(server, new ReviilPayload("parar", 1, Component.empty(), Component.empty()));
		for (ServerPlayer p : server.getPlayerList().getPlayers()) {
			p.removeEffect(Efectos.TEMBLOR.holder());
			p.removeEffect(Efectos.RULETA_ROJA.holder());
		}
	}

	private static DamageSource fuenteMagma(ServerPlayer p) {
		return new DamageSource(p.damageSources().starve().typeHolder()) {
			@Override
			public String getMsgId() {
				return "hotFloor";
			}
		};
	}

	private static DamageSource fuentePerla(ServerPlayer p) {
		return new DamageSource(p.damageSources().starve().typeHolder()) {
			@Override
			public String getMsgId() {
				return "fall";
			}
		};
	}

	public static DamageSource fuenteLetalPublica(ServerPlayer p, String id, String frase) {
		return fuenteLetal(p, id, frase);
	}

	/** Daño de "hambre" (ignora armadura) con nombre y mensaje de muerte propios. */
	static DamageSource fuenteLetal(ServerPlayer p, String id, String frase) {
		Holder<DamageType> tipo = p.damageSources().starve().typeHolder();
		return new DamageSource(tipo) {
			@Override
			public String getMsgId() {
				return id;
			}

			@Override
			public Component getLocalizedDeathMessage(LivingEntity victima) {
				return Component.empty().append(victima.getDisplayName()).append(Component.literal(frase));
			}
		};
	}

	public static boolean cambioActivoPublico(String id) {
		return cambioActivo(id);
	}

	static boolean cambioActivo(String id) {
		MinecraftServer server = Puente.servidor();
		return server != null && server.overworld() != null && cambios(server).activo(id);
	}

	private static DificultadDatos cambios(MinecraftServer server) {
		if (cambios == null)
			cambios = server.overworld().getDataStorage().computeIfAbsent(DificultadDatos.FACTORY, DificultadDatos.NOMBRE);
		return cambios;
	}

	static boolean perlaX2() {
		MinecraftServer server = Puente.servidor();
		return server != null && cambios(server).perla;
	}

	static boolean magmaDano() {
		MinecraftServer server = Puente.servidor();
		return server != null && cambios(server).magma;
	}

	public static boolean totemGastadoAhora(ServerPlayer p) {
		Long t = ULTIMO_AVISO_TOTEM.get(p.getUUID());
		return t != null && p.level().getGameTime() - t <= 1L;
	}

	/** Mensaje en el chat cuando alguien consume un tótem (salvo con /totem mensaje off). */
	public static void avisarTotem(ServerPlayer p, DamageSource fuente, Component nombreTotem) {
		if (p.getServer() == null || cambios(p.getServer()).sinMensajeTotem)
			return;
		long ahora = p.level().getGameTime();
		Long previo = ULTIMO_AVISO_TOTEM.put(p.getUUID(), ahora);
		if (previo != null && ahora - previo <= 1L)
			return;
		Component mensaje = Component.empty()
				.append(Component.literal("").setStyle(Style.EMPTY.withFont(id("totem"))))
				.append(Component.literal(" "))
				.append(p.getName().copy())
				.append(Component.literal(" consumió "))
				.append(nombreTotem.copy().withStyle(estilo -> estilo.withColor(COLOR_TOTEM)))
				.append(Component.literal(" ("))
				.append(razonTotem(fuente, p))
				.append(Component.literal(")"))
				.withStyle(estilo -> estilo.withColor(COLOR_TOTEM));
		p.getServer().getPlayerList().broadcastSystemMessage(mensaje, false);
	}

	public static Component nombreTotem(ItemStack stack) {
		return stack.getHoverName().copy();
	}

	private static Component razonTotem(DamageSource fuente, ServerPlayer victima) {
		if ("cloudruleta_supernova.onda".equals(fuente.getMsgId()))
			return Component.literal("Onda Expansiva");
		if ("cloudruleta_supernova".equals(fuente.getMsgId()))
			return Component.literal("Supernova");
		if ("cloudruleta.herobrine".equals(fuente.getMsgId()))
			return Component.literal("Herobrine");
		if ("nuclear_explosion".equals(fuente.getMsgId()))
			return Component.literal("Explosión de Creeper Nuclear");
		String base = switch (fuente.getMsgId()) {
			case "fall" -> "caída";
			case "lava" -> "lava";
			case "inFire", "onFire" -> "fuego";
			case "hotFloor" -> "bloque de magma";
			case "cloudruleta.boton" -> "botón";
			case "cloudruleta.placa" -> "placa de presión";
			case "cloudruleta.puerta" -> "puerta";
			case "cloudruleta.gas" -> "Gas Tóxico de Reviil";
			case "cloudruleta.luna" -> "Luna maldita";
			case "cloudruleta.rayo" -> "Rayo mortal de Reviil";
			case "cloudruleta.agujero" -> "Agujeros Negros de Reviil";
			case "drown" -> "ahogamiento";
			case "explosion", "explosion.player" -> "explosión";
			case "arrow" -> "flecha";
			case "trident" -> "tridente";
			case "fireball", "fireworks" -> "bola de fuego";
			case "thrown", "indirectMagic", "magic" -> "magia";
			case "wither", "witherSkull" -> "Wither";
			case "starve" -> "hambre";
			case "cactus" -> "cactus";
			case "sweetBerryBush" -> "arbusto de bayas";
			case "fallingBlock", "anvil" -> "bloque que cae";
			case "fallingStalactite" -> "estalactita";
			case "stalagmite" -> "estalagmita";
			case "flyIntoWall" -> "choque al volar";
			case "freeze" -> "congelación";
			case "lightningBolt" -> "rayo";
			case "dragonBreath" -> "aliento de dragón";
			case "thorns" -> "espinas";
			case "sonic_boom" -> "grito del Warden";
			case "cramming" -> "aplastamiento";
			case "inWall" -> "asfixia en un bloque";
			case "dryout" -> "deshidratación";
			case "mob", "player", "mob_projectile" -> "ataque";
			case "generic" -> "daño";
			default -> fuente.getMsgId();
		};
		Entity autor = fuente.getEntity();
		if (autor == null || autor == victima)
			return Component.literal(base);
		return Component.literal(base + " de ").append(autor.getName());
	}

	/** Manejadores de eventos (antes la clase interna Ruleta.Eventos registrada en el bus de Forge). */
	public static final class Eventos {
		private Eventos() {
		}

		@SubscribeEvent
		public static void alTick(TickEvent.ServerTickEvent e) {
			if (e.phase != TickEvent.Phase.END)
				return;
			tickChat++;
			if (!CHAT_VERDE.isEmpty()) {
				List<ServerPlayer> escribieron = new ArrayList<>();
				CHAT_VERDE.forEach((p, hasta) -> {
					if (tickChat >= hasta)
						escribieron.add(p);
				});
				// el nombre verde dura 10 ticks tras escribir (Puente aplica NameFormat en cada getDisplayName)
				for (ServerPlayer p : escribieron)
					CHAT_VERDE.remove(p);
			}
			MinecraftServer server = e.getServer();
			if (server != null) {
				Mensajes.tick(server);
				MomentoReviil.tick(server);
			}
		}

		@SubscribeEvent
		public static void alApagar(ServerStoppedEvent e) {
			cambios = null;
			Mensajes.parar();
			MomentoReviil.limpiar();
			Pantalla.aviso = null;
			Pantalla.temporizador = null;
		}

		@SubscribeEvent
		public static void alTeletransportarPerla(EntityEvents.EnderPearlEvent e) {
			float dano = e.getAttackDamage();
			if (perlaX2())
				dano *= 2.0F;
			ServerPlayer jugador = e.getPlayer();
			MinecraftServer servidor = jugador == null ? null : jugador.getServer();
			if (jugador != null)
				CambiosEfectos.marcarPerla(jugador);
			if (servidor != null && dano > 0.0F && cambioActivo(Cambios.ID_PERLA_ARMADURA)) {
				e.setAttackDamage(0.0F);
				float finalDano = dano;
				servidor.execute(() -> {
					if (jugador.isAlive())
						jugador.hurt(fuentePerla(jugador), finalDano);
				});
			} else {
				e.setAttackDamage(dano);
			}
			if (jugador != null && !e.isCanceled())
				CambiosEfectos.perlaAterrizo(jugador, e.getTargetX(), e.getTargetY(), e.getTargetZ());
		}

		@SubscribeEvent(priority = EventPriority.LOWEST)
		public static void alUsarTotem(LivingEvents.LivingUseTotemEvent e) {
			if (e.getEntity() instanceof ServerPlayer p)
				avisarTotem(p, e.getSource(), nombreTotem(e.getTotem()));
		}

		@SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
		public static void antesDeMorir(LivingEvents.LivingDeathEvent e) {
			if (e.getEntity() instanceof ServerPlayer p)
				INVENTARIO_ANTES.put(p.getUUID(), contarObjetos(p));
		}

		/** Si otra cosa canceló la muerte (un tótem del mod), se busca qué tótem falta para avisar. */
		@SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
		public static void despuesDeMorir(LivingEvents.LivingDeathEvent e) {
			if (!(e.getEntity() instanceof ServerPlayer p))
				return;
			Map<Item, Integer> antes = INVENTARIO_ANTES.remove(p.getUUID());
			if (antes == null || !e.isCanceled() || !p.isAlive() || p.getHealth() <= 0.0F)
				return;
			Map<Item, Integer> ahora = contarObjetos(p);
			Item gastado = null;
			for (Map.Entry<Item, Integer> en : antes.entrySet()) {
				if (en.getValue() > ahora.getOrDefault(en.getKey(), 0)) {
					ResourceLocation id = BuiltInRegistries.ITEM.getKey(en.getKey());
					if (gastado == null || id != null && id.getPath().contains("totem"))
						gastado = en.getKey();
				}
			}
			if (gastado != null)
				avisarTotem(p, e.getSource(), nombreTotem(new ItemStack(gastado)));
		}

		private static Map<Item, Integer> contarObjetos(ServerPlayer p) {
			Map<Item, Integer> r = new HashMap<>();
			int n = p.getInventory().getContainerSize();
			for (int i = 0; i < n; i++) {
				ItemStack st = p.getInventory().getItem(i);
				if (!st.isEmpty())
					r.merge(st.getItem(), st.getCount(), Integer::sum);
			}
			return r;
		}

		@SubscribeEvent
		public static void alChat(ServerChatEvent e) {
			ServerPlayer p = e.getPlayer();
			CHAT_VERDE.put(p, tickChat + TICKS_CHAT_VERDE);
		}

		@SubscribeEvent
		public static void alNombreJugador(PlayerEvent.NameFormat e) {
			if (e.getEntity() instanceof ServerPlayer p && CHAT_VERDE.containsKey(p))
				e.setDisplayname(e.getDisplayname().copy().withStyle(estilo -> estilo.withColor(TextColor.fromRgb(COLOR_NOMBRE))));
		}

		@SubscribeEvent
		public static void alTickJugador(TickEvent.PlayerTickEvent e) {
			if (e.phase != TickEvent.Phase.END || !(e.player instanceof ServerPlayer p) || !magmaDano())
				return;
			if (!p.isAlive() || p.isSpectator() || p.isCreative())
				return;
			AABB caja = p.getBoundingBox().inflate(0.002D);
			BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
			for (int x = Mth.floor(caja.minX); x <= Mth.floor(caja.maxX); x++)
				for (int y = Mth.floor(caja.minY); y <= Mth.floor(caja.maxY); y++)
					for (int z = Mth.floor(caja.minZ); z <= Mth.floor(caja.maxZ); z++)
						if (p.level().getBlockState(pos.set(x, y, z)).is(Blocks.MAGMA_BLOCK)) {
							p.hurt(fuenteMagma(p), DANO_MAGMA);
							return;
						}
		}

		@SubscribeEvent
		public static void alEntrar(PlayerEvent.PlayerLoggedInEvent e) {
			if (e.getEntity() instanceof ServerPlayer p && p.getServer() != null) {
				Red.enviar(p, new CambiosPayload(cambios(p.getServer()).csv()));
				MomentoReviil.alEntrar(p);
				Pantalla.alEntrar(p);
			}
		}
	}

	/** Cambios de dificultad activos (guardados en el mundo, nombre "cloudruleta_perla" como en el original). */
	static final class DificultadDatos extends SavedData {
		static final String NOMBRE = "cloudruleta_perla";
		static final SavedData.Factory<DificultadDatos> FACTORY = new SavedData.Factory<>(DificultadDatos::new, DificultadDatos::cargar, null);
		boolean perla;
		boolean magma;
		boolean sinMensajeTotem;
		final Set<String> otros = new LinkedHashSet<>();

		static DificultadDatos cargar(CompoundTag tag, HolderLookup.Provider registros) {
			DificultadDatos d = new DificultadDatos();
			d.perla = tag.getBoolean("activo");
			d.magma = tag.getBoolean("magma");
			d.sinMensajeTotem = tag.getBoolean("sin_mensaje_totem");
			ListTag lista = tag.getList("cambios", Tag.TAG_STRING);
			for (int i = 0; i < lista.size(); i++)
				d.otros.add(lista.getString(i));
			return d;
		}

		@Override
		public CompoundTag save(CompoundTag tag, HolderLookup.Provider registros) {
			tag.putBoolean("activo", perla);
			tag.putBoolean("magma", magma);
			tag.putBoolean("sin_mensaje_totem", sinMensajeTotem);
			ListTag lista = new ListTag();
			for (String id : otros)
				lista.add(StringTag.valueOf(id));
			tag.put("cambios", lista);
			return tag;
		}

		boolean activo(String id) {
			if (Cambios.ID_PERLA.equals(id))
				return perla;
			return Cambios.ID_MAGMA.equals(id) ? magma : otros.contains(id);
		}

		void desactivar(String id) {
			if (Cambios.ID_PERLA.equals(id))
				perla = false;
			else if (Cambios.ID_MAGMA.equals(id))
				magma = false;
			else
				otros.remove(id);
		}

		void activar(String id) {
			if (Cambios.ID_PERLA.equals(id))
				perla = true;
			else if (Cambios.ID_MAGMA.equals(id))
				magma = true;
			else
				otros.add(id);
		}

		String csv() {
			StringJoiner j = new StringJoiner(",");
			if (perla)
				j.add("perla");
			if (magma)
				j.add("magma");
			for (String id : otros)
				j.add(id);
			return j.toString();
		}
	}
}
