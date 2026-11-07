package net.cloudruleta.nuclear.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.cloudruleta.nuclear.NuclearCreeperMod;

/** Servidor → cliente: animación de tótem con el objeto usado. */
public record TotemActivatePacket(ItemStack stack) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<TotemActivatePacket> TYPE =
			new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(NuclearCreeperMod.MODID, "main"));
	// writeItem/readItem de 1.20.1 → ItemStack.OPTIONAL_STREAM_CODEC (los objetos llevan componentes en 1.21.1)
	public static final StreamCodec<RegistryFriendlyByteBuf, TotemActivatePacket> STREAM_CODEC = CustomPacketPayload.codec(TotemActivatePacket::write, TotemActivatePacket::decode);

	public static void encode(TotemActivatePacket packet, RegistryFriendlyByteBuf buf) {
		ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, packet.stack);
	}

	public static TotemActivatePacket decode(RegistryFriendlyByteBuf buf) {
		return new TotemActivatePacket(ItemStack.OPTIONAL_STREAM_CODEC.decode(buf));
	}

	private void write(RegistryFriendlyByteBuf buf) {
		encode(this, buf);
	}

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
