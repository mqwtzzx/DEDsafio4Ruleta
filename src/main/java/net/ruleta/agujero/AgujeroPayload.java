package net.ruleta.agujero;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.phys.Vec3;
import net.ruleta.Ruleta;

import java.util.ArrayList;
import java.util.List;

/** Servidor → cliente: agujeros negros activos. */
public record AgujeroPayload(boolean activo, List<AgujeroPayload.Agujero> agujeros) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<AgujeroPayload> TYPE = new CustomPacketPayload.Type<>(Ruleta.id("agujero"));
	public static final StreamCodec<RegistryFriendlyByteBuf, AgujeroPayload> STREAM_CODEC = CustomPacketPayload.codec(AgujeroPayload::write, AgujeroPayload::decode);

	public record Agujero(Vec3 pos, float radio, String dimension) {
	}

	public static void encode(AgujeroPayload m, RegistryFriendlyByteBuf b) {
		b.writeBoolean(m.activo);
		b.writeVarInt(m.agujeros.size());
		for (Agujero a : m.agujeros) {
			b.writeDouble(a.pos().x);
			b.writeDouble(a.pos().y);
			b.writeDouble(a.pos().z);
			b.writeFloat(a.radio());
			b.writeUtf(a.dimension());
		}
	}

	public static AgujeroPayload decode(RegistryFriendlyByteBuf b) {
		boolean activo = b.readBoolean();
		int n = Math.min(b.readVarInt(), 64);
		List<Agujero> l = new ArrayList<>(n);
		for (int i = 0; i < n; i++) {
			Vec3 p = new Vec3(b.readDouble(), b.readDouble(), b.readDouble());
			float r = b.readFloat();
			String d = b.readUtf();
			l.add(new Agujero(p, r, d));
		}
		return new AgujeroPayload(activo, l);
	}

	private void write(RegistryFriendlyByteBuf b) {
		encode(this, b);
	}

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
