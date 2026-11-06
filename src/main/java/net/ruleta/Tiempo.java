package net.ruleta;

import java.util.Locale;

/** Lee un tiempo escrito como "mm:ss", "hh:mm:ss", "30s" o minutos ("1.5"). Devuelve segundos o -1. */
public final class Tiempo {
	private Tiempo() {
	}

	public static int leer(String txt) {
		String t = txt.trim().toLowerCase(Locale.ROOT);
		try {
			if (t.contains(":")) {
				String[] p = t.split(":");
				if (p.length == 2)
					return Math.max(0, Integer.parseInt(p[0].trim()) * 60 + Integer.parseInt(p[1].trim()));
				return p.length == 3
						? Math.max(0, Integer.parseInt(p[0].trim()) * 3600 + Integer.parseInt(p[1].trim()) * 60 + Integer.parseInt(p[2].trim()))
						: -1;
			}
			return t.endsWith("s")
					? Math.max(0, Integer.parseInt(t.substring(0, t.length() - 1).trim()))
					: Math.max(0, Math.round(Float.parseFloat(t) * 60.0F));
		} catch (NumberFormatException ex) {
			return -1;
		}
	}
}
