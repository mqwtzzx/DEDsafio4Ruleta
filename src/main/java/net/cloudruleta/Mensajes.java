package net.cloudruleta;

import com.google.gson.JsonObject;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/** Anuncios de la cloudruleta en el chat, avisos en pantalla y temporizadores. */
public final class Mensajes {
	private static final Logger LOG = LoggerFactory.getLogger(Ruleta.MOD_ID);
	static final int TICKS_MENSAJE = Math.round(278.6F) + 1;
	public static final int TICKS_MENSAJE_PUBLICO = TICKS_MENSAJE;
	static final int TICKS_ANIMACION = Math.round(316.0F);
	static final int TICKS_TEMA = Math.round(1289.4F);
	private static boolean conTema;
	private static boolean esperarTema;
	static final int TICKS_NUTRIA = Math.round(370.0F);
	private static boolean nutria;
	private static final String ICONO = "";
	public static final String TITULO_DEF = "RULETA {COLOR}:";
	public static final String TEXTO_DEF = "El jugador {jugador} giró la cloudruleta y salió {COLOR}.";
	private static final Map<RuletaColor, Estilo> ESTILO = new EnumMap<>(RuletaColor.class);
	private static long tick;
	private static long en = -1L;
	private static RuletaColor color;
	private static String jugador = "";
	private static List<UUID> para;
	private static String texto;
	private static String tituloPersonal;
	private static int segundosTemp = -1;
	private static String idPendiente;
	private static Runnable alEnviar;
	private static Runnable antes;
	private static boolean phora;
	private static boolean phoraVoz;
	private static final int TICKS_VOZ_PHORA = 100;
	private static boolean animacion;
	private static long finTemp = -1L;
	private static List<UUID> paraTemp;
	private static final Map<RuletaColor, String> ENCABEZADO = new EnumMap<>(RuletaColor.class);
	private static final int TICKS_EFECTOS = TICKS_ANIMACION + 20;

	private Mensajes() {
	}

	static void programar(RuletaColor c, String quien, String titulo, String txt, int segundos, Collection<ServerPlayer> a, String idCambio,
						  Runnable accionAlEnviar, boolean conAnimacion) {
		programar(c, quien, titulo, txt, segundos, a, idCambio, accionAlEnviar, conAnimacion, false);
	}

	static void programar(RuletaColor c, String quien, String titulo, String txt, int segundos, Collection<ServerPlayer> a, String idCambio,
						  Runnable accionAlEnviar, boolean conAnimacion, boolean sinRuleta) {
		animacion = conAnimacion;
		conTema = false;
		esperarTema = false;
		antes = null;
		phora = false;
		phoraVoz = false;
		nutria = c == RuletaColor.ROSA;
		idPendiente = idCambio;
		alEnviar = accionAlEnviar;
		tituloPersonal = titulo;
		en = tick + (sinRuleta ? 1 : TICKS_MENSAJE);
		color = c;
		texto = txt;
		segundosTemp = segundos;
		jugador = quien;
		para = a == null ? null : a.stream().map(Entity::getUUID).toList();
	}

	static void marcarTema(boolean esperarFin) {
		conTema = true;
		esperarTema = esperarFin;
	}

	static void antesDelAnuncio(Runnable r) {
		antes = r;
	}

	static void marcarPhora() {
		phora = true;
		phoraVoz = false;
	}

	static void parar() {
		en = -1L;
		antes = null;
		phora = false;
		phoraVoz = false;
		animacion = false;
		nutria = false;
		segundosTemp = -1;
		idPendiente = null;
		alEnviar = null;
	}

	static boolean ocupado() {
		return en >= 0L;
	}

	static boolean nutriaEnCurso() {
		return en >= 0L && color == RuletaColor.ROSA;
	}

	static String cambioPendiente() {
		return idPendiente;
	}

	static void parar(MinecraftServer server) {
		parar();
		quitarTemporizador(server);
	}

	private static void enviar(MinecraftServer server, List<UUID> destino, ReviilPayload p) {
		if (destino == null) {
			Red.enviarATodos(server, p);
			return;
		}
		for (UUID id : destino) {
			ServerPlayer pl = server.getPlayerList().getPlayer(id);
			if (pl != null)
				Red.enviar(pl, p);
		}
	}

	private static void quitarTemporizador(MinecraftServer server) {
		if (finTemp >= 0L) {
			finTemp = -1L;
			Pantalla.temporizador = null;
			enviar(server, paraTemp, new ReviilPayload("temporizador", -1, Component.empty(), Component.empty()));
		}
	}

	public static void temporizadorComando(MinecraftServer server, int segundos) {
		quitarTemporizador(server);
		paraTemp = null;
		finTemp = tick + (long) segundos * 20L;
		Pantalla.temporizador = "comando";
		enviar(server, null, new ReviilPayload("temporizador", segundos, Component.empty(), Component.empty()));
	}

	public static boolean detenerTemporizador(MinecraftServer server) {
		boolean habia = finTemp >= 0L;
		quitarTemporizador(server);
		return habia;
	}

	static void tick(MinecraftServer server) {
		tick++;
		if (finTemp >= 0L && tick >= finTemp)
			quitarTemporizador(server);
		if (en >= 0L && tick >= en && nutria) {
			nutria = false;
			en = tick + TICKS_NUTRIA;
			enviar(server, para, new ReviilPayload("nutria", 1, Component.empty(), Component.empty()));
			return;
		}
		if (en >= 0L && tick >= en && animacion) {
			animacion = false;
			en = tick + (esperarTema ? Math.max(TICKS_ANIMACION, TICKS_TEMA) : TICKS_ANIMACION);
			enviar(server, para, new ReviilPayload("animacion", conTema ? 2 : 1, Component.empty(), Component.empty()));
			efectosAnimacion(server, para, conTema ? TICKS_TEMA + 20 : TICKS_EFECTOS);
			return;
		}
		if (en >= 0L && tick >= en && phoraVoz) {
			phoraVoz = false;
			en = tick + TICKS_VOZ_PHORA;
			enviar(server, para, new ReviilPayload("phora_voz", 1, Component.empty(), Component.empty()));
			return;
		}
		if (en >= 0L && tick >= en && antes != null) {
			Runnable r = antes;
			antes = null;
			r.run();
			int espera = MomentoReviil.ticksHastaGrieta();
			if (espera > 0) {
				MomentoReviil.alargar(espera);
				en = tick + espera;
				return;
			}
		}
		if (en >= 0L && tick >= en) {
			en = -1L;
			Runnable activar = alEnviar;
			idPendiente = null;
			alEnviar = null;
			RuletaColor colorAnuncio = phora ? RuletaColor.NARANJA : color;
			String tituloAnuncio = !phora || tituloPersonal != null && !tituloPersonal.isBlank() ? tituloPersonal : "MOMENTO PHORA:";
			phora = false;
			if (texto != null && !texto.isBlank()) {
				Pantalla.sacarAviso(server, para);
				if (llevaAviso(colorAnuncio)) {
					Component[] av = aviso(colorAnuncio, tituloAnuncio, texto, jugador);
					Pantalla.ponerAviso(server, para, colorAnuncio.id, av[0], av[1]);
				}
				Component m = anuncio(colorAnuncio, tituloAnuncio, texto, jugador);
				ReviilPayload campanita = new ReviilPayload("mensaje", 1, Component.empty(), Component.empty());
				if (para == null) {
					server.getPlayerList().broadcastSystemMessage(m, false);
					Red.enviarATodos(server, campanita);
				} else {
					for (UUID id : para) {
						ServerPlayer p = server.getPlayerList().getPlayer(id);
						if (p != null) {
							p.sendSystemMessage(m);
							Red.enviar(p, campanita);
						}
					}
				}
			}
			if (segundosTemp > 0) {
				paraTemp = para;
				finTemp = tick + (long) segundosTemp * 20L;
				Pantalla.temporizador = "verde";
				enviar(server, paraTemp, new ReviilPayload("temporizador", segundosTemp, Component.empty(), Component.empty()));
				segundosTemp = -1;
			}
			if (activar != null)
				activar.run();
		}
	}

	private static void efectosAnimacion(MinecraftServer server, List<UUID> destino, int ticks) {
		Collection<ServerPlayer> jugadores = destino == null
				? server.getPlayerList().getPlayers()
				: destino.stream().map(id -> server.getPlayerList().getPlayer(id)).filter(Objects::nonNull).toList();
		for (ServerPlayer p : jugadores) {
			p.addEffect(new MobEffectInstance(Efectos.TEMBLOR.holder(), ticks, 0, false, false, true));
			p.addEffect(new MobEffectInstance(Efectos.RULETA_ROJA.holder(), ticks, 0, false, false, true));
		}
	}

	public static Component vista(RuletaColor c, String titulo, String texto, String jugador) {
		Config cfg = new Config();
		cfg.titulo = titulo;
		cfg.texto = texto;
		return mensaje(c, cfg, jugador);
	}

	public static Component vistaMomento(boolean reviil, String titulo, String texto, String jugador) {
		Style t = Style.EMPTY.withFont(ResourceLocation.fromNamespaceAndPath(Ruleta.MOD_ID, reviil ? "rotulo" : "rotulo_nutria"))
				.withColor(reviil ? 11013656 : 11540590);
		Style cuerpo = Style.EMPTY.applyFormat(reviil ? ChatFormatting.RED : ChatFormatting.LIGHT_PURPLE);
		RuletaColor c = reviil ? RuletaColor.ROJO : RuletaColor.ROSA;
		return Component.empty()
				.append(Component.literal(ICONO).setStyle(t))
				.append(Component.literal(" " + titulo).setStyle(t.withBold(true)))
				.append(Component.literal("\n\n"))
				.append(poner(texto, cuerpo, cuerpo.withBold(true), c, jugador));
	}

	static Component anuncio(RuletaColor c, String txt, String quien) {
		return anuncio(c, null, txt, quien);
	}

	public static String encabezado(RuletaColor c) {
		return ENCABEZADO.get(c);
	}

	static Component anuncio(RuletaColor c, String tituloPropio, String txt, String quien) {
		String encabezado = tituloPropio != null && !tituloPropio.isBlank() ? tituloPropio : ENCABEZADO.get(c);
		Estilo e = ESTILO.get(c);
		Style titulo = Style.EMPTY.withFont(ResourceLocation.fromNamespaceAndPath(Ruleta.MOD_ID, e.fuente())).withColor(e.titulo());
		MutableComponent t = Component.empty()
				.append(Component.literal(ICONO).setStyle(titulo))
				.append(Component.literal(" ").setStyle(titulo))
				.append(metalico(encabezado, titulo.withBold(true), e.titulo()));
		Style cuerpo = Style.EMPTY.applyFormat(e.texto());
		return t.append(Component.literal("\n\n")).append(poner(txt, cuerpo, cuerpo.withBold(true), c, quien));
	}

	public static int claro(int rgb) {
		int r = rgb >> 16 & 255;
		int g = rgb >> 8 & 255;
		int b = rgb & 255;
		return r + (255 - r) * 30 / 100 << 16 | g + (255 - g) * 30 / 100 << 8 | b + (255 - b) * 30 / 100;
	}

	static int mezcla(int a, int b, float k) {
		k = Math.max(0.0F, Math.min(1.0F, k));
		int r = Math.round((float) (a >> 16 & 255) + (float) ((b >> 16 & 255) - (a >> 16 & 255)) * k);
		int g = Math.round((float) (a >> 8 & 255) + (float) ((b >> 8 & 255) - (a >> 8 & 255)) * k);
		int bl = Math.round((float) (a & 255) + (float) ((b & 255) - (a & 255)) * k);
		return r << 16 | g << 8 | bl;
	}

	/** Texto con degradado metálico y un brillo, letra a letra. */
	static MutableComponent metalico(String texto, Style estilo, int base) {
		int claro = claro(base);
		int oscuro = mezcla(claro, 0, 0.28F);
		MutableComponent out = Component.empty();
		int n = Math.max(1, texto.codePointCount(0, texto.length()) - 1);
		int i = 0;
		for (int k = 0; k < texto.length(); i++) {
			int cp = texto.codePointAt(k);
			float u = (float) i / (float) n;
			float onda = 0.5F + 0.5F * (float) Math.cos((double) u * Math.PI * 2.0D);
			int col = mezcla(oscuro, claro, onda);
			float brillo = (float) Math.exp(-Math.pow((u - 0.33F) / 0.14F, 2.0D));
			col = mezcla(col, 16774384, brillo * 0.7F);
			out.append(Component.literal(new String(Character.toChars(cp))).setStyle(estilo.withColor(col)));
			k += Character.charCount(cp);
		}
		return out;
	}

	static boolean llevaAviso(RuletaColor c) {
		return true;
	}

	static Component[] aviso(RuletaColor c, String tituloPropio, String txt, String quien) {
		String encabezado = tituloPropio != null && !tituloPropio.isBlank() ? tituloPropio : ENCABEZADO.get(c);
		Estilo e = ESTILO.get(c);
		Style titulo = Style.EMPTY.withFont(ResourceLocation.fromNamespaceAndPath(Ruleta.MOD_ID, e.fuente())).withColor(e.titulo());
		Component t = Component.empty()
				.append(Component.literal(ICONO).setStyle(titulo))
				.append(Component.literal(" ").setStyle(titulo))
				.append(Component.literal(encabezado).setStyle(titulo.withBold(true)));
		Style cuerpo = Style.EMPTY.applyFormat(e.texto());
		return new Component[]{t, poner(txt, cuerpo, cuerpo.withBold(true), c, quien)};
	}

	static Component mensaje(RuletaColor c, Config cfg, String quien) {
		Estilo e = ESTILO.get(c);
		Style titulo = Style.EMPTY.withFont(ResourceLocation.fromNamespaceAndPath(Ruleta.MOD_ID, e.fuente())).withColor(e.titulo());
		MutableComponent t = Component.empty()
				.append(Component.literal(ICONO).setStyle(titulo))
				.append(Component.literal(" ").setStyle(titulo))
				.append(poner(cfg.titulo, titulo.withBold(true), titulo.withBold(true), c, quien));
		Style cuerpo = Style.EMPTY.applyFormat(e.texto());
		return t.append(Component.literal("\n\n")).append(poner(cfg.texto, cuerpo, cuerpo.withBold(true), c, quien));
	}

	/** Sustituye {jugador}, {COLOR} (resaltados) y {color} (normal) en el texto. */
	private static MutableComponent poner(String txt, Style normal, Style resalte, RuletaColor c, String quien) {
		String nombre = ESTILO.get(c).nombre();
		MutableComponent out = Component.empty();
		StringBuilder suelto = new StringBuilder();
		int i = 0;
		while (i < txt.length()) {
			String resto = txt.substring(i);
			String puesto = null;
			int largo = 0;
			if (resto.startsWith("{jugador}")) {
				puesto = quien;
				largo = 9;
			} else if (resto.startsWith("{COLOR}")) {
				puesto = nombre.toUpperCase(Locale.ROOT);
				largo = 7;
			}
			if (puesto != null) {
				if (!suelto.isEmpty())
					out.append(Component.literal(suelto.toString()).setStyle(normal));
				suelto.setLength(0);
				out.append(Component.literal(puesto).setStyle(resalte));
				i += largo;
			} else if (resto.startsWith("{color}")) {
				suelto.append(nombre);
				i += 7;
			} else {
				suelto.append(txt.charAt(i));
				i++;
			}
		}
		if (!suelto.isEmpty())
			out.append(Component.literal(suelto.toString()).setStyle(normal));
		return out;
	}

	static {
		ENCABEZADO.put(RuletaColor.ROJO, "MOMENTO REVIIL:");
		ENCABEZADO.put(RuletaColor.ROSA, "MOMENTO NUTRIA:");
		ENCABEZADO.put(RuletaColor.VERDE, "ANUNCIO:");
		ENCABEZADO.put(RuletaColor.NARANJA, "CAMBIO DE DIFICULTAD:");
		ENCABEZADO.put(RuletaColor.AMARILLO, "NUEVO MOB:");
		ENCABEZADO.put(RuletaColor.MORADO, "SANCIÓN:");
		ENCABEZADO.put(RuletaColor.CIAN, "MISIÓN DEL DÍA:");
		ENCABEZADO.put(RuletaColor.AZUL, "Evento Especial");
		ESTILO.put(RuletaColor.ROJO, new Estilo("Roja", ChatFormatting.RED, 11013656, "rotulo"));
		ESTILO.put(RuletaColor.ROSA, new Estilo("Rosa", ChatFormatting.LIGHT_PURPLE, 11540590, "rotulo_nutria"));
		ESTILO.put(RuletaColor.MORADO, new Estilo("Morada", ChatFormatting.DARK_PURPLE, 6953626, "rotulo_morado"));
		ESTILO.put(RuletaColor.AZUL, new Estilo("Azul", ChatFormatting.BLUE, 1972367, "rotulo_azul"));
		ESTILO.put(RuletaColor.CIAN, new Estilo("Cian", ChatFormatting.AQUA, 558730, "rotulo_cian"));
		ESTILO.put(RuletaColor.VERDE, new Estilo("Verde", ChatFormatting.GREEN, 2001437, "rotulo_verde"));
		ESTILO.put(RuletaColor.AMARILLO, new Estilo("Amarilla", ChatFormatting.YELLOW, 10261520, "rotulo_amarillo"));
		ESTILO.put(RuletaColor.NARANJA, new Estilo("Naranja", ChatFormatting.GOLD, 11035662, "rotulo_naranja"));
	}

	/** Título y texto de los mensajes por color (config/cloudruleta.json, sección "mensajes"). */
	static final class Config {
		String titulo = TITULO_DEF;
		String texto = TEXTO_DEF;
		boolean mostrar = true;

		static Config leer(RuletaColor c) {
			Config cfg = new Config();
			try {
				JsonObject todos = ConfigArchivo.seccion("mensajes");
				if (todos == null) {
					guardarTodos();
					return cfg;
				}
				JsonObject r = todos.getAsJsonObject(c.id);
				if (r == null)
					return cfg;
				if (r.has("titulo"))
					cfg.titulo = r.get("titulo").getAsString();
				if (r.has("texto"))
					cfg.texto = r.get("texto").getAsString();
				if (r.has("mostrar"))
					cfg.mostrar = r.get("mostrar").getAsBoolean();
			} catch (Exception ex) {
				LOG.warn("[mensajes] no se pudo leer config/cloudruleta.json, uso los de siempre", ex);
			}
			return cfg;
		}

		static void cambiar(RuletaColor c, String clave, String valor) throws IOException {
			JsonObject todos = ConfigArchivo.seccion("mensajes");
			if (todos == null)
				todos = valoresDelPack();
			JsonObject r = todos.getAsJsonObject(c.id);
			if (r == null) {
				r = new Config().json();
				todos.add(c.id, r);
			}
			if (clave.equals("mostrar"))
				r.addProperty("mostrar", Boolean.parseBoolean(valor));
			else
				r.addProperty(clave, valor);
			ConfigArchivo.guardar("mensajes", todos);
		}

		private JsonObject json() {
			JsonObject r = new JsonObject();
			r.addProperty("titulo", titulo);
			r.addProperty("texto", texto);
			r.addProperty("mostrar", mostrar);
			return r;
		}

		private static JsonObject valoresDelPack() {
			JsonObject todos = new JsonObject();
			for (RuletaColor c : RuletaColor.VALUES)
				todos.add(c.id, new Config().json());
			return todos;
		}

		private static void guardarTodos() throws IOException {
			ConfigArchivo.guardar("mensajes", valoresDelPack());
		}
	}

	private record Estilo(String nombre, ChatFormatting texto, int titulo, String fuente) {
	}
}
