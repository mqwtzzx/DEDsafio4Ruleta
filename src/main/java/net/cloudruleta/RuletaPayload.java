package net.cloudruleta;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Servidor → cliente: girar la cloudruleta en pantalla. */
public record RuletaPayload(int result, float startAngle, int spins, float offset, String who, boolean mini) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<RuletaPayload> TYPE = new CustomPacketPayload.Type<>(Ruleta.id("cloudruleta"));
	public static final StreamCodec<RegistryFriendlyByteBuf, RuletaPayload> STREAM_CODEC = CustomPacketPayload.codec(RuletaPayload::write, RuletaPayload::decode);

	public static void encode(RuletaPayload m, RegistryFriendlyByteBuf b) {
		b.writeVarInt(m.result);
		b.writeFloat(m.startAngle);
		b.writeVarInt(m.spins);
		b.writeFloat(m.offset);
		b.writeUtf(m.who);
		b.writeBoolean(m.mini);
	}

	public static RuletaPayload decode(RegistryFriendlyByteBuf b) {
		return new RuletaPayload(b.readVarInt(), b.readFloat(), b.readVarInt(), b.readFloat(), b.readUtf(), b.readBoolean());
	}

	private void write(RegistryFriendlyByteBuf b) {
		encode(this, b);
	}

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
