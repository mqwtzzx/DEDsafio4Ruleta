package net.ruleta;

import java.util.List;

/** Catálogo de los Momentos Reviil (identificadores, títulos y textos). */
public final class Momentos {
	public static final String GAS_ARRIBA = "gas_arriba";
	public static final String GAS_ABAJO = "gas_abajo";
	public static final String VIDA_OCULTA = "vida_oculta";
	public static final String SIN_REGENERACION = "sin_regeneracion";
	public static final String CREEPERS = "creepers";
	public static final String VIDA_OCULTA_CREEPERS = "vida_oculta_creepers";
	public static final String CEGUERA_CREEPERS = "ceguera_creepers";
	public static final String LUNA = "luna";
	public static final String CREEPERS_NEGATIVAS = "creepers_negativas";
	public static final String BUNKER_RAYO = "bunker_rayo";
	public static final String BUNKER_AGUJEROS = "bunker_agujeros";
	public static final String ECLIPSE = "eclipse";
	public static final String COORDS = "{bunker}";
	public static final String TITULO = "Momento Reviil";
	public static final String[] ECLIPSE_TITULOS = {
			"Momento ReviiL — Eclipse Solar",
			"Momento ReviiL — Eclipse Solar",
			"Momento ReviiL — Eclipse y Herobrine",
			"Momento ReviiL — Refugiarse en el Bunker"
	};
	public static final String[] ECLIPSE_TEXTOS = {
			"Se acerca un Eclipse Solar. Regresen a sus bases inmediatamente.",
			"El Eclipse se acerca. Les recomendamos mantenerse en la superficie y NO ir bajo tierra. Desconocemos los efectos del Eclipse.",
			"¡Ha llegado el Eclipse! Deben utilizar linternas para abrirse paso por el mundo. CUIDADO: Herobrine está entre nosotros. NO lo mires a los ojos o te atacará y te hará mucho daño. Puedes apuntarle con la Linterna para que desaparezca.",
			"¡ReviiL nos está observando y quiere lanzar un rayo mortal! Eón lo vio justo a tiempo y ha construido un Bunker para refugiarse de él. Antes que termine el contador TODOS deben estar dentro o morirán. {bunker}"
	};
	public static final List<Momento> TODOS = List.of(
			new Momento("gas_arriba", "Momento Reviil — Gas tóxico invisible (subir)", "Reviil libera un gas tóxico invisible en todas las dimensiones. Durante este evento, todos deben permanecer por encima de la capa 250 o recibirán daño continuo hasta morir.", true),
			new Momento("gas_abajo", "Momento Reviil — Gas tóxico invisible (bajar)", "¡Reviil ha liberado un gas tóxico invisible! Antes que termine el temporizador TODOS deben DESCENDER a una Capa negativa (desde la -1 hacia abajo), o recibirán daño hasta morir.", true),
			new Momento("vida_oculta", "Momento Reviil — Vida oculta", "Reviil lanzó un conjuro con la Ruleta. Hasta que termine el contador no podrán ver su vida.", false),
			new Momento("sin_regeneracion", "Momento Reviil — Sin regeneración natural", "Reviil lanzó un conjuro con la Ruleta. Hasta que termine el contador no se puede regenerar vida de manera natural.", false),
			new Momento("creepers", "Momento Reviil — Lluvia de Creepers Eléctricos", "¡Reviil está haciendo llover Creepers Eléctricos! En un momento aleatorio y en más de una ocasión, puede aparecer un Creeper Eléctrico con 1 de Vida en tu posición.", false),
			new Momento("vida_oculta_creepers", "Momento Reviil — Vida oculta y Creepers Eléctricos", "Reviil lanzó un conjuro con la Ruleta. Hasta que termine el contador, no podrán ver su vida. Además, en un momento aleatorio y en más de una ocasión, puede aparecer un Creeper Eléctrico con 1 de Vida en tu posición.", false),
			new Momento("ceguera_creepers", "Momento Reviil — Ceguera y Creepers Eléctricos", "Reviil lanzó un conjuro con la Ruleta y dio ceguera por unos momentos! Además, en un momento aleatorio, y en más de una ocasión, puede aparecer un Creeper Eléctrico con 1 de Vida en tu posición.", false),
			new Momento("luna", "Momento Reviil — Maldición de la Luna", "¡Vaya horrible noche para tener una maldición! El primer jugador en mirar a la Luna recibirá daño instantáneo y morirá. Este cambio permanecerá hasta que alguien muera por esa razón. Luego será desactivado.", false),
			new Momento("creepers_negativas", "Momento Reviil — Creepers Eléctricos en Capas Negativas", "Reviil lanzó un conjuro con la Ruleta. En cualquier momento y de manera aleatoria aparecerán Creepers Eléctricos con 1 de Vida en tu posición. También activo: Deben estar en Capas Negativas.", true),
			new Momento("bunker_rayo", "Momento ReviiL — Rayo mortal y Búnker", "¡Reviil nos está observando y quiere lanzar un rayo mortal! Eón construyó un Búnker para salvarnos. Todos deben estar DENTRO antes que termine el contador o recibirán daño hasta morir. Diríjanse a {bunker}.", true),
			new Momento("bunker_agujeros", "Momento ReviiL — Agujeros Negros", "¡No puede ser! ¡Reviil comprimió más de 700 millones de toneladas de Materia Roja y creó Agujeros Negros! Debemos llegar al Búnker creado por Eón antes que termine el contador para evitar ser absorbidos. Quien no esté dentro al finalizar el tiempo recibirá daño hasta morir. El Nether ha sido desactivado. Diríjanse a {bunker}.", true),
			new Momento("eclipse", "Momento ReviiL — Eclipse Solar", "Se acerca un Eclipse Solar. Regresen a sus bases inmediatamente.", false));

	private Momentos() {
	}

	public static Momento porId(String id) {
		for (Momento m : TODOS)
			if (m.id().equals(id))
				return m;
		return null;
	}

	/** ¿Dos momentos no se pueden armar a la vez? */
	public static boolean chocan(String a, String b) {
		boolean arribaA = GAS_ARRIBA.equals(a);
		boolean arribaB = GAS_ARRIBA.equals(b);
		boolean abajoA = GAS_ABAJO.equals(a) || CREEPERS_NEGATIVAS.equals(a);
		boolean abajoB = GAS_ABAJO.equals(b) || CREEPERS_NEGATIVAS.equals(b);
		boolean bunkerA = BUNKER_RAYO.equals(a) || BUNKER_AGUJEROS.equals(a);
		boolean bunkerB = BUNKER_RAYO.equals(b) || BUNKER_AGUJEROS.equals(b);
		if (bunkerA && bunkerB && !a.equals(b) || bunkerA && (arribaB || abajoB) || bunkerB && (arribaA || abajoA))
			return true;
		return arribaA && abajoB || abajoA && arribaB;
	}

	public static boolean ocultaVida(String id) {
		return VIDA_OCULTA.equals(id) || VIDA_OCULTA_CREEPERS.equals(id);
	}

	public static boolean conCreepers(String id) {
		return CREEPERS.equals(id) || VIDA_OCULTA_CREEPERS.equals(id) || CEGUERA_CREEPERS.equals(id) || CREEPERS_NEGATIVAS.equals(id);
	}

	public record Momento(String id, String titulo, String texto, boolean temporizador) {
	}
}
