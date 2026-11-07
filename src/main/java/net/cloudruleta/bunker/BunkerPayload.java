package net.cloudruleta.bunker;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.cloudruleta.Ruleta;

import java.util.ArrayList;
import java.util.List;

/** Servidor → cliente: búnkeres para el marcador. */
public record BunkerPayload(List<Bunkers.Bunker> lista, boolean todos) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<BunkerPayload> TYPE = new CustomPacketPayload.Type<>(Ruleta.id("bunker"));
	public static final StreamCodec<RegistryFriendlyByteBuf, BunkerPayload> STREAM_CODEC = CustomPacketPayload.codec(BunkerPayload::write, BunkerPayload::decode);

	public static void encode(BunkerPayload m, RegistryFriendlyByteBuf b) {
		b.writeBoolean(m.todos);
		b.writeVarInt(m.lista.size());
		for (Bunkers.Bunker k : m.lista) {
			b.writeUtf(k.nombre(), 64);
			b.writeUtf(k.dimension(), 256);
			b.writeInt(k.ex());
			b.writeInt(k.ey());
			b.writeInt(k.ez());
		}
	}

	public static BunkerPayload decode(RegistryFriendlyByteBuf b) {
		boolean todos = b.readBoolean();
		int n = b.readVarInt();
		List<Bunkers.Bunker> l = new ArrayList<>();
		for (int i = 0; i < n; i++)
			l.add(new Bunkers.Bunker(b.readUtf(64), b.readUtf(256), b.readInt(), b.readInt(), b.readInt()));
		return new BunkerPayload(l, todos);
	}

	private void write(RegistryFriendlyByteBuf b) {
		encode(this, b);
	}

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
