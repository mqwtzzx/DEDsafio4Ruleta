package net.ruleta.client;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.ruleta.MomentoPayload;
import net.ruleta.Momentos;
import net.ruleta.Tiempo;
import net.ruleta.client.eclipse.EclipseCliente;

/** Tarjetas de los Momentos Reviil para activarlos o desactivarlos. */
public class MomentosPantalla extends Screen {
	private static final int ESPACIO = 6;
	private final Screen padre;
	private final List<Tarjeta> tarjetas = new ArrayList<>();
	private int cardX;
	private int cardW;
	private int top;
	private int bottom;
	private int altoTotal;
	private double scroll;
	private static final int COLUMNAS = 3;

	private record Tarjeta(Momentos.Momento momento, List<FormattedCharSequence> texto, int x, int y, int ancho, int alto) {
	}

	public MomentosPantalla(Screen padre) {
		super(Component.literal("Momentos Reviil"));
		this.padre = padre;
	}

	@Override
	protected void init() {
		Fondo.poner();
		this.cardW = Math.min(this.width - 40, 760);
		this.cardX = (this.width - this.cardW) / 2;
		this.top = 38;
		this.bottom = this.height - 12;
		this.addRenderableWidget(new LanzadorFormulario.BotonVolver(8, 8, 22, 20, this::onClose, "Volver"));
		this.tarjetas.clear();
		this.altoTotal = 0;
		int col = (this.cardW - ESPACIO * 2) / COLUMNAS;
		int interior = col - 20;
		List<Momentos.Momento> todos = new ArrayList<>(Momentos.TODOS);
		int y = 0;

		for (int fila = 0; fila * COLUMNAS < todos.size(); fila++) {
			int altoFila = 0;
			List<List<FormattedCharSequence>> textos = new ArrayList<>();
			int n = Math.min(COLUMNAS, todos.size() - fila * COLUMNAS);
			for (int c = 0; c < n; c++) {
				List<FormattedCharSequence> texto = this.font.split(
						Component.literal(todos.get(fila * COLUMNAS + c).texto().replace("{bunker}", "las coordenadas del bunker")), interior);
				textos.add(texto);
				altoFila = Math.max(altoFila, 39 + texto.size() * 10 + 6);
			}
			for (int c = 0; c < n; c++)
				this.tarjetas.add(new Tarjeta(todos.get(fila * COLUMNAS + c), textos.get(c), this.cardX + c * (col + ESPACIO), y, col, altoFila));
			y += altoFila + ESPACIO;
		}

		this.altoTotal = Math.max(0, y - ESPACIO);
		this.scroll = Math.max(0.0D, Math.min(this.scroll, (double) this.maxScroll()));
	}

	private int maxScroll() {
		return Math.max(0, this.altoTotal - (this.bottom - this.top));
	}

	private static boolean bloqueado(Momentos.Momento m) {
		if (lunaFija(m))
			return true;
		if (MomentosCliente.activado(m.id()))
			return false;
		if (eclipseBloqueado(m) || eclipseSolo(m))
			return true;
		if ("creepers_negativas".equals(m.id()) && !MomentosCliente.enEfecto("gas_abajo"))
			return true;
		for (String otro : MomentosCliente.armados()) {
			if (Momentos.chocan(otro, m.id()))
				return true;
		}
		return false;
	}

	private static boolean eclipseSolo(Momentos.Momento m) {
		if ("eclipse".equals(m.id()))
			return !MomentosCliente.armados().isEmpty();
		return MomentosCliente.activado("eclipse") || EclipseCliente.activo();
	}

	private static boolean eclipseBloqueado(Momentos.Momento m) {
		return "eclipse".equals(m.id()) && !CambiosCliente.activo("herobrine");
	}

	private static boolean lunaFija(Momentos.Momento m) {
		return "luna".equals(m.id()) && MomentosCliente.enEfecto("luna");
	}

	private static boolean lanzado(Momentos.Momento m) {
		return MomentosCliente.activado(m.id()) && MomentosCliente.enEfecto(m.id());
	}

	/** 1.21.1: la rueda trae desplazamiento horizontal y vertical; se usa el vertical como el delta de 1.20.1. */
	@Override
	public boolean mouseScrolled(double mx, double my, double dx, double delta) {
		this.scroll = Math.max(0.0D, Math.min(this.scroll - delta * 16.0D, (double) this.maxScroll()));
		return true;
	}

	@Override
	public boolean mouseClicked(double mx, double my, int boton) {
		if (boton == 0 && my >= (double) this.top && my < (double) this.bottom && mx >= (double) this.cardX && mx < (double) (this.cardX + this.cardW)) {
			for (Tarjeta t : this.tarjetas) {
				int y = this.top - (int) this.scroll + t.y();
				if (my >= (double) y && my < (double) (y + t.alto()) && mx >= (double) t.x() && mx < (double) (t.x() + t.ancho())) {
					Momentos.Momento m = t.momento();
					if (lunaFija(m) || lanzado(m))
						return true;
					if (MomentosCliente.activado(m.id())) {
						RedCliente.enviar(new MomentoPayload(m.id(), false, 0));
					} else if (!bloqueado(m)) {
						if ("eclipse".equals(m.id()))
							Minecraft.getInstance().setScreen(new PantallaEclipse(this));
						else if (m.temporizador())
							Minecraft.getInstance().setScreen(new Temporizador(this, m));
						else
							RedCliente.enviar(new MomentoPayload(m.id(), true, 0));
					}
					Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
					return true;
				}
			}
		}
		return super.mouseClicked(mx, my, boton);
	}

	/** Screen.render de 1.21.1 ya pinta el fondo: se deja vacío y se pinta como en 1.20.1 en render(). */
	@Override
	public void renderBackground(GuiGraphics g, int mx, int my, float pt) {
	}

	@Override
	public void render(GuiGraphics g, int mx, int my, float pt) {
		Fondo.oscurecer(this, g);
		g.drawCenteredString(this.font, Component.literal("MOMENTOS REVIIL").withStyle(ChatFormatting.RED, ChatFormatting.BOLD), this.width / 2, 15, 16777215);
		g.enableScissor(this.cardX - 2, this.top, this.cardX + this.cardW + 2, this.bottom);
		for (Tarjeta t : this.tarjetas) {
			int y = this.top - (int) this.scroll + t.y();
			if (y + t.alto() >= this.top && y <= this.bottom)
				this.dibujar(g, t, y, mx, my);
		}
		g.disableScissor();

		int visible = this.bottom - this.top;
		if (this.altoTotal > visible) {
			int barra = Math.max(16, visible * visible / this.altoTotal);
			int by = this.top + (int) ((double) (visible - barra) * (this.scroll / (double) this.maxScroll()));
			g.fill(this.cardX + this.cardW + 5, this.top, this.cardX + this.cardW + 8, this.bottom, 1711276032);
			g.fill(this.cardX + this.cardW + 5, by, this.cardX + this.cardW + 8, by + barra, -5592406);
		}

		super.render(g, mx, my, pt);
	}

	private void dibujar(GuiGraphics g, Tarjeta t, int y, int mx, int my) {
		Momentos.Momento m = t.momento();
		boolean activo = MomentosCliente.activado(m.id());
		boolean bloq = bloqueado(m);
		int x = t.x();
		int w = t.ancho();
		boolean encima = !bloq && my >= this.top && my < this.bottom && mx >= x && mx < x + w && my >= y && my < y + t.alto();
		int borde = bloq ? -12303292 : activo ? -13705126 : -2084816;
		g.fill(x, y, x + w, y + t.alto(), borde);
		g.fill(x + 2, y + 2, x + w - 2, y + t.alto() - 2, encima ? -584439254 : -872415232);
		int tx = x + 10;
		int ty = y + 6;
		int colorEstado = bloq ? -8947849 : activo ? -12779670 : -49088;
		g.fill(tx, ty + 1, tx + 6, ty + 7, colorEstado);

		String estado = lunaFija(m)
				? "LANZADO · se libera cuando alguien mire la Luna"
				: bloq && "creepers_negativas".equals(m.id()) && !MomentosCliente.enEfecto("gas_abajo")
				? "BLOQUEADO · necesita el Gas tóxico (bajar) en curso"
				: null;
		if (estado == null && eclipseBloqueado(m))
			estado = "BLOQUEADO · primero activa el cambio de dificultad Herobrine";
		else if (estado == null && !activo && eclipseSolo(m))
			estado = "BLOQUEADO · el Eclipse va solo";
		if (estado == null && lanzado(m))
			estado = "eclipse".equals(m.id()) ? "ACTIVO · lanzado, hasta que acaben sus 4 Momentos Reviil" : "SELECCIONADO · ya lanzado";
		estado = estado != null ? estado
				: bloq ? "NO COMPATIBLE"
				: activo ? (MomentosCliente.enEfecto(m.id()) ? "ACTIVADO" : "ACTIVADO · empieza al salir el mensaje de la ruleta roja")
				: "DESACTIVADO";

		// se cuentan todas las líneas pero solo se pintan las dos primeras (en el original: lineas++ < 2)
		int lineas = 0;
		for (FormattedCharSequence l : this.font.split(Component.literal(estado).withStyle(ChatFormatting.BOLD), w - 30)) {
			if (lineas++ < 2) {
				g.drawString(this.font, l, tx + 10, ty, colorEstado);
				ty += 10;
			}
		}

		ty += Math.max(0, 2 - lineas) * 10;
		MutableComponent titulo = Component.literal(m.titulo()).withStyle(bloq ? ChatFormatting.DARK_GRAY : ChatFormatting.GOLD, ChatFormatting.BOLD);
		g.drawString(this.font, this.font.plainSubstrByWidth(titulo.getString(), w - 20), tx, ty, bloq ? -11184811 : -22016);
		ty += 13;
		int colorTexto = bloq ? -10066330 : -2039584;
		for (FormattedCharSequence linea : t.texto()) {
			g.drawString(this.font, linea, tx, ty, colorTexto);
			ty += 10;
		}
	}

	@Override
	public void removed() {
		Fondo.quitar();
	}

	@Override
	public void onClose() {
		Minecraft.getInstance().setScreen(this.padre);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	/** Pide el tiempo (y en los del bunker, el tiempo de puerta cerrada y el bunker) antes de activar un momento. */
	static class Temporizador extends Screen {
		private final MomentosPantalla padre;
		private final Momentos.Momento momento;
		private final boolean bunker;
		private EditBox tiempo;
		private EditBox cerrada;
		private Button aceptar;
		private Button botonBunkers;
		private String bunkerElegido = "";

		Temporizador(MomentosPantalla padre, Momentos.Momento momento) {
			super(Component.literal("Temporizador"));
			this.padre = padre;
			this.momento = momento;
			this.bunker = "bunker_rayo".equals(momento.id()) || "bunker_agujeros".equals(momento.id());
		}

		private EditBox caja(int x, int y, String pista) {
			EditBox e = new EditBox(this.font, x, y, 120, 20, Component.literal("Temporizador"));
			e.setMaxLength(8);
			e.setFilter(t -> t.matches("[0-9:]*"));
			e.setHint(Component.literal(pista));
			return this.addRenderableWidget(e);
		}

		@Override
		protected void init() {
			Fondo.poner();
			int cx = this.width / 2;
			int cy = this.height / 2;
			String antesT = this.tiempo == null ? "" : this.tiempo.getValue();
			String antesC = this.cerrada == null ? "" : this.cerrada.getValue();
			if (this.bunker) {
				this.tiempo = this.caja(cx - 60, cy - 30, "05:00");
				this.cerrada = this.caja(cx - 60, cy + 12, "02:30");
				this.tiempo.setValue(antesT);
				this.cerrada.setValue(antesC);
			} else {
				this.tiempo = this.caja(cx - 60, cy - 10, "05:00");
			}

			int yb = this.bunker ? cy + 40 : cy + 18;
			if (this.bunker) {
				this.botonBunkers = this.addRenderableWidget(Button.builder(Component.literal(PantallaBunkers.texto(this.bunkerElegido)),
						b -> Minecraft.getInstance().setScreen(new PantallaBunkers(this, this.bunkerElegido, k -> this.bunkerElegido = k)))
						.bounds(cx - 105, yb, 210, 20).build());
				yb += 26;
			}

			this.aceptar = this.addRenderableWidget(Button.builder(Component.literal("ACTIVAR"), b -> this.activar()).bounds(cx - 105, yb, 100, 20).build());
			this.addRenderableWidget(Button.builder(Component.literal("CANCELAR"), b -> this.onClose()).bounds(cx + 5, yb, 100, 20).build());
			this.setInitialFocus(this.tiempo);
		}

		private boolean valido() {
			return LanzadorFormulario.tiempoValido(this.tiempo.getValue().trim())
					&& (!this.bunker || LanzadorFormulario.tiempoValido(this.cerrada.getValue().trim()));
		}

		private void activar() {
			if (!this.valido())
				return;
			int t = Tiempo.leer(this.tiempo.getValue().trim());
			int c = this.bunker ? Tiempo.leer(this.cerrada.getValue().trim()) : 0;
			RedCliente.enviar(new MomentoPayload(this.momento.id(), true, t, c, this.bunker ? this.bunkerElegido : ""));
			this.onClose();
		}

		/** Screen.render de 1.21.1 ya pinta el fondo: se deja vacío y se pinta como en 1.20.1 en render(). */
		@Override
		public void renderBackground(GuiGraphics g, int mx, int my, float pt) {
		}

		@Override
		public void render(GuiGraphics g, int mx, int my, float pt) {
			Fondo.oscurecer(this, g);
			this.aceptar.active = this.valido();
			if (this.botonBunkers != null)
				this.botonBunkers.setMessage(Component.literal(PantallaBunkers.texto(this.bunkerElegido)));
			int cx = this.width / 2;
			int cy = this.height / 2;
			if (this.bunker) {
				g.drawCenteredString(this.font, Component.literal(this.momento.titulo()).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), cx, cy - 66, 16777215);
				g.drawCenteredString(this.font, "Tiempo para que lleguen al bunker (luego se cierra la puerta)", cx, cy - 44, 16777215);
				g.drawCenteredString(this.font, "Tiempo que la puerta estará cerrada (luego se abre)", cx, cy - 2, 16777215);
				g.drawCenteredString(this.font, "mm:ss o hh:mm:ss", cx, cy + 94, 8947848);
			} else {
				g.drawCenteredString(this.font, Component.literal(this.momento.titulo()).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), cx, cy - 48, 16777215);
				g.drawCenteredString(this.font, "TEMPORIZADOR: tiempo para que los jugadores lleguen a su capa", cx, cy - 30, 16777215);
				g.drawCenteredString(this.font, "mm:ss o hh:mm:ss. Al acabar empieza el daño del gas.", cx, cy + 46, 8947848);
			}
			super.render(g, mx, my, pt);
		}

		@Override
		public void removed() {
			Fondo.quitar();
		}

		@Override
		public void onClose() {
			Minecraft.getInstance().setScreen(this.padre);
		}

		@Override
		public boolean isPauseScreen() {
			return false;
		}
	}
}
