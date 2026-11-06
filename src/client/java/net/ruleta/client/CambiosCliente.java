package net.ruleta.client;

import net.ruleta.Cambios;
import net.ruleta.CambiosEfectos;

import java.util.Set;

/** Cambios de dificultad activos según el servidor (para lo que se decide en el cliente). */
public final class CambiosCliente {
	private static volatile Set<String> activos = Set.of();

	private CambiosCliente() {
	}

	public static Set<String> activos() {
		return activos;
	}

	public static boolean activo(String id) {
		return activos.contains(id);
	}

	static void poner(String csv) {
		activos = Set.copyOf(Cambios.separar(csv));
		CambiosEfectos.activosCliente = activos;
	}
}
