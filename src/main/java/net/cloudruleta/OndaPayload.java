package net.cloudruleta;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Servidor → cliente: onda de una explosión. */
public record OndaPayload(double x, double y, double z, float radio) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<OndaPayload> TYPE = new CustomPacketPayload.Type<>(Ruleta.id("onda"));
	public static final StreamCodec<RegistryFriendlyByteBuf, OndaPayload> STREAM_CODEC = CustomPacketPayload.codec(OndaPayload::write, OndaPayload::decode);

	public static void encode(OndaPayload m, RegistryFriendlyByteBuf b) {
		b.writeDouble(m.x);
		b.writeDouble(m.y);
		b.writeDouble(m.z);
		b.writeFloat(m.radio);
	}

	public static OndaPayload decode(RegistryFriendlyByteBuf b) {
		return new OndaPayload(b.readDouble(), b.readDouble(), b.readDouble(), b.readFloat());
	}

	private void write(RegistryFriendlyByteBuf b) {
		encode(this, b);
	}

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
