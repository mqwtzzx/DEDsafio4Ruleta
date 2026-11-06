package net.ruleta;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** config/ruleta.json: una sección por clave (antes en FMLPaths.CONFIGDIR; ahora la carpeta de config de Fabric). */
final class ConfigArchivo {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

	private ConfigArchivo() {
	}

	static Path ruta() {
		return FabricLoader.getInstance().getConfigDir().resolve("ruleta.json");
	}

	static JsonObject seccion(String clave) throws IOException {
		Path path = ruta();
		return !Files.exists(path) ? null : JsonParser.parseString(Files.readString(path, StandardCharsets.UTF_8)).getAsJsonObject().getAsJsonObject(clave);
	}

	static void guardar(String clave, JsonObject seccion) throws IOException {
		Path path = ruta();
		JsonObject root = new JsonObject();
		if (Files.exists(path)) {
			try {
				root = JsonParser.parseString(Files.readString(path, StandardCharsets.UTF_8)).getAsJsonObject();
			} catch (Exception ex) {
				root = new JsonObject();
			}
		}
		root.add(clave, seccion);
		Files.createDirectories(path.getParent());
		Files.writeString(path, GSON.toJson(root), StandardCharsets.UTF_8);
	}
}
