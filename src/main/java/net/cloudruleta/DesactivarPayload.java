package net.cloudruleta;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Cliente → servidor: desactivar un cambio de dificultad. */
public record DesactivarPayload(String id) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<DesactivarPayload> TYPE = new CustomPacketPayload.Type<>(Ruleta.id("desactivar"));
	public static final StreamCodec<RegistryFriendlyByteBuf, DesactivarPayload> STREAM_CODEC = CustomPacketPayload.codec(DesactivarPayload::write, DesactivarPayload::decode);

	public static void encode(DesactivarPayload m, RegistryFriendlyByteBuf b) {
		b.writeUtf(m.id, 128);
	}

	public static DesactivarPayload decode(RegistryFriendlyByteBuf b) {
		return new DesactivarPayload(b.readUtf(128));
	}

	private void write(RegistryFriendlyByteBuf b) {
		encode(this, b);
	}

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
