package net.ruleta.client;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.ruleta.Cambios;

/** Lista de cambios de dificultad (o de mobs nuevos) en tarjetas para elegir uno. */
public class CambiosPantalla extends Screen {
	private static final int ESPACIO = 6;
	private final LanzadorFormulario padre;
	private final List<Tarjeta> tarjetas = new ArrayList<>();
	private Button anterior;
	private Button siguiente;
	private int cardX;
	private int cardW;
	private int top;
	private int bottom;
	private int altoTotal;
	private double scroll;
	private static final int COLUMNAS = 3;
	private final List<List<Cambios.Entrada>> paginas;
	private final String encabezado;

	private record Tarjeta(Cambios.Entrada entrada, List<FormattedCharSequence> titulo, List<FormattedCharSequence> texto, int x, int y, int ancho, int alto) {
	}

	public CambiosPantalla(LanzadorFormulario padre) {
		this(padre, Cambios.PAGINAS, "CAMBIOS DE DIFICULTAD");
	}

	public CambiosPantalla(LanzadorFormulario padre, List<List<Cambios.Entrada>> paginas, String encabezado) {
		super(Component.literal(encabezado));
		this.padre = padre;
		this.paginas = paginas;
		this.encabezado = encabezado;
	}

	@Override
	protected void init() {
		Fondo.poner();
		this.padre.elegidos.removeIf(CambiosCliente::activo);
		this.cardW = Math.min(this.width - 40, 760);
		this.cardX = (this.width - this.cardW) / 2;
		this.top = 38;
		this.bottom = this.height - 36;
		this.padre.paginaCambios = Math.max(0, Math.min(this.padre.paginaCambios, this.paginas.size() - 1));
		this.addRenderableWidget(new LanzadorFormulario.BotonVolver(8, 8, 22, 20, this::onClose, "Volver"));
		int y = this.height - 28;
		this.anterior = this.addRenderableWidget(Button.builder(Component.literal("<"), b -> this.cambiarPagina(-1)).bounds(this.width / 2 - 50, y, 20, 20).build());
		this.siguiente = this.addRenderableWidget(Button.builder(Component.literal(">"), b -> this.cambiarPagina(1)).bounds(this.width / 2 + 30, y, 20, 20).build());
		this.armar();
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

	private void cambiarPagina(int delta) {
		int nueva = Math.max(0, Math.min(this.padre.paginaCambios + delta, this.paginas.size() - 1));
		if (nueva != this.padre.paginaCambios) {
			this.padre.paginaCambios = nueva;
			this.scroll = 0.0D;
			this.armar();
		}
	}

	private void armar() {
		this.tarjetas.clear();
		int col = (this.cardW - ESPACIO * 2) / COLUMNAS;
		int interior = col - 20;
		List<Cambios.Entrada> pagina = this.paginas.get(this.padre.paginaCambios);
		int y = 0;

		for (int fila = 0; fila * COLUMNAS < pagina.size(); fila++) {
			int altoFila = 0;
			List<Object[]> enFila = new ArrayList<>();
			for (int c = 0; c < COLUMNAS && fila * COLUMNAS + c < pagina.size(); c++) {
				Cambios.Entrada e = pagina.get(fila * COLUMNAS + c);
				List<FormattedCharSequence> titulo = this.font.split(this.titulo(e, "ELEGIDO", ChatFormatting.YELLOW), interior);
				List<FormattedCharSequence> texto = this.font.split(Component.literal(e.texto()), interior);
				altoFila = Math.max(altoFila, 6 + titulo.size() * 10 + 3 + texto.size() * 10 + 6);
				enFila.add(new Object[]{e, titulo, texto, c});
			}

			for (Object[] o : enFila) {
				@SuppressWarnings("unchecked")
				List<FormattedCharSequence> ti = (List<FormattedCharSequence>) o[1];
				@SuppressWarnings("unchecked")
				List<FormattedCharSequence> tx = (List<FormattedCharSequence>) o[2];
				int c = (Integer) o[3];
				this.tarjetas.add(new Tarjeta((Cambios.Entrada) o[0], ti, tx, this.cardX + c * (col + ESPACIO), y, col, altoFila));
			}
			y += altoFila + ESPACIO;
		}

		this.altoTotal = Math.max(0, y - ESPACIO);
		this.scroll = Math.max(0.0D, Math.min(this.scroll, (double) this.maxScroll()));
		this.anterior.active = this.padre.paginaCambios > 0;
		this.siguiente.active = this.padre.paginaCambios < this.paginas.size() - 1;
	}

	private int maxScroll() {
		return Math.max(0, this.altoTotal - (this.bottom - this.top));
	}

	private Component titulo(Cambios.Entrada e, String estado, ChatFormatting colorEstado) {
		MutableComponent out = Component.empty();
		if (estado != null)
			out.append(Component.literal(estado + " · ").withStyle(colorEstado, ChatFormatting.BOLD));
		return out.append(Component.literal(e.titulo()).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
	}

	private boolean bloqueada(Cambios.Entrada e) {
		return CambiosCliente.activo(e.id());
	}

	private boolean elegida(Cambios.Entrada e) {
		return this.padre.elegidos.contains(e.id());
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
					if (!this.bloqueada(t.entrada())) {
						String id = t.entrada().id();
						boolean estabaElegido = this.padre.elegidos.contains(id);
						this.padre.elegidos.clear();
						if (!estabaElegido)
							this.padre.elegidos.add(id);
						Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
					}
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
		this.padre.elegidos.removeIf(CambiosCliente::activo);
		Fondo.oscurecer(this, g);
		g.drawCenteredString(this.font, Component.literal(this.encabezado).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), this.width / 2, 15, 16777215);
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

		String pagina = (this.padre.paginaCambios + 1) + " / " + this.paginas.size();
		g.drawCenteredString(this.font, pagina, this.width / 2, this.height - 22, 16777215);
		super.render(g, mx, my, pt);
	}

	private void dibujar(GuiGraphics g, Tarjeta t, int y, int mx, int my) {
		Cambios.Entrada e = t.entrada();
		boolean activa = this.bloqueada(e);
		boolean hundida = activa || this.elegida(e);
		int x = t.x();
		int w = t.ancho();
		boolean encima = !activa && my >= this.top && my < this.bottom && mx >= x && mx < x + w && my >= y && my < y + t.alto();
		int borde = hundida ? -16777216 : encima ? -96 : -11184811;
		g.fill(x, y, x + w, y + t.alto(), borde);
		g.fill(x + 1, y + 1, x + w - 1, y + t.alto() - 1, hundida ? -301989888 : encima ? -869059789 : -1442840576);
		int tx = x + 10;
		int ty = y + 6 + (hundida ? 1 : 0);
		Component titulo = activa ? this.titulo(e, "ACTIVO", ChatFormatting.GREEN)
				: this.elegida(e) ? this.titulo(e, "ELEGIDO", ChatFormatting.YELLOW)
				: this.titulo(e, null, ChatFormatting.GRAY);

		for (FormattedCharSequence linea : this.font.split(titulo, w - 20)) {
			g.drawString(this.font, linea, tx, ty, -1);
			ty += 10;
		}

		ty += 3;
		int colorTexto = hundida ? -6250336 : -2039584;
		for (FormattedCharSequence linea : t.texto()) {
			g.drawString(this.font, linea, tx, ty, colorTexto);
			ty += 10;
		}
	}
}
