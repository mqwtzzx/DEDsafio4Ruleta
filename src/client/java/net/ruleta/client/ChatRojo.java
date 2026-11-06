package net.ruleta.client;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.ruleta.compat.SubscribeEvent;
import net.ruleta.compat.client.ClientChatReceivedEvent;

/** Icono en los mensajes de entrar/salir, mensaje esperado en rojo y cabeza en los mensajes de muerte. */
public final class ChatRojo {
	private static Component esperado;
	private static long hasta;
	private static final Style ICONO = Style.EMPTY.withFont(ResourceLocation.fromNamespaceAndPath("ruleta", "icono"));

	private ChatRojo() {
	}

	public static void esperar(Component mensaje) {
		esperado = mensaje;
		hasta = System.currentTimeMillis() + 3000L;
	}

	private static boolean entradaSalida(ClientChatReceivedEvent e) {
		Component m = e.getMessage();
		if (m != null && m.getContents() instanceof TranslatableContents tr && tr.getKey().startsWith("multiplayer.player.")) {
			String k = tr.getKey();
			if (k.equals("multiplayer.player.joined") || k.equals("multiplayer.player.joined.renamed") || k.equals("multiplayer.player.left")) {
				e.setMessage(Component.empty().append(Component.literal("").setStyle(ICONO)).append(Component.literal(" ")).append(m));
				return true;
			}
		}
		return false;
	}

	@SubscribeEvent
	public static void alLlegarChat(ClientChatReceivedEvent e) {
		if (entradaSalida(e))
			return;
		Component original = e.getMessage();
		ponerRojo(e);
		Component cabeza = CabezaChat.para(original);
		if (cabeza != null)
			e.setMessage(Component.empty().append(cabeza).append(e.getMessage()));
	}

	private static void ponerRojo(ClientChatReceivedEvent e) {
		if (esperado == null || !e.isSystem())
			return;
		if (System.currentTimeMillis() > hasta) {
			esperado = null;
			return;
		}
		Component m = e.getMessage();
		if (m != null && (m.equals(esperado) || m.getString().equals(esperado.getString()))) {
			e.setMessage(Component.empty().append(m).withStyle(ChatFormatting.RED));
			esperado = null;
		}
	}
}
