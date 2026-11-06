package net.ruleta.client;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import net.ruleta.Ruleta;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Opciones visuales del cliente (config/ruleta_cliente.json). */
public final class OpcionesCliente {
	public static boolean ruleta = true;
	public static boolean aviso = true;
	public static boolean temporizador = true;
	public static boolean reviilCielo = true;
	public static boolean animReviil = true;
	public static boolean animMuerte = true;
	public static boolean nutria = true;
	public static boolean coordenadas = true;

	private OpcionesCliente() {
	}

	/** FMLPaths.CONFIGDIR de Forge → carpeta config de Fabric Loader (la misma carpeta «config» del juego). */
	private static Path ruta() {
		return FabricLoader.getInstance().getConfigDir().resolve("ruleta_cliente.json");
	}

	public static void leer() {
		try {
			Path p = ruta();
			if (!Files.exists(p)) {
				guardar();
				return;
			}
			JsonObject o = JsonParser.parseString(Files.readString(p, StandardCharsets.UTF_8)).getAsJsonObject();
			ruleta = bool(o, "ver_ruleta", ruleta);
			aviso = bool(o, "ver_aviso", aviso);
			temporizador = bool(o, "ver_temporizador", temporizador);
			reviilCielo = bool(o, "ver_cielo_reviil", reviilCielo);
			animReviil = bool(o, "ver_animacion_reviil", animReviil);
			animMuerte = bool(o, "ver_animacion_muerte", animMuerte);
			nutria = bool(o, "ver_momento_nutria", nutria);
			coordenadas = bool(o, "ver_coordenadas", coordenadas);
		} catch (Exception ex) {
			Ruleta.LOG.warn("[opciones] no se pudo leer config/ruleta_cliente.json", ex);
		}
	}

	public static void guardar() {
		try {
			JsonObject o = new JsonObject();
			o.addProperty("ver_ruleta", ruleta);
			o.addProperty("ver_aviso", aviso);
			o.addProperty("ver_temporizador", temporizador);
			o.addProperty("ver_cielo_reviil", reviilCielo);
			o.addProperty("ver_animacion_reviil", animReviil);
			o.addProperty("ver_animacion_muerte", animMuerte);
			o.addProperty("ver_momento_nutria", nutria);
			o.addProperty("ver_coordenadas", coordenadas);
			Files.createDirectories(ruta().getParent());
			Files.writeString(ruta(), new GsonBuilder().setPrettyPrinting().create().toJson(o), StandardCharsets.UTF_8);
		} catch (Exception ex) {
			Ruleta.LOG.warn("[opciones] no se pudo guardar config/ruleta_cliente.json", ex);
		}
	}

	private static boolean bool(JsonObject o, String k, boolean siNo) {
		return o.has(k) ? o.get(k).getAsBoolean() : siNo;
	}
}
