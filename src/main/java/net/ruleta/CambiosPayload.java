package net.ruleta;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Servidor → cliente: lista (csv) de cambios de dificultad activos. */
public record CambiosPayload(String activos) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<CambiosPayload> TYPE = new CustomPacketPayload.Type<>(Ruleta.id("cambios"));
	public static final StreamCodec<RegistryFriendlyByteBuf, CambiosPayload> STREAM_CODEC = CustomPacketPayload.codec(CambiosPayload::write, CambiosPayload::decode);

	public static void encode(CambiosPayload m, RegistryFriendlyByteBuf b) {
		b.writeUtf(m.activos == null ? "" : m.activos, 4096);
	}

	public static CambiosPayload decode(RegistryFriendlyByteBuf b) {
		return new CambiosPayload(b.readUtf(4096));
	}

	private void write(RegistryFriendlyByteBuf b) {
		encode(this, b);
	}

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
