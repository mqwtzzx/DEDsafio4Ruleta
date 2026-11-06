package net.ruleta;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Cliente → servidor: activar o desactivar un momento. */
public record MomentoPayload(String id, boolean activar, int segundos, int segundos2, String extra) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<MomentoPayload> TYPE = new CustomPacketPayload.Type<>(Ruleta.id("momento"));
	public static final StreamCodec<RegistryFriendlyByteBuf, MomentoPayload> STREAM_CODEC = CustomPacketPayload.codec(MomentoPayload::write, MomentoPayload::decode);

	public MomentoPayload(String id, boolean activar, int segundos) {
		this(id, activar, segundos, 0, "");
	}

	public MomentoPayload(String id, boolean activar, int segundos, int segundos2) {
		this(id, activar, segundos, segundos2, "");
	}

	public static void encode(MomentoPayload m, RegistryFriendlyByteBuf b) {
		b.writeUtf(m.id, 64);
		b.writeBoolean(m.activar);
		b.writeVarInt(m.segundos);
		b.writeVarInt(m.segundos2);
		b.writeUtf(m.extra == null ? "" : m.extra, 512);
	}

	public static MomentoPayload decode(RegistryFriendlyByteBuf b) {
		return new MomentoPayload(b.readUtf(64), b.readBoolean(), b.readVarInt(), b.readVarInt(), b.readUtf(512));
	}

	private void write(RegistryFriendlyByteBuf b) {
		encode(this, b);
	}

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
