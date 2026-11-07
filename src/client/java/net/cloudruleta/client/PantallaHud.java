package net.cloudruleta.client;

import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.cloudruleta.compat.SubscribeEvent;
import net.cloudruleta.compat.client.RegisterClientCommandsEvent;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/** /hud: activar o desactivar lo que ve cada jugador. */
public class PantallaHud extends Screen {
	public PantallaHud() {
		super(Component.literal("HUD"));
	}

	@Override
	protected void init() {
		int w = 240;
		int x = (this.width - w) / 2;
		int y = this.height / 2 - 56;
		this.interruptor(x, y, w, "Animación del Momento Reviil", () -> OpcionesCliente.animReviil, v -> OpcionesCliente.animReviil = v);
		this.interruptor(x, y + 24, w, "Animación de muerte", () -> OpcionesCliente.animMuerte, v -> OpcionesCliente.animMuerte = v);
		this.interruptor(x, y + 48, w, "Momento Nutria", () -> OpcionesCliente.nutria, v -> OpcionesCliente.nutria = v);
		this.interruptor(x, y + 72, w, "Coordenadas", () -> OpcionesCliente.coordenadas, v -> OpcionesCliente.coordenadas = v);
		this.interruptor(x, y + 96, w, "Cielo del Momento Reviil", () -> OpcionesCliente.reviilCielo, v -> OpcionesCliente.reviilCielo = v);
		this.addRenderableWidget(Button.builder(Component.literal("Listo"), b -> this.onClose()).bounds(x + w / 2 - 50, y + 128, 100, 20).build());
	}

	private void interruptor(int x, int y, int w, String nombre, BooleanSupplier leer, Consumer<Boolean> poner) {
		this.addRenderableWidget(Button.builder(texto(nombre, leer.getAsBoolean()), b -> {
			poner.accept(!leer.getAsBoolean());
			OpcionesCliente.guardar();
			b.setMessage(texto(nombre, leer.getAsBoolean()));
		}).bounds(x, y, w, 20).build());
	}

	private static Component texto(String nombre, boolean si) {
		return Component.literal(nombre + ": ").append(si ? Component.literal("ACTIVADO").withStyle(ChatFormatting.GREEN)
				: Component.literal("DESACTIVADO").withStyle(ChatFormatting.RED));
	}

	@Override
	public void renderBackground(GuiGraphics g, int mx, int my, float pt) {
	}

	@Override
	public void render(GuiGraphics g, int mx, int my, float pt) {
		Fondo.oscurecer(this, g);
		g.drawCenteredString(this.font, Component.literal("HUD").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), this.width / 2, this.height / 2 - 84, 16777215);
		g.drawCenteredString(this.font, "Solo cambia lo que ves tú", this.width / 2, this.height / 2 - 70, 11184810);
		super.render(g, mx, my, pt);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	public static final class Comando {
		private Comando() {
		}

		@SubscribeEvent
		public static void alRegistrar(RegisterClientCommandsEvent e) {
			e.getDispatcher().register(ClientCommandManager.literal("hud").executes(c -> {
				Minecraft mc = Minecraft.getInstance();
				mc.tell(() -> mc.setScreen(new PantallaHud()));
				return 1;
			}));
		}
	}
}
