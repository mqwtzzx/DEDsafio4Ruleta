package net.ruleta.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** Lista paginada de jugadores conectados para elegir uno (o «TODOS»). */
public class JugadoresPantalla extends Screen {
	private final Screen padre;
	private final boolean conTodos;
	private final Consumer<String> alElegir;
	private int pagina;
	private static final int POR_PAGINA = 8;

	public JugadoresPantalla(Screen padre, boolean conTodos, Consumer<String> alElegir) {
		super(Component.literal("Jugadores"));
		this.padre = padre;
		this.conTodos = conTodos;
		this.alElegir = alElegir;
	}

	private List<String> nombres() {
		List<String> out = new ArrayList<>();
		if (Minecraft.getInstance().getConnection() != null)
			for (PlayerInfo i : Minecraft.getInstance().getConnection().getOnlinePlayers())
				out.add(i.getProfile().getName());
		out.sort(String.CASE_INSENSITIVE_ORDER);
		return out;
	}

	@Override
	protected void init() {
		Fondo.poner();
		List<String> lista = new ArrayList<>();
		if (this.conTodos)
			lista.add("");
		lista.addAll(this.nombres());
		int paginas = Math.max(1, (lista.size() + POR_PAGINA - 1) / POR_PAGINA);
		this.pagina = Math.max(0, Math.min(this.pagina, paginas - 1));
		int w = Math.min(220, this.width - 40);
		int x = (this.width - w) / 2;
		int y = 40;
		for (int i = this.pagina * POR_PAGINA; i < Math.min(lista.size(), (this.pagina + 1) * POR_PAGINA); i++) {
			String nombre = lista.get(i);
			Component texto = nombre.isEmpty() ? Component.literal("TODOS").withStyle(ChatFormatting.YELLOW) : Component.literal(nombre);
			this.addRenderableWidget(Button.builder(texto, b -> {
				this.alElegir.accept(nombre);
				this.onClose();
			}).bounds(x, y, w, 20).build());
			y += 24;
		}
		int yb = this.height - 28;
		Button ant = this.addRenderableWidget(Button.builder(Component.literal("<"), b -> {
			this.pagina--;
			this.rebuildWidgets();
		}).bounds(this.width / 2 - 50, yb, 20, 20).build());
		Button sig = this.addRenderableWidget(Button.builder(Component.literal(">"), b -> {
			this.pagina++;
			this.rebuildWidgets();
		}).bounds(this.width / 2 + 30, yb, 20, 20).build());
		ant.active = this.pagina > 0;
		sig.active = this.pagina < paginas - 1;
		this.addRenderableWidget(new LanzadorFormulario.BotonVolver(8, 8, 22, 20, this::onClose, "Volver"));
	}

	/** Screen.render de 1.21.1 ya pinta el fondo: se deja vacío y se pinta como en 1.20.1 en render(). */
	@Override
	public void renderBackground(GuiGraphics g, int mx, int my, float pt) {
	}

	@Override
	public void render(GuiGraphics g, int mx, int my, float pt) {
		Fondo.oscurecer(this, g);
		g.drawCenteredString(this.font, Component.literal("ELIGE UN JUGADOR").withStyle(ChatFormatting.BOLD), this.width / 2, 18, 16777215);
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
