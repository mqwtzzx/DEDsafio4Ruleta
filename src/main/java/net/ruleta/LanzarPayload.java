package net.ruleta;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Cliente → servidor: el formulario del lanzador pide girar la ruleta. */
public record LanzarPayload(int color, int tipo, boolean animacion, boolean reviil, String titulo, String mensaje, String tiempo, String cambios,
							boolean cielo, String para, int sancion, String objetivo, String tiempoBan, boolean phora) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<LanzarPayload> TYPE = new CustomPacketPayload.Type<>(Ruleta.id("lanzar"));
	public static final StreamCodec<RegistryFriendlyByteBuf, LanzarPayload> STREAM_CODEC = CustomPacketPayload.codec(LanzarPayload::write, LanzarPayload::decode);

	public LanzarPayload(int color, int tipo, boolean animacion, boolean reviil, String titulo, String mensaje, String tiempo, String cambios) {
		this(color, tipo, animacion, reviil, titulo, mensaje, tiempo, cambios, false, "", 0, "", "", false);
	}

	public LanzarPayload(int color, int tipo, boolean animacion, boolean reviil, String titulo, String mensaje, String tiempo, String cambios,
						 boolean cielo, String para, int sancion, String objetivo, String tiempoBan) {
		this(color, tipo, animacion, reviil, titulo, mensaje, tiempo, cambios, cielo, para, sancion, objetivo, tiempoBan, false);
	}

	public static void encode(LanzarPayload m, RegistryFriendlyByteBuf b) {
		b.writeVarInt(m.color);
		b.writeVarInt(m.tipo);
		b.writeBoolean(m.animacion);
		b.writeBoolean(m.reviil);
		b.writeUtf(m.titulo == null ? "" : m.titulo, 256);
		b.writeUtf(m.mensaje == null ? "" : m.mensaje, 2048);
		b.writeUtf(m.tiempo == null ? "" : m.tiempo, 32);
		b.writeUtf(m.cambios == null ? "" : m.cambios, 1024);
		b.writeBoolean(m.cielo);
		b.writeUtf(m.para == null ? "" : m.para, 64);
		b.writeVarInt(m.sancion);
		b.writeUtf(m.objetivo == null ? "" : m.objetivo, 64);
		b.writeUtf(m.tiempoBan == null ? "" : m.tiempoBan, 32);
		b.writeBoolean(m.phora);
	}

	public static LanzarPayload decode(RegistryFriendlyByteBuf b) {
		return new LanzarPayload(b.readVarInt(), b.readVarInt(), b.readBoolean(), b.readBoolean(), b.readUtf(256), b.readUtf(2048), b.readUtf(32),
				b.readUtf(1024), b.readBoolean(), b.readUtf(64), b.readVarInt(), b.readUtf(64), b.readUtf(32), b.readBoolean());
	}

	private void write(RegistryFriendlyByteBuf b) {
		encode(this, b);
	}

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
