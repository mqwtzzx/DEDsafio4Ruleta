package net.ruleta.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityAttachment;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Team;
import net.ruleta.compat.Event;
import net.ruleta.compat.SubscribeEvent;
import net.ruleta.compat.client.RenderNameTagEvent;
import org.joml.Matrix4f;

/** La vida de cada jugador (número + corazón) encima de su nombre. */
public final class VidaNombre {
	private static final Style CORAZON = Style.EMPTY.withFont(ResourceLocation.fromNamespaceAndPath("ruleta", "vida")).withColor(16777215);
	private static final int COLOR_TRAS_PARED = 553648127;

	private VidaNombre() {
	}

	@SubscribeEvent
	public static void alDibujarNombre(RenderNameTagEvent e) {
		if (!(e.getEntity() instanceof Player jugador) || e.getResult() == Event.Result.DENY)
			return;
		Minecraft mc = Minecraft.getInstance();
		EntityRenderDispatcher dispatcher = mc.getEntityRenderDispatcher();
		double distancia = dispatcher.distanceToSqr(jugador);
		if (e.getResult() != Event.Result.ALLOW && !seVeElNombre(jugador, distancia))
			return;
		int vida = Math.max(0, (int) Math.ceil((double) jugador.getHealth()));
		Component linea = Component.literal(Integer.toString(vida)).withStyle(ChatFormatting.WHITE).append(Component.literal(" "))
				.append(Component.literal("").setStyle(CORAZON));
		// getDisplayObjective(2) de 1.20.1 = la puntuación bajo el nombre (DisplaySlot.BELOW_NAME)
		boolean hayPuntuacion = distancia < 100.0D && jugador.getScoreboard().getDisplayObjective(DisplaySlot.BELOW_NAME) != null;
		float y = -10.0F - (hayPuntuacion ? 10.349999F : 0.0F);
		// 1.20.1: translate(0, getNameTagOffsetY(), 0); 1.21.1 sitúa el nombre en el anclaje NAME_TAG (+0.5), igual que vanilla
		Vec3 ancla = jugador.getAttachments().getNullable(EntityAttachment.NAME_TAG, 0, jugador.getViewYRot(e.getPartialTick()));
		if (ancla == null)
			return;
		PoseStack pose = e.getPoseStack();
		pose.pushPose();
		pose.translate(ancla.x, ancla.y + 0.5D, ancla.z);
		pose.mulPose(dispatcher.cameraOrientation());
		pose.scale(-0.025F, -0.025F, 0.025F);
		Matrix4f matriz = pose.last().pose();
		int fondo = (int) (mc.options.getBackgroundOpacity(0.25F) * 255.0F) << 24;
		Font fuente = e.getEntityRenderer().getFont();
		float x = (float) (-fuente.width(linea)) / 2.0F;
		boolean atravesarParedes = !jugador.isDiscrete();
		fuente.drawInBatch(linea, x, y, COLOR_TRAS_PARED, false, matriz, e.getMultiBufferSource(),
				atravesarParedes ? Font.DisplayMode.SEE_THROUGH : Font.DisplayMode.NORMAL, fondo, e.getPackedLight());
		if (atravesarParedes)
			fuente.drawInBatch(linea, x, y, -1, false, matriz, e.getMultiBufferSource(), Font.DisplayMode.NORMAL, 0, e.getPackedLight());
		pose.popPose();
	}

	/** Las mismas reglas que vanilla para mostrar el nombre (distancia, equipos, invisibilidad). */
	private static boolean seVeElNombre(Player jugador, double distancia) {
		Minecraft mc = Minecraft.getInstance();
		LocalPlayer yo = mc.player;
		if (yo == null)
			return false;
		float maxima = jugador.isDiscrete() ? 32.0F : 64.0F;
		if (distancia >= (double) (maxima * maxima))
			return false;
		boolean noInvisible = !jugador.isInvisibleTo(yo);
		if (jugador != yo) {
			Team suyo = jugador.getTeam();
			Team mio = yo.getTeam();
			if (suyo != null) {
				Team.Visibility vis = suyo.getNameTagVisibility();
				return switch (vis) {
					case ALWAYS -> noInvisible;
					case NEVER -> false;
					case HIDE_FOR_OTHER_TEAMS -> mio == null ? noInvisible : suyo.isAlliedTo(mio) && (suyo.canSeeFriendlyInvisibles() || noInvisible);
					case HIDE_FOR_OWN_TEAM -> mio == null ? noInvisible : !suyo.isAlliedTo(mio) && noInvisible;
				};
			}
		}
		return Minecraft.renderNames() && jugador != mc.getCameraEntity() && noInvisible && !jugador.isVehicle();
	}
}
