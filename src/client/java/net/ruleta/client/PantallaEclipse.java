package net.ruleta.client;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.ruleta.MomentoPayload;
import net.ruleta.Momentos;
import net.ruleta.Tiempo;

/** Configuración del Momento Reviil «Eclipse»: las 4 ruletas y los tiempos entre ellas. */
public class PantallaEclipse extends Screen {
	private static final String[] PISTAS = {"01:00", "01:00", "01:00", "05:00", "02:30"};
	private static final int HUECO = 60;
	private final Screen padre;
	private final EditBox[] cajas = new EditBox[5];
	private Button aceptar;
	private Button botonBunkers;
	private String bunker = "";
	private int cardW;
	private int top;
	private int alto;

	public PantallaEclipse(Screen padre) {
		super(Component.literal("Momento Reviil — Eclipse"));
		this.padre = padre;
	}

	private EditBox caja(int x, int y, int ancho, String pista, String antes) {
		EditBox e = new EditBox(this.font, x, y, ancho, 18, Component.literal("Tiempo"));
		e.setMaxLength(8);
		e.setFilter(t -> t.matches("[0-9:]*"));
		e.setHint(Component.literal(pista));
		e.setValue(antes == null ? "" : antes);
		return this.addRenderableWidget(e);
	}

	private int cardX(int i) {
		return 10 + i * (this.cardW + HUECO);
	}

	@Override
	protected void init() {
		Fondo.poner();
		String[] antes = new String[5];
		for (int i = 0; i < 5; i++)
			antes[i] = this.cajas[i] == null ? "" : this.cajas[i].getValue();

		this.cardW = (this.width - 20 - 180) / 4;

		int lineasMax = 0;
		int interior = (int) ((float) (this.cardW - 12) / 0.75F);
		for (int i = 0; i < 4; i++) {
			String texto = Momentos.ECLIPSE_TEXTOS[i].replace("{bunker}", "X: ? // Z: ? (las del bunker)");
			int n = this.font.split(Component.literal(Momentos.ECLIPSE_TITULOS[i]).withStyle(ChatFormatting.BOLD), interior).size()
					+ this.font.split(Component.literal(texto), interior).size();
			lineasMax = Math.max(lineasMax, n);
		}

		this.alto = Math.max(60, Math.round((float) (15 + lineasMax * 10) * 0.75F) + 14);
		int bloque = 30 + this.alto + 6 + 18 + 14 + 18 + 12 + 20;
		this.top = Math.max(34, (this.height - bloque) / 2 + 30);
		int mitad = this.top + this.alto / 2;
		for (int i = 0; i < 3; i++) {
			int x = this.cardX(i) + this.cardW + 5;
			this.cajas[i] = this.caja(x, mitad + 10, 50, PISTAS[i], antes[i]);
		}

		int x4 = this.cardX(3);
		int y4 = this.top + this.alto + 6;
		this.botonBunkers = this.addRenderableWidget(Button.builder(Component.literal(PantallaBunkers.texto(this.bunker)),
				b -> Minecraft.getInstance().setScreen(new PantallaBunkers(this, this.bunker, k -> this.bunker = k))).bounds(x4, y4, this.cardW, 18).build());
		int mitadW = (this.cardW - 4) / 2;
		this.cajas[3] = this.caja(x4, y4 + 32, mitadW, PISTAS[3], antes[3]);
		this.cajas[4] = this.caja(x4 + mitadW + 4, y4 + 32, mitadW, PISTAS[4], antes[4]);

		int cx = this.width / 2;
		int yb = y4 + 32 + 18 + 12;
		this.aceptar = this.addRenderableWidget(Button.builder(Component.literal("ACTIVAR"), b -> this.activar()).bounds(cx - 105, yb, 100, 20).build());
		this.addRenderableWidget(Button.builder(Component.literal("CANCELAR"), b -> this.onClose()).bounds(cx + 5, yb, 100, 20).build());
	}

	private boolean valido() {
		for (EditBox e : this.cajas) {
			String v = e.getValue().trim();
			if (!v.isEmpty() && !LanzadorFormulario.tiempoValido(v))
				return false;
		}
		return true;
	}

	private int segundos(int i) {
		String v = this.cajas[i].getValue().trim();
		return Tiempo.leer(v.isEmpty() ? PISTAS[i] : v);
	}

	private void activar() {
		if (!this.valido())
			return;
		String extra = this.segundos(0) + ";" + this.segundos(1) + ";" + this.segundos(2) + ";" + this.segundos(3) + ";" + this.segundos(4) + ";" + this.bunker;
		RedCliente.enviar(new MomentoPayload("eclipse", true, 0, 0, extra));
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
		this.botonBunkers.setMessage(Component.literal(PantallaBunkers.texto(this.bunker)));
		g.drawCenteredString(this.font, Component.literal("MOMENTO REVIIL — ECLIPSE: 1 + 2 + 3 + 4").withStyle(ChatFormatting.RED, ChatFormatting.BOLD), this.width / 2, this.top - 26, 16777215);
		g.drawCenteredString(this.font, "Cada contador entre tarjetas = el tiempo hasta que sale la siguiente ruleta (solo lo ves tú, los jugadores no).", this.width / 2, this.top - 14, 10066329);

		for (int i = 0; i < 4; i++)
			this.tarjeta(g, i);

		int mitad = this.top + this.alto / 2;
		for (int i = 0; i < 3; i++) {
			int cx = this.cardX(i) + this.cardW + 30;
			g.pose().pushPose();
			g.pose().translate((float) cx, (float) (mitad - 22), 0.0F);
			g.pose().scale(2.0F, 2.0F, 1.0F);
			g.drawCenteredString(this.font, "+", 0, 0, -43691);
			g.pose().popPose();
			this.pequeno(g, "siguiente", cx, mitad - 4, -4473925);
			this.pequeno(g, "ruleta en:", cx, mitad + 3, -4473925);
		}

		int x4 = this.cardX(3);
		int y4 = this.top + this.alto + 6;
		int mitadW = (this.cardW - 4) / 2;
		this.pequeno(g, "contador", x4 + mitadW / 2, y4 + 23, -4473925);
		this.pequeno(g, "cerrado", x4 + mitadW + 4 + mitadW / 2, y4 + 23, -4473925);
		super.render(g, mx, my, pt);
	}

	private void pequeno(GuiGraphics g, String t, int cx, int y, int color) {
		g.pose().pushPose();
		g.pose().translate((float) cx, (float) y, 0.0F);
		g.pose().scale(0.75F, 0.75F, 1.0F);
		g.drawCenteredString(this.font, t, 0, 0, color);
		g.pose().popPose();
	}

	private void tarjeta(GuiGraphics g, int i) {
		int x = this.cardX(i);
		int y = this.top;
		int w = this.cardW;
		g.fill(x, y, x + w, y + this.alto, -2084816);
		g.fill(x + 2, y + 2, x + w - 2, y + this.alto - 2, -872415232);
		float k = 0.75F;
		int interior = (int) ((float) (w - 12) / k);
		g.enableScissor(x + 2, y + 2, x + w - 2, y + this.alto - 2);
		g.pose().pushPose();
		g.pose().translate((float) (x + 6), (float) (y + 6), 0.0F);
		g.pose().scale(k, k, 1.0F);
		int ty = 0;
		g.drawString(this.font, (i + 1) + " · Ruleta Roja - Momento ReviiL", 0, ty, -43691);
		ty += 12;
		for (FormattedCharSequence l : this.font.split(Component.literal(Momentos.ECLIPSE_TITULOS[i]).withStyle(ChatFormatting.BOLD), interior)) {
			g.drawString(this.font, l, 0, ty, -22016);
			ty += 10;
		}
		ty += 3;
		String texto = Momentos.ECLIPSE_TEXTOS[i].replace("{bunker}", "X: ? // Z: ? (las del bunker)");
		List<FormattedCharSequence> lineas = this.font.split(Component.literal(texto), interior);
		for (FormattedCharSequence l : lineas) {
			g.drawString(this.font, l, 0, ty, -2039584);
			ty += 10;
		}
		g.pose().popPose();
		g.disableScissor();
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
