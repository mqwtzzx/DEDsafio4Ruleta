package net.ruleta.objetos;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** Clic derecho sobre un jugador: lo manda a su cama (o al spawn del mundo). */
public class PaloRegreso extends Item {
	public PaloRegreso(Item.Properties p) {
		super(p);
	}

	@Override
	public InteractionResult interactLivingEntity(ItemStack palo, Player quien, LivingEntity objetivo, InteractionHand mano) {
		if (!(objetivo instanceof ServerPlayer jugador))
			return InteractionResult.PASS;
		if (quien.level().isClientSide)
			return InteractionResult.SUCCESS;
		humo(jugador.serverLevel(), jugador.position());
		regresar(jugador);
		quien.getCooldowns().addCooldown(this, 10);
		return InteractionResult.CONSUME;
	}

	public static void regresar(ServerPlayer p) {
		MinecraftServer server = p.getServer();
		if (server == null)
			return;
		BlockPos cama = p.getRespawnPosition();
		ServerLevel nivelCama = server.getLevel(p.getRespawnDimension());
		if (cama != null && nivelCama != null) {
			// 1.20.1: Player.findRespawnPositionAndUseSpawnBlock(nivel, cama, ángulo, forzado, true).
			// 1.21.1: el mismo cálculo (sin gastar carga del ancla) está en ServerPlayer y devuelve un DimensionTransition.
			DimensionTransition sitio = p.findRespawnPositionAndUseSpawnBlock(true, DimensionTransition.DO_NOTHING);
			if (!sitio.missingRespawnBlock() && sitio.newLevel() == nivelCama) {
				Vec3 v = sitio.pos();
				p.teleportTo(nivelCama, v.x, v.y, v.z, p.getRespawnAngle(), 0.0F);
				despues(p, "Has vuelto a tu zona de respawn.");
				return;
			}
		}
		ServerLevel mundo = server.overworld();
		BlockPos spawn = mundo.getSharedSpawnPos();
		int y = mundo.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, spawn.getX(), spawn.getZ());
		p.teleportTo(mundo, (double) spawn.getX() + 0.5D, y, (double) spawn.getZ() + 0.5D, mundo.getSharedSpawnAngle(), 0.0F);
		despues(p, "No tienes zona de respawn.");
	}

	private static void despues(ServerPlayer p, String texto) {
		p.fallDistance = 0.0F;
		p.displayClientMessage(Component.literal(texto).withStyle(ChatFormatting.WHITE), true);
		ServerLevel nivel = p.serverLevel();
		nivel.playSound(null, p.getX(), p.getY(), p.getZ(), Objetos.SONIDO_REGRESO.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
	}

	static void humo(ServerLevel nivel, Vec3 pos) {
		nivel.sendParticles(ParticleTypes.LARGE_SMOKE, pos.x, pos.y + 1.0D, pos.z, 30, 0.3D, 0.8D, 0.3D, 0.02D);
		nivel.playSound(null, pos.x, pos.y, pos.z, Objetos.SONIDO_REGRESO.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
	}

	@Override
	public void appendHoverText(ItemStack s, Item.TooltipContext contexto, List<Component> lineas, TooltipFlag f) {
		lineas.add(Component.literal("Clic derecho sobre un jugador: lo manda a su cama (o al spawn del mundo)").withStyle(ChatFormatting.GRAY));
	}
}
