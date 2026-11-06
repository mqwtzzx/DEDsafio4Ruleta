package net.ruleta;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Catálogo de cambios de dificultad (ruleta naranja) y mobs nuevos (ruleta amarilla). */
public final class Cambios {
	public static final String ID_PERLA = "perla";
	public static final String ID_MAGMA = "magma";
	public static final String ID_CREEPERS = "creepers-electricos";
	public static final String ID_PERLA_ARMADURA = "perla-ignora-armadura";
	public static final String ID_WARDEN_SHRIEKER = "warden-sculk-shrieker";
	public static final String ID_RESPIRAR = "respirar-bajo-agua";
	public static final String ID_SIN_GOLEMS = "sin-golems";
	public static final String ID_SIN_LECHE = "sin-leche";
	public static final String ID_BOMBAS_WARDEN = "bombas-warden";
	public static final String ID_WARDEN_DEEP_DARK = "warden-deep-dark";
	public static final String ID_SIN_BROTES = "sin-brotes";
	public static final String ID_ZOMBIES_TREPADORES = "zombies-trepadores";
	public static final String ID_BLAZES_NETHER = "blazes-nether";
	public static final String ID_ARANAS_TELARANAS = "aranas-telaranas";
	public static final String ID_ZOMBIES_MEJORADOS = "zombies-mejorados";
	public static final String ID_ZOMBIES_NETHER = "zombies-nether";
	public static final String ID_BOGGED = "bogged-noche";
	public static final String ID_STRAY = "stray-overworld";
	public static final String ID_EXTINCION = "extincion-animales";
	public static final String ID_LAVA_FUEGO = "dano-lava-fuego";
	public static final String ID_WITHER_NETHER = "wither-skeleton-nether";
	public static final String ID_ARANAS_CUEVA = "aranas-cueva-overworld";
	public static final String ID_DANO_CAIDA = "dano-caida";
	public static final String ID_ARANAS_FUERZA = "aranas-fuerza";
	public static final String ID_NUCLEEPERS = "nucleepers-techo-nether";
	public static final String ID_INVISIBLES = "criaturas-detectan-invisibles";
	public static final String ID_ARANAS_TELARANA = "aranas-telarana";
	public static final String ID_FUEGO_SIN_ARMADURA = "fuego-sin-armadura-nether";
	public static final String ID_SIN_REGENERACION = "sin-regeneracion";
	public static final String ID_FUEGO_INFINITO = "fuego-infinito-nether";
	public static final String ID_FUEGO_LAVA_X10 = "dano-fuego-lava-x10";
	public static final String ID_BOTON = "dano-boton";
	public static final String ID_PLACA = "dano-placa";
	public static final String ID_SIN_ABSORCION = "sin-absorcion";
	public static final String ID_AHOGAMIENTO = "ahogamiento-aumentado";
	public static final String ID_PUERTAS = "dano-puertas";
	public static final String ID_ZOMBIES_PICO = "zombies-pico-diamante";
	public static final String ID_ENDERMITE = "endermite-100-dano";
	public static final String ID_ENDERMAN = "enderman-100-dano";
	public static final String ID_MIS_OJOS = "mis-ojos";
	public static final String ID_WITHER_NOCHE = "wither-skeleton-noche";
	public static final String ID_ARANAS_FUERZA_IV = "aranas-fuerza-iv";
	public static final String ID_PERLA_NUCLEEPER = "perla-creeper-nuclear";
	public static final String ID_ZOMBIES_INTELIGENTES = "zombies-inteligentes";
	public static final String ID_HEROBRINE = "herobrine";
	private static final String F1 = "21 sep 2026";
	private static final String F2 = "22 sep 2026";
	private static final String F3 = "23 sep 2026";
	private static final String F4 = "6 oct 2026";
	private static final String F5 = "7 oct 2026";
	private static final String F6 = "9 oct 2026";
	private static final String F7 = "12 oct 2026";
	private static final String F8 = "3 nov 2026";
	private static final List<Entrada> BLOQUE_1 = List.of(
			new Entrada("creepers-electricos", "21 sep 2026", "Cambio de dificultad — Creepers Eléctricos", "Reviil ha electrificado a los Creepers! Los Creepers Normales han sido modificados por Creepers Eléctricos. Con el equipamiento que tenemos, sólo podemos sobrevivir a su explosión utilizando un escudo."),
			new Entrada("perla-ignora-armadura", "21 sep 2026", "Ruleta naranja — Cambio de dificultad", "Reviil corrompió la composición de las Ender Pearls. A partir de ahora, su daño ignora la armadura."),
			new Entrada("warden-sculk-shrieker", "21 sep 2026", "Cambio de dificultad — Wardens ahora dejan Sculk Shrieker al morir", "Reviil corrompió el alma de los Wardens. A partir de ahora, al morir dejan un Sculk Shrieker."),
			new Entrada("respirar-bajo-agua", "21 sep 2026", "Cambio de dificultad — Respirar bajo el agua", "Reviil alteró las corrientes de agua. Las puertas y trampillas ya no pueden utilizarse para respirar bajo el agua."),
			new Entrada("sin-golems", "21 sep 2026", "Cambio de dificultad — Sin Gólems de Hierro ni de Nieve", "Reviil lanzó una maldición que impide que las calabazas cobren vida. Los Gólems de Hierro y de Nieve ya no pueden ser invocados."),
			new Entrada("sin-leche", "21 sep 2026", "Cambio de dificultad — Ya no se puede sacar leche a las vacas", "Reviil ordeña a todas las vacas y, a partir de ahora, los jugadores ya no pueden obtener leche de ellas."),
			new Entrada("bombas-warden", "21 sep 2026", "Cambio de dificultad — Bombas Warden Voladoras", "Reviil crea una nueva criatura: las Bombas Warden Voladoras, que ahora pueden aparecer naturalmente en el Deep Dark."),
			new Entrada("warden-deep-dark", "21 sep 2026", "Cambio de dificultad — Wardens en el Deep Dark", "Reviil altera la genética de los Wardens. A partir de ahora pueden aparecer naturalmente en el Deep Dark."),
			new Entrada("sin-brotes", "21 sep 2026", "Cambio de dificultad", "Los árboles dejan de soltar brotes, por lo que replantar ya no es una opción."));
	private static final List<Entrada> BLOQUE_2 = List.of(
			new Entrada("zombies-trepadores", "22 sep 2026", "Cambio de dificultad — Zombies trepadores", "Cuando los zombies aparecen hay un 50% de probabilidad de que puedan escalar paredes como las Arañas y sean inmunes al sol."),
			new Entrada("blazes-nether", "22 sep 2026", "Cambio de dificultad — Blazes en el Nether", "El Inframundo no descansa. Ahora los Blaze aparecen naturalmente en el Nether."),
			new Entrada("perla", "23 sep 2026", "Cambio de dificultad — Ender Pearls más dañinas", "Reviil ha utilizado el poder de la Ruleta para corromper la composición de las Ender Pearls. Ahora su daño aumentó a 10."),
			new Entrada("magma", "23 sep 2026", "Cambio de dificultad — Bloques de Magma letales", "El calor de las capas bajas es insoportable. Ahora los Bloques de Magma hacen siete mil millones de cuatrillones de daño. El daño también se aplica al tocar cualquiera de las caras del bloque vertical u horizontalmente."),
			new Entrada("aranas-telaranas", "23 sep 2026", "Cambio de dificultad — Arañas de Cueva al romper telarañas", "Al romper una Tela de Araña existe la probabilidad de que salga una Araña de Cueva."),
			new Entrada("zombies-mejorados", "23 sep 2026", "Cambio de dificultad — Zombies mejorados", "Reviil está experimentando con los Zombies. Ahora todos pueden escalar, no se queman con el sol y spawnean con una Espada de Hierro con Filo V."));
	private static final List<Entrada> BLOQUE_3 = List.of(
			new Entrada("zombies-nether", "", "Cambio de dificultad — Zombies en el Nether", "Ahora los Zombies aparecen en el Nether, sin importar la luz."),
			new Entrada("bogged-noche", "", "Cambio de dificultad — Esqueletos Bogged durante la noche", "A partir de este momento los Esqueletos Bogged (veneno) aparecen durante la noche."),
			new Entrada("stray-overworld", "", "Cambio de dificultad — Esqueletos Stray en el Overworld", "A partir de este momento los Esqueletos Stray pueden salir durante la noche en el Overworld."),
			new Entrada("extincion-animales", "", "Cambio de dificultad — Extinción de los animales", "Reviil ha lanzado un gas tóxico que afecta a los animales. Los animales fueron extintos. Ningún animal volverá a aparecer."),
			new Entrada("dano-lava-fuego", "", "Cambio de dificultad — Daño de Lava y Fuego", "La Lava y el Fuego han triplicado su daño."),
			new Entrada("wither-skeleton-nether", "", "Cambio de dificultad — Wither Skeleton en el Nether", "Los Wither Skeleton ahora aparecen naturalmente en el Nether."),
			new Entrada("herobrine", "3 nov 2026", "Cambio de dificultad — Herobrine", "Added Herobrine. Si lo ves, utiliza la Linterna para hacerlo desaparecer. NO lo mires a los ojos."));
	private static final List<Entrada> BLOQUE_4 = List.of(
			new Entrada("aranas-cueva-overworld", "", "Cambio de dificultad — Arañas de Cueva en el Overworld", "Ahora las Arañas de Cueva aparecen en el Overworld durante la Noche. Además, todas las arañas tienen Velocidad I infinita."),
			new Entrada("dano-caida", "", "Cambio de dificultad — Daño de Caída", "“El Daño de Caída ha sido multiplicado x2.”"),
			new Entrada("aranas-fuerza", "", "Cambio de dificultad — Arañas con Fuerza III", "Ahora las Arañas y Arañas de Cueva tienen Fuerza III."),
			new Entrada("nucleepers-techo-nether", "", "Cambio de dificultad — Creepers Nucleares en el Techo del Nether", "Los Creepers Nucleares ahora aparecen en el Techo del Nether. Además, pueden ver a través de las paredes y romper bloques para ir hacia ti."),
			new Entrada("criaturas-detectan-invisibles", "", "Cambio de dificultad — Criaturas detectan entidades invisibles", "Reviil ha hecho que todas las Criaturas desarrollen su olfato. Ahora pueden detectar la presencia de entidades invisibles. La invisibilidad ya no sirve para esconderse de los Mobs."),
			new Entrada("aranas-telarana", "", "Cambio de dificultad — Arañas dejan Tela de Araña", "Ahora las Arañas y Arañas de Cueva dejan una Tela de Araña al golpearte y al Morir."));
	private static final List<Entrada> BLOQUE_5 = List.of(
			new Entrada("fuego-sin-armadura-nether", "", "Cambio de dificultad — Fuego Infinito en el Nether sin Armadura", "Estar en el Nether sin CUALQUIERA de las Piezas de la Armadura te aplica Fuego Infinito."),
			new Entrada("sin-regeneracion", "", "Cambio de dificultad — Regeneración Natural desactivada", "La Regeneración Natural ha sido desactivada permanentemente."),
			new Entrada("fuego-infinito-nether", "", "Cambio de dificultad — Fuego Infinito al Quemarte en el Nether", "Si te quemas en el Nether el Fuego es Infinito."),
			new Entrada("dano-fuego-lava-x10", "", "Cambio de dificultad — Daño de Fuego y Lava multiplicado", "El daño del Fuego y de la Lava han sido multiplicados x10."),
			new Entrada("dano-boton", "", "Cambio de dificultad — Daño de Botón", "Interactuar con un Botón ahora te hace 4 mil millones de millones de millones de millones de daño."),
			new Entrada("dano-placa", "", "Cambio de dificultad — Daño de Placa de Presión", "Interactuar con una Placa de Presión ahora te hace 1 billón daño."));
	private static final List<Entrada> BLOQUE_6 = List.of(
			new Entrada("sin-absorcion", "6 oct 2026", "Cambio de dificultad — Sin Absorción", "La manzana de oro y la manzana encantada ya no dan el efecto de Absorción"),
			new Entrada("ahogamiento-aumentado", "6 oct 2026", "Cambio de dificultad — Ahogamiento aumentado", "Ahora pierdes la respiración 5 veces más rápido cuando te sumerges en agua y el daño por ahogamiento ha sido multiplicado x10"),
			new Entrada("dano-puertas", "6 oct 2026", "Cambio de dificultad — Daño al interactuar con puertas", "Interactuar con puertas te hace un CHINGO de daño (o sea te mata). Importante: Las Puertas con Código o de Hierro NO cuentan para este cambio."),
			new Entrada("zombies-pico-diamante", "7 oct 2026", "Cambio de dificultad — Zombies con Pico de Diamante", "Ahora los Zombies aparecen con Pico de Diamante... ¿Se estarán preparando para algo?"),
			new Entrada("endermite-100-dano", "7 oct 2026", "Cambio de dificultad — Endermite con 100 de daño", "Ahora los Endermite hacen 100 de daño al golpearte."));
	private static final List<Entrada> BLOQUE_7 = List.of(
			new Entrada("enderman-100-dano", "9 oct 2026", "Cambio de dificultad — Enderman con 100 de daño", "Ahora los Enderman hacen 100 de daño al golpearte."),
			new Entrada("mis-ojos", "9 oct 2026", "Cambio de Dificultad — Mis Ojos!", "Desde ahora, si miras al Sol tendrás Miopía y Daltonismo durante 3 minutos. Además no podrás ver tu vida durante ese tiempo."),
			new Entrada("wither-skeleton-noche", "9 oct 2026", "Cambio de Dificultad — Noches de Withers", "Ahora los Wither Skeleton aparecen naturalmente en la noche del Overworld"),
			new Entrada("aranas-fuerza-iv", "9 oct 2026", "Cambio de Dificultad — Fuerza Arácnida", "Ahora todas las Arañas ahora tienen Fuerza IV y las Arañas de Cueva ahora aparecen en el hielo sin importar la Luz"),
			new Entrada("perla-creeper-nuclear", "12 oct 2026", "Cambio de dificultad — Creeper Nuclear al usar Ender Pearl", "Al aterrizar una Ender Pearl existe una probabilidad del 1% de spawnear un Creeper Nuclear."),
			new Entrada("zombies-inteligentes", "12 oct 2026", "Cambio de dificultad — Zombies Inteligentes en la Capa 40", "Los Zombies inteligentes, que pueden romper y poner bloques, aparecen de Capa 40 hacia abajo naturalmente... ¿Estarán preparándose para algo?"));
	public static final String ID_MOB_NUCLEEPER_NIEVE = "mob-creeper-nuclear-nieve";
	public static final String ID_MOB_CREEPER_PASTO = "mob-creeper-pasto";
	public static final String ID_MOB_CREEPER_ROJO = "mob-creeper-rojo";
	public static final List<Entrada> MOBS_1 = List.of(
			new Entrada("mob-creeper-nuclear-nieve", "", "Nuevo mob — El Creeper Nuclear", "Se ha visto una nueva criatura enviada por Reviil! El Creeper Nuclear. Una criatura que provoca grandes explosiones. La ÚNICA manera de sobrevivir a su explosión es utilizando un Escudo. Sólo sale en biomas de Nieve. IMPORTANTE: Es totalmente real. Si estás incluso a 40 bloques y no te cubres con el escudo, podría matarte. PD: El escudo se romperá si te cubres para evitar el daño de la explosión."),
			new Entrada("mob-creeper-pasto", "", "Nuevo Mob — Creeper de Pasto", "Se ha visto una nueva criatura enviada por ReviiL: El Creeper de Pasto. Tiene probabilidad de salir al romperse: Bloques de Pasto, Pasto Alto, Pasto Bajo, Arbustos, entre otras hierbas. Son muy veloces y explotan al instante."),
			new Entrada("mob-creeper-rojo", "", "Nuevo Mob — Creeper Rojo con Efectos Desconocidos", "Creeper Rojo con Efectos Desconocidos. Al explotar aplica un efecto desconocido."));
	public static final List<List<Entrada>> PAGINAS_MOBS = List.of(MOBS_1);
	public static final List<List<Entrada>> PAGINAS = List.of(BLOQUE_1, BLOQUE_2, BLOQUE_3, BLOQUE_4, BLOQUE_5, BLOQUE_6, BLOQUE_7);

	private Cambios() {
	}

	public static Entrada porId(String id) {
		for (List<Entrada> pagina : PAGINAS_MOBS)
			for (Entrada e : pagina)
				if (e.id().equals(id))
					return e;
		for (List<Entrada> pagina : PAGINAS)
			for (Entrada e : pagina)
				if (e.id().equals(id))
					return e;
		return null;
	}

	public static Set<String> separar(String csv) {
		Set<String> out = new LinkedHashSet<>();
		if (csv != null) {
			for (String s : csv.split(",")) {
				String id = s.trim();
				if (!id.isEmpty())
					out.add(id);
			}
		}
		return out;
	}

	public static List<Entrada> elegidos(String csv) {
		List<Entrada> out = new ArrayList<>();
		for (String id : separar(csv)) {
			Entrada e = porId(id);
			if (e != null)
				out.add(e);
		}
		return out;
	}

	/** Une los textos en uno, con punto final entre medias si hace falta. */
	public static String unirTextos(List<Entrada> lista) {
		StringBuilder sb = new StringBuilder();
		for (Entrada e : lista) {
			if (sb.length() > 0) {
				char ultimo = sb.charAt(sb.length() - 1);
				if (ultimo != '.' && ultimo != '!' && ultimo != '?')
					sb.append('.');
				sb.append(' ');
			}
			sb.append(e.texto());
		}
		return sb.toString();
	}

	public record Entrada(String id, String fecha, String titulo, String texto) {
	}
}
