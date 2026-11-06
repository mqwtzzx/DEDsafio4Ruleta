package net.ruleta.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.ruleta.Cambios;
import net.ruleta.LanzarPayload;
import net.ruleta.Mensajes;
import net.ruleta.Momentos;
import net.ruleta.RuletaColor;
import net.ruleta.Sanciones;
import net.ruleta.client.phora.PhoraCliente;
import net.ruleta.client.ruleta.RuletaHud;

/** Formulario para lanzar una ruleta (título, mensaje, destinatario, tamaño y extras según el color). */
public class LanzadorFormulario extends Screen {
	private static final int MAX_MENSAJE = 1000;
	private static final String[] TIPOS = {"MINI", "NORMAL"};
	private static final String[] CONTENIDOS = {"MENSAJE", "ANIMACIÓN", "CIELO"};

	private final RuletaColor color;
	private final boolean rojo;
	private boolean reviil;
	private boolean[] contenidoGuardado;
	private final boolean[] opciones = {true, false, false};
	private BotonInterruptor botonReviil;
	private boolean phora;
	private BotonInterruptor botonPhora;
	private final boolean morado;
	private int sancion;
	private String objetivo = "";
	private EditBox tiempoBan;
	private String tiempoBanGuardado = "";
	private BotonMC botonSancion;
	private BotonMC botonObjetivo;
	private String para = "";
	private BotonMC botonPara;
	private BotonMC botonMomentos;
	private BotonMC botonCambios;
	private final boolean conTemporizador;
	private final boolean naranja;
	private final boolean amarillo;
	final Set<String> elegidos = new LinkedHashSet<>();
	int paginaCambios;
	private EditBox temporizador;
	private String temporizadorGuardado = "";
	private int yTemporizador;
	private int tipo = 1;
	private EditBox titulo;
	private MultiLineEditBox mensaje;
	private BotonMC lanzar;
	private BotonMC[] botonesTipo;
	private BotonMC[] botonesContenido;
	private String momentosVistos;
	private String tituloPuesto = "";
	private String mensajePuesto = "";
	private String tituloGuardado = "";
	private String mensajeGuardado = "";
	private int xPanel;
	private int yPanel;
	private int anchoPanel;
	private int altoPanel;
	private int x0;
	private int ancho;
	private int xR;
	private int anchoR;
	private int yTop;
	private int yAbajo;
	private int yMensaje;
	private int yContador;
	private int ySumario;
	private final List<Etiqueta> etiquetas = new ArrayList<>();

	private record Etiqueta(String texto, int x, int y) {
	}

	public LanzadorFormulario(RuletaColor color) {
		super(Component.literal("Lanzar ruleta"));
		this.color = color;
		this.rojo = color == RuletaColor.ROJO;
		this.conTemporizador = color == RuletaColor.ROSA || color == RuletaColor.VERDE;
		this.morado = color == RuletaColor.MORADO;
		this.naranja = color == RuletaColor.NARANJA;
		this.amarillo = color == RuletaColor.AMARILLO;
	}

	static boolean tiempoValido(String s) {
		try {
			String[] p = s.trim().split(":", -1);
			int total;
			if (p.length == 1) {
				return Integer.parseInt(p[0]) > 0;
			} else if (p.length == 2) {
				int mm = Integer.parseInt(p[0]);
				int ss = Integer.parseInt(p[1]);
				if (ss > 59 || mm > 99)
					return false;
				total = mm * 60 + ss;
			} else if (p.length == 3) {
				int hh = Integer.parseInt(p[0]);
				int mm = Integer.parseInt(p[1]);
				int ss = Integer.parseInt(p[2]);
				if (ss > 59 || mm > 59 || hh > 24 || (hh == 24 && (mm > 0 || ss > 0)))
					return false;
				total = hh * 3600 + mm * 60 + ss;
			} else {
				return false;
			}
			return total > 0;
		} catch (NumberFormatException ex) {
			return false;
		}
	}

	@Override
	protected void init() {
		if (this.titulo != null)
			this.tituloGuardado = this.titulo.getValue();
		if (this.mensaje != null)
			this.mensajeGuardado = this.mensaje.getValue();
		if (this.temporizador != null)
			this.temporizadorGuardado = this.temporizador.getValue();
		if (this.tiempoBan != null)
			this.tiempoBanGuardado = this.tiempoBan.getValue();
		Fondo.poner();
		this.etiquetas.clear();

		int margen = 12;
		int hueco = 12;
		this.anchoPanel = Math.min(this.width - 24, 600);
		this.xPanel = (this.width - this.anchoPanel) / 2;
		this.altoPanel = Math.min(this.height - 16, 340);
		this.yPanel = (this.height - this.altoPanel) / 2;

		int xCont = this.xPanel + margen;
		int anchoCont = this.anchoPanel - margen * 2;
		this.ancho = (anchoCont - hueco) * 44 / 100;
		this.x0 = xCont;
		this.xR = xCont + this.ancho + hueco;
		this.anchoR = anchoCont - this.ancho - hueco;

		this.yTop = this.yPanel + 42;
		this.yAbajo = this.yPanel + this.altoPanel - margen;

		this.etiquetas.add(new Etiqueta("1 · TÍTULO", this.x0, this.yTop));
		int yTitulo = this.yTop + 11;
		this.titulo = new EditBox(this.font, this.x0, yTitulo, this.ancho, 20, Component.literal("Título"));
		this.titulo.setMaxLength(48);
		this.titulo.setHint(Component.literal("Título (opcional)"));
		this.titulo.setValue(this.tituloGuardado);
		this.addRenderableWidget(this.titulo);

		this.etiquetas.add(new Etiqueta("2 · MENSAJE", this.x0, yTitulo + 30));
		this.yMensaje = yTitulo + 41;
		int altoMensaje = Math.max(40, this.yAbajo - 14 - 28 - this.yMensaje);
		this.yContador = this.yMensaje + altoMensaje + 3;

		this.mensaje = new MultiLineEditBox(this.font, this.x0, this.yMensaje, this.ancho, altoMensaje, Component.literal("Escribe el mensaje..."), Component.literal("Mensaje"));
		this.mensaje.setValue(this.mensajeGuardado);
		this.addRenderableWidget(this.mensaje);

		int y = this.yTop;

		this.etiquetas.add(new Etiqueta("3 · ¿PARA QUIÉN?", this.xR, y));
		this.botonPara = this.addRenderableWidget(conAyuda(new BotonMC(this.xR, y + 11, this.anchoR, 20, this.textoPara(),
				() -> Minecraft.getInstance().setScreen(new JugadoresPantalla(this, true, nombre -> this.para = nombre))),
				"Elige un jugador: solo él verá esta ruleta y su mensaje"));
		y += 41;

		this.etiquetas.add(new Etiqueta("4 · TAMAÑO", this.xR, y));
		int gap = 6;
		int n = TIPOS.length;
		int bw = (this.anchoR - gap * (n - 1)) / n;
		int yTipo = y + 11;
		this.botonesTipo = new BotonMC[n];
		for (int i = 0; i < n; i++) {
			int idx = i;
			this.botonesTipo[i] = this.addRenderableWidget(new BotonMC(this.xR + i * (bw + gap), yTipo, bw, 20, TIPOS[i], () -> {
				this.tipo = idx;
				this.refrescar();
			}));
		}
		y = yTipo + 30;

		if (this.conTemporizador || this.rojo) {
			this.etiquetas.add(new Etiqueta(this.conTemporizador ? "5 · TEMPORIZADOR" : "5 · EXTRAS", this.xR, y));
			this.yTemporizador = y + 11;
			this.temporizador = new EditBox(this.font, this.xR, this.yTemporizador, 120, 20, Component.literal("Temporizador"));
			this.temporizador.setMaxLength(8);
			this.temporizador.setFilter(s -> s.matches("[0-9:]*"));
			this.temporizador.setHint(Component.literal("Sin temporizador"));
			this.temporizador.setValue(this.temporizadorGuardado);
			this.addRenderableWidget(this.temporizador);

			if (this.rojo) {
				int mitad = (this.anchoR - 6) / 2;
				this.botonMomentos = this.addRenderableWidget(conAyuda(new BotonMC(this.xR, this.yTemporizador, mitad, 20, this.textoMomentos(),
						() -> Minecraft.getInstance().setScreen(new MomentosPantalla(this))),
						"Activa o desactiva los Momentos Reviil (se pueden tener varios a la vez)"));
				this.botonPhora = this.addRenderableWidget(new BotonInterruptor(this.xR + mitad + 6, this.yTemporizador, this.anchoR - mitad - 6, 20,
						"MOMENTO PHORA", "MOMENTO PHORA: ACTIVO",
						"Lluvia de meteoritos de los Phora (cielo, música y todo). Mientras esté activo, las demás opciones quedan bloqueadas. Con temporizador, al acabar se acaba el momento; sin él, sigue hasta DETENER RULETA.",
						this.phora, v -> {
							this.phora = v;
							this.refrescar();
						}));
				this.botonPhora.colorEncendido = -2061794;
			}
			y = this.yTemporizador + 44;
		}

		if (this.amarillo) {
			this.etiquetas.add(new Etiqueta("5 · EXTRAS", this.xR, y));
			this.botonCambios = this.addRenderableWidget(conAyuda(new BotonMC(this.xR, y + 11, this.anchoR, 20, this.textoMobs(),
					() -> Minecraft.getInstance().setScreen(new CambiosPantalla(this, Cambios.PAGINAS_MOBS, "MOBS NUEVOS"))),
					"Elige el mob nuevo (permanente) que se lanza con esta ruleta. Solo se puede elegir uno"));
			y += 41;
		}

		if (this.naranja) {
			this.etiquetas.add(new Etiqueta("5 · EXTRAS", this.xR, y));
			this.botonCambios = this.addRenderableWidget(conAyuda(new BotonMC(this.xR, y + 11, this.anchoR, 20, this.textoCambios(),
					() -> Minecraft.getInstance().setScreen(new CambiosPantalla(this))),
					"Elige el cambio de dificultad (permanente) que se lanza con esta ruleta. Solo se puede elegir uno"));
			y += 41;
		}

		if (this.morado) {
			this.etiquetas.add(new Etiqueta("5 · CASTIGO", this.xR, y));
			this.yTemporizador = y + 11;
			this.botonSancion = this.addRenderableWidget(conAyuda(new BotonMC(this.xR, this.yTemporizador, this.anchoR, 20, this.textoSancion(), () -> {
				this.sancion = (this.sancion + 1) % 3;
				this.botonSancion.setMessage(Component.literal(this.textoSancion()));
				this.refrescar();
			}), "Castigo al jugador elegido, 10 segundos después del anuncio: BAN por el tiempo indicado o KICK (\"GG\")"));
			int mitad = (this.anchoR - 6) / 2;
			int yFila2 = this.yTemporizador + 26;
			this.botonObjetivo = this.addRenderableWidget(new BotonMC(this.xR, yFila2, mitad, 20, this.textoObjetivo(),
					() -> Minecraft.getInstance().setScreen(new JugadoresPantalla(this, false, nombre -> this.objetivo = nombre))));
			this.tiempoBan = new EditBox(this.font, this.xR + mitad + 6, yFila2, this.anchoR - mitad - 6, 20, Component.literal("Duración ban"));
			this.tiempoBan.setMaxLength(10);
			this.tiempoBan.setFilter(t -> t.matches("[0-9hmHM ]*"));
			this.tiempoBan.setHint(Component.literal("DURACIÓN (30m, 2h)"));
			this.tiempoBan.setValue(this.tiempoBanGuardado);
			this.addRenderableWidget(this.tiempoBan);
			y = yFila2 + 30;
		}

		if (this.rojo) {
			this.etiquetas.add(new Etiqueta("6 · QUÉ SALE", this.xR, y));
			int yReviil = y + 11;
			this.botonReviil = this.addRenderableWidget(new BotonInterruptor(this.xR, yReviil, this.anchoR, 20, "REVIIL: APAGADO", "REVIIL: ENCENDIDO",
					"Encendido: la ruleta roja lanza el Momento Reviil. Apagado: solo la ruleta roja con su mensaje.", this.reviil, v -> {
						this.reviil = v;
						this.refrescar();
					}));
			int cw = (this.anchoR - gap * 2) / 3;
			int yContenido = yReviil + 26;
			int nc = CONTENIDOS.length;
			this.botonesContenido = new BotonMC[nc];
			for (int i = 0; i < nc; i++) {
				int idx = i;
				this.botonesContenido[i] = this.addRenderableWidget(new BotonMC(this.xR + i * (cw + gap), yContenido, cw, 20, CONTENIDOS[i], () -> {
					this.opciones[idx] = !this.opciones[idx];
					this.refrescar();
				}));
			}
		}

		this.ySumario = this.yAbajo - 38;
		this.lanzar = this.addRenderableWidget(new BotonMC(this.xR, this.yAbajo - 22, this.anchoR, 22, "LANZAR", () -> this.enviar()));
		this.lanzar.verde = true;
		this.addRenderableWidget(new BotonVolver(this.xPanel + this.anchoPanel - 30, this.yPanel + 9, 20, 20,
				() -> Minecraft.getInstance().setScreen(new LanzadorRuleta())));
		this.refrescar();
		this.setInitialFocus(this.titulo);
	}

	private static BotonMC conAyuda(BotonMC b, String ayuda) {
		b.setTooltip(Tooltip.create(Component.literal(ayuda)));
		return b;
	}

	private String textoPara() {
		return this.para.isEmpty() ? "TODOS LOS JUGADORES" : "SOLO: " + this.para;
	}

	private void rellenarMomentos() {
		if (!this.rojo || this.titulo == null || this.mensaje == null)
			return;
		String firma = String.join(",", MomentosCliente.armados());
		if (firma.equals(this.momentosVistos))
			return;
		boolean primera = this.momentosVistos == null;
		this.momentosVistos = firma;
		if (primera && (!this.titulo.getValue().isBlank() || !this.mensaje.getValue().isBlank()))
			return;

		String t = "";
		StringBuilder x = new StringBuilder();
		for (String id : MomentosCliente.armados()) {
			Momentos.Momento mo = Momentos.porId(id);
			if (mo == null)
				continue;
			if (x.length() > 0)
				x.append(" ");
			x.append(mo.texto());
			t = "Momento Reviil";
		}

		if (x.length() == 0 && (!this.titulo.getValue().equals(this.tituloPuesto) || !this.mensaje.getValue().equals(this.mensajePuesto)))
			return;

		this.titulo.setValue(t);
		this.mensaje.setValue(x.toString());
		this.tituloPuesto = this.titulo.getValue();
		this.mensajePuesto = this.mensaje.getValue();
		if (x.length() > 0 && this.opciones != null && this.opciones.length > 0)
			this.opciones[0] = true;
	}

	private String textoMomentos() {
		int n = MomentosCliente.armados().size();
		return n == 0 ? "MOMENTOS REVIIL" : "MOMENTOS REVIIL (" + n + ")";
	}

	private String textoSancion() {
		return switch (this.sancion) {
			case 1 -> "CASTIGO: BAN";
			case 2 -> "CASTIGO: KICK (GG)";
			default -> "CASTIGO: NINGUNO";
		};
	}

	private String textoObjetivo() {
		return this.objetivo.isEmpty() ? "ELEGIR JUGADOR..." : this.objetivo;
	}

	private String textoMobs() {
		this.elegidos.removeIf(CambiosCliente::activo);
		return this.elegidos.isEmpty() ? "MOBS NUEVOS" : "MOBS NUEVOS (" + this.elegidos.size() + ")";
	}

	private String textoCambios() {
		this.elegidos.removeIf(CambiosCliente::activo);
		return this.elegidos.isEmpty() ? "CAMBIOS" : "CAMBIOS (" + this.elegidos.size() + ")";
	}

	private void refrescar() {
		for (int i = 0; i < this.botonesTipo.length; i++)
			this.botonesTipo[i].elegido = i == this.tipo;

		if (this.botonesContenido != null) {
			for (int i = 0; i < this.botonesContenido.length; i++)
				this.botonesContenido[i].elegido = this.opciones[i];
		}

		if (this.tiempoBan != null) {
			this.tiempoBan.visible = this.sancion == 1;
			this.tiempoBan.active = this.sancion == 1;
		}

		if (this.botonObjetivo != null)
			this.botonObjetivo.visible = this.sancion != 0;

		if (this.temporizador != null) {
			boolean ver = this.temporizadorVisible();
			this.temporizador.visible = ver;
			this.temporizador.active = ver;
		}

		if (this.rojo && this.botonMomentos != null) {
			this.botonMomentos.visible = !this.phora;
			if (this.temporizador != null)
				this.temporizador.setWidth(Math.min(120, this.botonMomentos.getWidth()));
		}
	}

	private boolean temporizadorVisible() {
		return this.conTemporizador || (this.rojo && this.phora);
	}

	private static String limpiar(String s) {
		return s == null ? "" : s.replace("\r", "").trim();
	}

	private String tiempoElegido() {
		return this.temporizadorVisible() ? limpiar(this.temporizador.getValue()) : "";
	}

	private LanzarPayload construir() {
		boolean conTexto = !this.rojo || this.opciones[0];
		String t = conTexto ? limpiar(this.titulo.getValue()) : "";
		String m = conTexto ? limpiar(this.mensaje.getValue()) : "";
		boolean animacion = this.rojo && this.opciones[1];
		boolean cielo = this.rojo && this.opciones[2];
		String cambios = this.naranja || this.amarillo ? String.join(",", this.elegidos) : "";
		int sancionElegida = this.morado ? this.sancion : 0;
		String tiempo = this.tiempoBan != null ? limpiar(this.tiempoBan.getValue()) : "";
		if (this.rojo && this.phora)
			return new LanzarPayload(this.color.ordinal(), this.tipo, false, false, t, m, this.tiempoElegido(), "", false, "", 0, "", "", true);
		return new LanzarPayload(this.color.ordinal(), this.tipo, animacion, this.rojo && this.reviil && this.para.isEmpty(), t, m, this.tiempoElegido(),
				cambios, cielo, this.para, sancionElegida, this.objetivo, tiempo);
	}

	private boolean tiempoOk() {
		String s = this.tiempoElegido();
		return s.isEmpty() || tiempoValido(s);
	}

	private void enviar() {
		if (this.lanzar.active) {
			RedCliente.enviar(this.construir());
			this.onClose();
		}
	}

	@Override
	public void removed() {
		Fondo.quitar();
	}

	private String nombreRuleta() {
		String nombre = Mensajes.encabezado(this.color);
		return nombre.endsWith(":") ? nombre.substring(0, nombre.length() - 1) : nombre;
	}

	private String resumen() {
		String alcance = this.para.isEmpty() ? "para todos" : "solo para " + this.para;
		if (this.rojo && this.phora)
			return "Sale: Momento Phora · " + alcance;
		if (this.rojo && (this.reviil || !MomentosCliente.armados().isEmpty()))
			return "Sale: Momento Reviil · " + alcance;
		String cuerpo = this.rojo ? this.resumenRoja() : this.tipo == 0 ? "mini" : "normal";
		return "Sale: " + this.nombreRuleta() + " " + cuerpo + " · " + alcance;
	}

	private String resumenRoja() {
		List<String> l = new ArrayList<>();
		if (this.opciones[0])
			l.add("mensaje");
		if (this.opciones[1])
			l.add("animación");
		if (this.opciones[2])
			l.add("cielo");
		return l.isEmpty() ? "solo la ruleta" : "con " + String.join(" + ", l);
	}

	private void dibujarPanel(GuiGraphics g) {
		int x = this.xPanel;
		int y = this.yPanel;
		int w = this.anchoPanel;
		int h = this.altoPanel;
		g.fill(x, y, x + w, y + h, -233564127);
		g.fill(x, y, x + w, y + 1, -14012356);
		g.fill(x, y + h - 1, x + w, y + h, -14012356);
		g.fill(x, y, x + 1, y + h, -14012356);
		g.fill(x + w - 1, y, x + w, y + h, -14012356);
		g.fill(x + 1, y + 1, x + w - 1, y + 36, -14999252);
		g.fill(x + 1, y + 36, x + w - 1, y + 37, -14012356);
		g.fill(x + 1, y + 36, x + 56, y + 37, 0xFF000000 | this.color.rgb);
		int sep = this.x0 + this.ancho + 6;
		g.fill(sep, this.yTop, sep + 1, this.yAbajo, -14012356);
	}

	static void caja(GuiGraphics g, int x, int y, int w, int h, int cara, int borde) {
		g.fill(x, y, x + w, y + h, borde);
		g.fill(x + 1, y + 1, x + w - 1, y + h - 1, cara);
	}

	/** Screen.render de 1.21.1 ya pinta el fondo: se deja vacío y se pinta como en 1.20.1 en render(). */
	@Override
	public void renderBackground(GuiGraphics g, int mx, int my, float pt) {
	}

	@Override
	public void render(GuiGraphics ctx, int mx, int my, float pt) {
		Fondo.oscurecer(this, ctx);
		this.dibujarPanel(ctx);

		RuletaHud.icono(ctx, this.x0, this.yPanel + 8, 22, this.color);
		ctx.flush();
		ctx.drawString(this.font, Component.literal(this.nombreRuleta()).withStyle(Style.EMPTY.withBold(true).withColor(this.color.rgb)), this.x0 + 30, this.yPanel + 10, 16777215);
		ctx.drawString(this.font, "Rellena las secciones de izquierda a derecha y pulsa LANZAR", this.x0 + 30, this.yPanel + 22, -6250324);

		for (Etiqueta e : this.etiquetas)
			ctx.drawString(this.font, Component.literal(e.texto()).withStyle(ChatFormatting.BOLD), e.x(), e.y(), -6642766);

		int largo = limpiar(this.mensaje.getValue()).length();
		boolean pasa = largo > MAX_MENSAJE;
		boolean tiempoMal = !this.tiempoOk();
		boolean sancionMal = this.morado && this.sancion != 0
				&& (this.objetivo.isEmpty() || (this.sancion == 1 && Sanciones.leerTiempo(this.tiempoBan.getValue()) <= 0L));
		this.lanzar.active = !pasa && !tiempoMal && !sancionMal;

		boolean conSancion = this.morado && this.sancion != 0;
		if (conSancion && !this.para.isEmpty())
			this.para = "";

		boolean siempreTodos = this.color == RuletaColor.ROSA || this.naranja || this.amarillo;
		if (siempreTodos)
			this.para = "";

		if (this.botonPara != null) {
			this.botonPara.setMessage(Component.literal(this.textoPara()));
			this.botonPara.active = !(this.rojo && this.phora) && !conSancion && !siempreTodos;
		}

		if (this.botonMomentos != null)
			this.botonMomentos.setMessage(Component.literal(this.textoMomentos()));

		this.rellenarMomentos();

		boolean paraUno = !this.para.isEmpty();
		if (paraUno)
			this.elegidos.clear();

		if (this.botonCambios != null) {
			this.botonCambios.active = !paraUno;
			this.botonCambios.setMessage(Component.literal(this.amarillo ? this.textoMobs() : this.textoCambios()));
		}

		if (this.botonMomentos != null)
			this.botonMomentos.active = !paraUno;

		if (this.botonReviil != null) {
			boolean hayMomentos = !MomentosCliente.armados().isEmpty() && !paraUno;
			if (hayMomentos && !this.reviil) {
				this.reviil = true;
				this.botonReviil.forzar(true);
			}
			this.botonReviil.active = !paraUno && !hayMomentos && !this.phora;
		}

		if (this.botonPhora != null) {
			boolean enCurso = PhoraCliente.enCurso();
			if ((enCurso || paraUno) && this.phora) {
				this.phora = false;
				this.botonPhora.forzar(false);
				this.refrescar();
			}
			this.botonPhora.active = !enCurso && !paraUno;
			this.botonPhora.setTooltip(Tooltip.create(Component.literal(enCurso
					? "Ya hay un Momento Phora en curso: no se puede lanzar otro hasta que se termine."
					: paraUno
					? "El Momento Phora es para todos: quita el jugador de PARA para poder usarlo."
					: "Lluvia de meteoritos de los Phora (cielo, música y todo). Mientras esté activo, las demás opciones quedan bloqueadas. Con temporizador, al acabar se acaba el momento; sin él, sigue hasta DETENER RULETA.")));
		}

		if (this.rojo && this.phora) {
			if (this.botonesContenido != null) {
				for (BotonMC b : this.botonesContenido)
					b.active = false;
			}
			if (this.botonMomentos != null)
				this.botonMomentos.active = false;
		} else if (this.botonesContenido != null) {
			boolean momentos = !MomentosCliente.armados().isEmpty() && !paraUno;
			boolean luna = momentos && MomentosCliente.activado("luna");
			boolean bloquear = (this.reviil || momentos) && !paraUno;
			if (bloquear) {
				boolean[] forzado = {true, true, !luna};
				if (this.contenidoGuardado == null)
					this.contenidoGuardado = this.opciones.clone();
				if (!Arrays.equals(this.opciones, forzado)) {
					System.arraycopy(forzado, 0, this.opciones, 0, 3);
					this.refrescar();
				}
			} else if (this.contenidoGuardado != null) {
				System.arraycopy(this.contenidoGuardado, 0, this.opciones, 0, 3);
				this.contenidoGuardado = null;
				this.refrescar();
			}
			for (int i = 0; i < this.botonesContenido.length; i++)
				this.botonesContenido[i].active = !bloquear;
		}

		if (this.botonObjetivo != null)
			this.botonObjetivo.setMessage(Component.literal(this.textoObjetivo()));

		String contador = largo + "/1000";
		ctx.drawString(this.font, contador, this.x0 + this.ancho - this.font.width(contador), this.yContador, pasa ? -43691 : -7697770);
		if (pasa)
			ctx.drawString(this.font, "Demasiado largo: acorta el texto", this.x0, this.yContador, -43691);

		if (this.temporizadorVisible()) {
			String vacio = this.rojo && this.phora ? "vacío = hasta DETENER RULETA" : "vacío = sin temporizador";
			String ayuda = tiempoMal ? "Usa 59:59 o hasta 24:00:00" : "mm:ss o hh:mm:ss (" + vacio + ")";
			ctx.drawString(this.font, ayuda, this.xR, this.yTemporizador + 24, tiempoMal ? -43691 : -7697770);
		}

		ctx.fill(this.xR, this.ySumario - 6, this.xR + this.anchoR, this.ySumario - 5, -14012356);
		String resumen = this.resumen();
		Font f = this.font;
		if (f.width(resumen) > this.anchoR)
			resumen = f.plainSubstrByWidth(resumen, this.anchoR - f.width("...")) + "...";
		ctx.drawString(this.font, resumen, this.xR, this.ySumario, -3616805);

		super.render(ctx, mx, my, pt);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	/** Botón con el aspecto del mod (texto que se desplaza si no cabe). */
	static class BotonMC extends AbstractButton {
		private final Runnable alPulsar;
		boolean verde;
		boolean elegido;
		private long desde = -1L;

		BotonMC(int x, int y, int w, int h, String texto, Runnable alPulsar) {
			super(x, y, w, h, Component.literal(texto));
			this.alPulsar = alPulsar;
		}

		@Override
		public void onPress() {
			this.alPulsar.run();
		}

		@Override
		public void renderWidget(GuiGraphics g, int mx, int my, float pt) {
			int x = this.getX();
			int y = this.getY();
			int w = this.width;
			int h = this.height;
			boolean sobre = this.isHoveredOrFocused();
			int cara;
			int borde;
			int color;
			if (!this.active) {
				cara = -15065562;
				borde = -14275786;
				color = -10788240;
			} else if (this.elegido) {
				cara = sobre ? -14399110 : -14796193;
				borde = -12877066;
				color = -1;
			} else if (this.verde) {
				cara = sobre ? -12877066 : -14326805;
				borde = -10443270;
				color = -1;
			} else {
				cara = sobre ? -13683132 : -14275786;
				borde = sobre ? -10721928 : -12959408;
				color = -1644049;
			}
			caja(g, x, y, w, h, cara, borde);

			Font f = Minecraft.getInstance().font;
			String texto = this.getMessage().getString();
			int area = w - 8;
			int tw = f.width(texto);
			int ty = y + (h - 8) / 2;
			if (tw <= area) {
				g.drawString(f, texto, x + (w - tw) / 2, ty, color);
			} else if (!sobre) {
				this.desde = -1L;
				g.drawString(f, f.plainSubstrByWidth(texto, area - f.width("...")) + "...", x + 4, ty, color);
			} else {
				long ahora = Util.getMillis();
				if (this.desde < 0L)
					this.desde = ahora;
				float ciclo = (float) (tw + 28);
				float off = (float) (ahora - this.desde) / 1000.0F * 34.0F % ciclo;
				int inicio = x + 4 - (int) off;
				g.enableScissor(x + 4, y, x + w - 4, y + h);
				g.drawString(f, texto, inicio, ty, color);
				g.drawString(f, texto, inicio + (int) ciclo, ty, color);
				g.disableScissor();
			}
		}

		@Override
		protected void updateWidgetNarration(NarrationElementOutput out) {
			this.defaultButtonNarrationText(out);
		}
	}

	/** Botón cuadrado con una flecha de píxeles para volver. */
	static class BotonVolver extends AbstractButton {
		private static final String[] FLECHA = {
				"..X......",
				".XX......",
				"XXXXXXX..",
				".XX....X.",
				"..X.....X",
				"........X",
				".......X.",
				"..XXXXX.."
		};
		private final Runnable alPulsar;

		BotonVolver(int x, int y, int w, int h, Runnable alPulsar) {
			this(x, y, w, h, alPulsar, "Volver a la ruleta");
		}

		BotonVolver(int x, int y, int w, int h, Runnable alPulsar, String ayuda) {
			super(x, y, w, h, Component.empty());
			this.alPulsar = alPulsar;
			this.setTooltip(Tooltip.create(Component.literal(ayuda)));
		}

		@Override
		public void onPress() {
			this.alPulsar.run();
		}

		@Override
		public void renderWidget(GuiGraphics g, int mx, int my, float pt) {
			boolean sobre = this.isHoveredOrFocused();
			caja(g, this.getX(), this.getY(), this.width, this.height, sobre ? -13683132 : -14275786, sobre ? -10721928 : -12959408);
			int cell = 2;
			int ox = this.getX() + (this.width - 9 * cell) / 2;
			int oy = this.getY() + (this.height - FLECHA.length * cell) / 2;

			for (int pase = 0; pase < 2; pase++) {
				int color = pase == 0 ? -15855338 : -1644049;
				int d = 1 - pase;
				for (int r = 0; r < FLECHA.length; r++) {
					for (int c = 0; c < 9; c++) {
						if (FLECHA[r].charAt(c) == 'X')
							g.fill(ox + c * cell + d, oy + r * cell + d, ox + (c + 1) * cell + d, oy + (r + 1) * cell + d, color);
					}
				}
			}
		}

		@Override
		protected void updateWidgetNarration(NarrationElementOutput out) {
			this.defaultButtonNarrationText(out);
		}
	}

	/** Botón de dos estados (encendido/apagado). */
	static class BotonInterruptor extends AbstractButton {
		private boolean activo;
		private final String textoApagado;
		private final String textoEncendido;
		private final Consumer<Boolean> alCambiar;
		int colorEncendido;

		BotonInterruptor(int x, int y, int w, int h, String textoApagado, String textoEncendido, String ayuda, boolean activo, Consumer<Boolean> alCambiar) {
			super(x, y, w, h, Component.empty());
			this.activo = activo;
			this.textoApagado = textoApagado;
			this.textoEncendido = textoEncendido;
			this.alCambiar = alCambiar;
			this.setTooltip(Tooltip.create(Component.literal(ayuda)));
		}

		@Override
		public void onPress() {
			this.activo = !this.activo;
			this.alCambiar.accept(this.activo);
		}

		void forzar(boolean v) {
			this.activo = v;
		}

		@Override
		public void renderWidget(GuiGraphics g, int mx, int my, float pt) {
			int x = this.getX();
			int y = this.getY();
			int w = this.width;
			int h = this.height;
			boolean sobre = this.isHoveredOrFocused();
			int cara;
			int borde;
			int color;
			if (this.activo && this.colorEncendido != 0) {
				cara = 0xFF000000 | (this.colorEncendido & 0xFFFFFF);
				borde = -16288;
				color = -1;
			} else if (this.activo) {
				cara = -14796193;
				borde = -12877066;
				color = -7092739;
			} else {
				cara = sobre ? -13683132 : -14275786;
				borde = sobre ? -10721928 : -12959408;
				color = sobre ? -1 : -1644049;
			}
			caja(g, x, y, w, h, cara, borde);

			Font f = Minecraft.getInstance().font;
			String texto = this.activo ? this.textoEncendido : this.textoApagado;
			if (f.width(texto) > w - 6)
				texto = f.plainSubstrByWidth(texto, w - 6 - f.width("...")) + "...";
			g.drawString(f, texto, x + (w - f.width(texto)) / 2, y + (h - 8) / 2 + (this.activo ? 1 : 0), color);
		}

		@Override
		protected void updateWidgetNarration(NarrationElementOutput out) {
			this.defaultButtonNarrationText(out);
		}
	}
}
