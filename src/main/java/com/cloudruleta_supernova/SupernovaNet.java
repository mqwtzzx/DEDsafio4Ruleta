package com.cloudruleta_supernova;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * Antes un SimpleChannel de Forge «cloudruleta_supernova:sync» (protocolo "2"). En Fabric es un payload; el receptor del cliente
 * (SupernovaGhosts.apply) lo registra el código de cliente.
 */
public class SupernovaNet {
	private static final String PROTOCOL = "2";
	private static boolean registered;

	public static void init() {
		if (registered)
			return;
		registered = true;
		PayloadTypeRegistry.playS2C().register(SyncMessage.TYPE, SyncMessage.STREAM_CODEC);
	}

	/** Antes PacketDistributor.DIMENSION: a todos los jugadores de la dimensión de la estrella. */
	public static void broadcast(SupernovaEntity e, boolean remove) {
		if (!(e.level() instanceof ServerLevel sl))
			return;
		SyncMessage m = SyncMessage.of(e, remove);
		for (ServerPlayer p : sl.players())
			enviar(p, m);
	}

	public static void sendTo(ServerPlayer p, SupernovaEntity e) {
		enviar(p, SyncMessage.of(e, false));
	}

	private static void enviar(ServerPlayer p, SyncMessage m) {
		if (ServerPlayNetworking.canSend(p, SyncMessage.TYPE))
			ServerPlayNetworking.send(p, m);
	}

	public static class SyncMessage implements CustomPacketPayload {
		public static final CustomPacketPayload.Type<SyncMessage> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(SupernovaMod.MODID, "sync"));
		public static final StreamCodec<RegistryFriendlyByteBuf, SyncMessage> STREAM_CODEC = CustomPacketPayload.codec((m, b) -> encode(m, b), SyncMessage::decode);
		public final int id;
		public final String dim;
		public final double x;
		public final double y;
		public final double z;
		public final float maxRadius;
		public final int born;
		public final int detonated;
		public final int hold;
		public final boolean remove;

		public SyncMessage(int id, String dim, double x, double y, double z, float maxRadius, int born, int detonated, int hold, boolean remove) {
			this.id = id;
			this.dim = dim;
			this.x = x;
			this.y = y;
			this.z = z;
			this.maxRadius = maxRadius;
			this.born = born;
			this.detonated = detonated;
			this.hold = hold;
			this.remove = remove;
		}

		static SyncMessage of(SupernovaEntity e, boolean remove) {
			return new SyncMessage(e.getId(), e.level().dimension().location().toString(), e.getX(), e.getY(), e.getZ(), e.getMaxRadius(), e.bornTick(),
					e.detonatedTick(), e.getHoldTicks(), remove);
		}

		public SupernovaState toState() {
			return new SupernovaState(this.id, this.dim, this.x, this.y, this.z, this.maxRadius, this.born, this.detonated, this.hold);
		}

		static void encode(SyncMessage m, RegistryFriendlyByteBuf b) {
			b.writeInt(m.id);
			b.writeUtf(m.dim);
			b.writeDouble(m.x);
			b.writeDouble(m.y);
			b.writeDouble(m.z);
			b.writeFloat(m.maxRadius);
			b.writeInt(m.born);
			b.writeInt(m.detonated);
			b.writeInt(m.hold);
			b.writeBoolean(m.remove);
		}

		static SyncMessage decode(RegistryFriendlyByteBuf b) {
			return new SyncMessage(b.readInt(), b.readUtf(), b.readDouble(), b.readDouble(), b.readDouble(), b.readFloat(), b.readInt(), b.readInt(), b.readInt(),
					b.readBoolean());
		}

		@Override
		public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}
}
