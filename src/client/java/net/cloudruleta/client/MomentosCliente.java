package net.cloudruleta.client;

import net.cloudruleta.Momentos;

import java.util.LinkedHashSet;
import java.util.Set;

/** Momentos Reviil armados y en efecto según el servidor. */
public final class MomentosCliente {
	private static final Set<String> ARMADOS = new LinkedHashSet<>();
	private static final Set<String> EFECTO = new LinkedHashSet<>();

	private MomentosCliente() {
	}

	static void poner(String armados, String efecto) {
		ARMADOS.clear();
		EFECTO.clear();
		leer(armados, ARMADOS);
		leer(efecto, EFECTO);
	}

	private static void leer(String csv, Set<String> destino) {
		if (csv == null)
			return;
		for (String id : csv.split(","))
			if (!id.isBlank())
				destino.add(id.trim());
	}

	public static Set<String> armados() {
		return ARMADOS;
	}

	public static boolean activado(String id) {
		return ARMADOS.contains(id);
	}

	public static boolean enEfecto(String id) {
		return EFECTO.contains(id);
	}

	public static boolean vidaOculta() {
		for (String id : EFECTO)
			if (Momentos.ocultaVida(id))
				return true;
		return false;
	}

	static void limpiar() {
		ARMADOS.clear();
		EFECTO.clear();
	}
}
