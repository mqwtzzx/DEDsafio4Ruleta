package net.ruleta;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Servidor → cliente: órdenes de pantalla (pieza + valor + dos textos). */
public record ReviilPayload(String pieza, int valor, Component titulo, Component aviso) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<ReviilPayload> TYPE = new CustomPacketPayload.Type<>(Ruleta.id("reviil"));
	public static final StreamCodec<RegistryFriendlyByteBuf, ReviilPayload> STREAM_CODEC = CustomPacketPayload.codec(ReviilPayload::write, ReviilPayload::decode);

	public static void encode(ReviilPayload m, RegistryFriendlyByteBuf b) {
		b.writeUtf(m.pieza);
		b.writeInt(m.valor);
		// en 1.20.5+ los textos van con el códec de componentes (FriendlyByteBuf.writeComponent ya no existe)
		ComponentSerialization.TRUSTED_STREAM_CODEC.encode(b, m.titulo);
		ComponentSerialization.TRUSTED_STREAM_CODEC.encode(b, m.aviso);
	}

	public static ReviilPayload decode(RegistryFriendlyByteBuf b) {
		return new ReviilPayload(b.readUtf(), b.readInt(), ComponentSerialization.TRUSTED_STREAM_CODEC.decode(b),
				ComponentSerialization.TRUSTED_STREAM_CODEC.decode(b));
	}

	private void write(RegistryFriendlyByteBuf b) {
		encode(this, b);
	}

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
