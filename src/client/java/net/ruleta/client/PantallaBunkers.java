package net.ruleta.client;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.ruleta.bunker.Bunkers;
import net.ruleta.client.bunker.BunkerMarcador;

/** Elegir el bunker de un momento (ordenados por distancia al jugador). */
public class PantallaBunkers extends Screen {
	private final Screen padre;
	private final Consumer<String> alElegir;
	private final String actual;
	private final List<Bunkers.Bunker> lista = new ArrayList<>();
	private int scroll;
	private static final int FILA = 24;
	private static final int VISIBLES = 7;

	public PantallaBunkers(Screen padre, String actual, Consumer<String> alElegir) {
		super(Component.literal("Bunkers"));
		this.padre = padre;
		this.actual = actual == null ? "" : actual;
		this.alElegir = alElegir;
	}

	public static String texto(String clave) {
		for (Bunkers.Bunker b : BunkerMarcador.todos()) {
			if (b.clave().equals(clave))
				return "BUNKERS: " + etiqueta(b);
		}
		return "BUNKERS: al azar (o uno nuevo)";
	}

	private static double distancia(Bunkers.Bunker b) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || mc.level == null)
			return 0.0D;
		double d = Math.sqrt(mc.player.distanceToSqr(b.mx(), mc.player.getY(), b.mz()));
		return b.dimension().equals(mc.level.dimension().location().toString()) ? d : Double.MAX_VALUE;
	}

	private static String etiqueta(Bunkers.Bunker b) {
		double d = distancia(b);
		return b.nombre() + " — " + (d == Double.MAX_VALUE ? "otra dimensión" : Math.round(d) + " m");
	}

	@Override
	protected void init() {
		Fondo.poner();
		this.lista.clear();
		this.lista.addAll(BunkerMarcador.todos());
		this.lista.sort(Comparator.comparingDouble(PantallaBunkers::distancia));
		this.clearWidgets();
		int cx = this.width / 2;
		int y = 50;
		int ancho = Math.min(300, this.width - 40);
		int fin = Math.min(this.lista.size(), this.scroll + VISIBLES);

		for (int i = this.scroll; i < fin; i++) {
			Bunkers.Bunker b = this.lista.get(i);
			String t = (b.clave().equals(this.actual) ? "▶ " : "") + etiqueta(b);
			this.addRenderableWidget(Button.builder(Component.literal(t), x -> this.elegir(b.clave())).bounds(cx - ancho / 2, y, ancho, 20).build());
			y += FILA;
		}

		int yb = 224;
		if (this.lista.size() > VISIBLES) {
			this.addRenderableWidget(Button.builder(Component.literal("▲"), x -> this.mover(-7)).bounds(cx + ancho / 2 + 4, 50, 20, 20).build());
			this.addRenderableWidget(Button.builder(Component.literal("▼"), x -> this.mover(7)).bounds(cx + ancho / 2 + 4, 194, 20, 20).build());
		}

		this.addRenderableWidget(Button.builder(Component.literal((this.actual.isEmpty() ? "▶ " : "") + "AL AZAR (o uno nuevo si no hay)"), x -> this.elegir(""))
				.bounds(cx - ancho / 2, yb, ancho, 20).build());
		this.addRenderableWidget(Button.builder(Component.literal("VOLVER"), x -> this.onClose()).bounds(cx - 50, yb + 26, 100, 20).build());
	}

	private void mover(int d) {
		this.scroll = Math.max(0, Math.min(this.scroll + d, Math.max(0, this.lista.size() - VISIBLES)));
		this.init();
	}

	private void elegir(String clave) {
		this.alElegir.accept(clave);
		this.onClose();
	}

	/** Screen.render de 1.21.1 ya pinta el fondo: se deja vacío y se pinta como en 1.20.1 en render(). */
	@Override
	public void renderBackground(GuiGraphics g, int mx, int my, float pt) {
	}

	@Override
	public void render(GuiGraphics g, int mx, int my, float pt) {
		Fondo.oscurecer(this, g);
		g.drawCenteredString(this.font, Component.literal("BUNKERS").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), this.width / 2, 18, 16777215);
		g.drawCenteredString(this.font, this.lista.isEmpty()
				? "No hay ningún bunker: se construirá uno nuevo."
				: "Elige el bunker del momento (distancia a ti, el que lanza la ruleta)", this.width / 2, 32, 11184810);
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
