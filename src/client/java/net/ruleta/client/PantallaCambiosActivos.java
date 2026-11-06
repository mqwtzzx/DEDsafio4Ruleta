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
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.ruleta.Cambios;
import net.ruleta.DesactivarPayload;

/** Lista de los cambios de dificultad activos, con botón para desactivar el elegido. */
public class PantallaCambiosActivos extends Screen {
	private static final int ESPACIO = 6;
	private static final int COLUMNAS = 3;
	private Button desactivar;
	private String elegido;
	private double scroll;
	private int x0;
	private int ancho;
	private int top;
	private int bottom;
	private final List<int[]> cajas = new ArrayList<>();
	private final List<Cambios.Entrada> entradas = new ArrayList<>();

	public PantallaCambiosActivos() {
		super(Component.literal("Cambios activos"));
	}

	@Override
	protected void init() {
		this.ancho = Math.min(this.width - 40, 760);
		this.x0 = (this.width - this.ancho) / 2;
		this.top = 38;
		this.bottom = this.height - 40;
		this.desactivar = this.addRenderableWidget(Button.builder(Component.literal("Desactivar").withStyle(ChatFormatting.RED), b -> {
			if (this.elegido != null) {
				RedCliente.enviar(new DesactivarPayload(this.elegido));
				this.elegido = null;
			}
		}).bounds(this.width / 2 - 60, this.height - 30, 120, 20).build());
	}

	private void armar() {
		this.entradas.clear();
		this.cajas.clear();
		for (List<Cambios.Entrada> pagina : Cambios.PAGINAS) {
			for (Cambios.Entrada e : pagina) {
				if (CambiosCliente.activo(e.id()))
					this.entradas.add(e);
			}
		}
		for (List<Cambios.Entrada> pagina : Cambios.PAGINAS_MOBS) {
			for (Cambios.Entrada e : pagina) {
				if (CambiosCliente.activo(e.id()))
					this.entradas.add(e);
			}
		}
		for (String id : CambiosCliente.activos()) {
			if (Cambios.porId(id) == null)
				this.entradas.add(new Cambios.Entrada(id, "", id, ""));
		}

		int col = (this.ancho - ESPACIO * 2) / COLUMNAS;
		int y = 0;
		for (int i = 0; i < this.entradas.size(); i += COLUMNAS) {
			int alto = 0;
			for (int c = 0; c < COLUMNAS && i + c < this.entradas.size(); c++)
				alto = Math.max(alto, this.alto(this.entradas.get(i + c), col));
			for (int c = 0; c < COLUMNAS && i + c < this.entradas.size(); c++)
				this.cajas.add(new int[]{this.x0 + c * (col + ESPACIO), y, col, alto});
			y += alto + ESPACIO;
		}

		if (this.elegido != null && !CambiosCliente.activo(this.elegido))
			this.elegido = null;
		this.desactivar.visible = this.elegido != null;
	}

	private int alto(Cambios.Entrada e, int col) {
		return 6 + this.font.split(Component.literal(e.titulo()), col - 20).size() * 10 + 3 + this.font.split(Component.literal(e.texto()), col - 20).size() * 10 + 6;
	}

	/** 1.21.1: la rueda trae desplazamiento horizontal y vertical; se usa el vertical como el delta de 1.20.1. */
	@Override
	public boolean mouseScrolled(double mx, double my, double dx, double d) {
		this.scroll = Math.max(0.0D, this.scroll - d * 16.0D);
		return true;
	}

	@Override
	public boolean mouseClicked(double mx, double my, int boton) {
		if (boton == 0 && my >= (double) this.top && my < (double) this.bottom) {
			for (int i = 0; i < this.cajas.size(); i++) {
				int[] c = this.cajas.get(i);
				int y = this.top - (int) this.scroll + c[1];
				if (mx >= (double) c[0] && mx < (double) (c[0] + c[2]) && my >= (double) y && my < (double) (y + c[3])) {
					String id = this.entradas.get(i).id();
					this.elegido = id.equals(this.elegido) ? null : id;
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
		this.armar();
		Fondo.oscurecer(this, g);
		g.drawCenteredString(this.font, Component.literal("CAMBIOS DE DIFICULTAD ACTIVOS").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), this.width / 2, 15, 16777215);
		if (this.entradas.isEmpty()) {
			g.drawCenteredString(this.font, "No hay cambios de dificultad activos.", this.width / 2, this.height / 2 - 4, 11184810);
			super.render(g, mx, my, pt);
			return;
		}

		g.enableScissor(this.x0 - 2, this.top, this.x0 + this.ancho + 2, this.bottom);
		for (int i = 0; i < this.cajas.size(); i++) {
			int[] c = this.cajas.get(i);
			Cambios.Entrada e = this.entradas.get(i);
			int y = this.top - (int) this.scroll + c[1];
			boolean el = e.id().equals(this.elegido);
			boolean encima = mx >= c[0] && mx < c[0] + c[2] && my >= y && my < y + c[3] && my >= this.top && my < this.bottom;
			g.fill(c[0], y, c[0] + c[2], y + c[3], el ? -43691 : encima ? -96 : -11184811);
			g.fill(c[0] + 1, y + 1, c[0] + c[2] - 1, y + c[3] - 1, el ? -299892736 : encima ? -869059789 : -1442840576);
			int ty = y + 6;
			for (FormattedCharSequence l : this.font.split(Component.literal(e.titulo()).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), c[2] - 20)) {
				g.drawString(this.font, l, c[0] + 10, ty, 16777215);
				ty += 10;
			}
			ty += 3;
			for (FormattedCharSequence l : this.font.split(Component.literal(e.texto()), c[2] - 20)) {
				g.drawString(this.font, l, c[0] + 10, ty, 14737632);
				ty += 10;
			}
		}
		g.disableScissor();
		super.render(g, mx, my, pt);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
