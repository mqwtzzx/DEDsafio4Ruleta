package net.ruleta.objetos;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.ruleta.herobrine.HerobrineEntity;

import java.util.List;

/**
 * Linterna con batería. En 1.20.1 la carga iba en el NBT del objeto (claves "Carga" y "Prendida"); en 1.21.1
 * los objetos usan componentes de datos, así que esas mismas claves van dentro del componente minecraft:custom_data
 * (que es también donde Minecraft mueve el NBT antiguo al actualizar un mundo).
 */
public class Linterna extends Item {
	public static final int CARGA_MAX = 150;
	public static final int TICKS_PRENDER = 60;
	private static final String CARGA = "Carga";
	private static final String PRENDIDA = "Prendida";
	private static final double ALCANCE_HEROBRINE = 32.0D;

	public Linterna(Item.Properties p) {
		super(p);
	}

	private static CompoundTag datos(ItemStack s) {
		CustomData d = s.get(DataComponents.CUSTOM_DATA);
		return d == null ? null : d.getUnsafe();
	}

	public static int carga(ItemStack s) {
		CompoundTag t = datos(s);
		return t == null || !t.contains(CARGA) ? CARGA_MAX : t.getInt(CARGA);
	}

	public static boolean prendida(ItemStack s) {
		CompoundTag t = datos(s);
		return t != null && t.getBoolean(PRENDIDA) && carga(s) > 0;
	}

	public static float intensidad(ItemStack s) {
		if (!prendida(s))
			return 0.0F;
		float k = (float) carga(s) / 150.0F;
		return k >= 0.5F ? 1.0F : 0.25F + 0.75F * (k / 0.5F);
	}

	private static void poner(ItemStack s, int carga, boolean prendida) {
		CustomData.update(DataComponents.CUSTOM_DATA, s, t -> {
			t.putInt(CARGA, Mth.clamp(carga, 0, CARGA_MAX));
			t.putBoolean(PRENDIDA, prendida && carga > 0);
		});
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level nivel, Player jugador, InteractionHand mano) {
		ItemStack linterna = jugador.getItemInHand(mano);
		ItemStack otra = jugador.getItemInHand(mano == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
		if (mano == InteractionHand.MAIN_HAND && otra.is(Objetos.BATERIA.get())) {
			if (carga(linterna) >= CARGA_MAX)
				return InteractionResultHolder.fail(linterna);
			if (!nivel.isClientSide) {
				poner(linterna, CARGA_MAX, prendida(linterna));
				if (!jugador.getAbilities().instabuild)
					otra.shrink(1);
				nivel.playSound(null, jugador.getX(), jugador.getY(), jugador.getZ(), SoundEvents.ARMOR_EQUIP_IRON, SoundSource.PLAYERS, 1.0F, 1.4F);
			}
			return InteractionResultHolder.sidedSuccess(linterna, nivel.isClientSide);
		}
		if (prendida(linterna)) {
			jugador.startUsingItem(mano);
			return InteractionResultHolder.consume(linterna);
		}
		if (carga(linterna) <= 0)
			return InteractionResultHolder.fail(linterna);
		jugador.startUsingItem(mano);
		return InteractionResultHolder.consume(linterna);
	}

	@Override
	public void onUseTick(Level nivel, LivingEntity quien, ItemStack linterna, int quedan) {
		int usados = this.getUseDuration(linterna, quien) - quedan;
		if (usados < TICKS_PRENDER)
			return;
		if (!nivel.isClientSide) {
			boolean prender = !prendida(linterna);
			poner(linterna, carga(linterna), prender);
			nivel.playSound(null, quien.getX(), quien.getY(), quien.getZ(), SoundEvents.LEVER_CLICK, SoundSource.PLAYERS, 0.6F, prender ? 1.3F : 0.8F);
		}
		quien.releaseUsingItem();
	}

	@Override
	public int getUseDuration(ItemStack s, LivingEntity quien) {
		return 72000;
	}

	@Override
	public UseAnim getUseAnimation(ItemStack s) {
		return UseAnim.NONE;
	}

	@Override
	public void inventoryTick(ItemStack s, Level nivel, Entity quien, int hueco, boolean elegida) {
		if (nivel.isClientSide || !prendida(s))
			return;
		if (nivel.getGameTime() % 20L == 0L) {
			int c = carga(s) - 1;
			poner(s, c, c > 0);
			if (c <= 0)
				nivel.playSound(null, quien.getX(), quien.getY(), quien.getZ(), SoundEvents.LEVER_CLICK, SoundSource.PLAYERS, 0.6F, 0.6F);
		}
		if (quien instanceof ServerPlayer p && enMano(p, s) && nivel.getGameTime() % 4L == 0L)
			apuntarHerobrine((ServerLevel) nivel, p);
	}

	private static boolean enMano(Player p, ItemStack s) {
		return p.getMainHandItem() == s || p.getOffhandItem() == s;
	}

	private static void apuntarHerobrine(ServerLevel nivel, ServerPlayer p) {
		Vec3 ojos = p.getEyePosition();
		Vec3 vista = p.getViewVector(1.0F).normalize();
		AABB caja = new AABB(ojos, ojos).inflate(ALCANCE_HEROBRINE);
		for (HerobrineEntity h : nivel.getEntitiesOfClass(HerobrineEntity.class, caja)) {
			Vec3 hacia = h.position().add(0.0D, (double) h.getBbHeight() * 0.6D, 0.0D).subtract(ojos);
			double d = hacia.length();
			if (d > ALCANCE_HEROBRINE || d < 0.001D)
				continue;
			if (vista.dot(hacia.scale(1.0D / d)) > 0.96D && p.hasLineOfSight(h))
				h.desaparecer();
		}
	}

	@Override
	public boolean isBarVisible(ItemStack s) {
		return carga(s) < CARGA_MAX;
	}

	@Override
	public int getBarWidth(ItemStack s) {
		return Math.round(13.0F * (float) carga(s) / 150.0F);
	}

	@Override
	public int getBarColor(ItemStack s) {
		float k = (float) carga(s) / 150.0F;
		return Mth.hsvToRgb(k / 3.0F, 1.0F, 1.0F);
	}

	/**
	 * En Forge era shouldCauseReequipAnimation(antes, ahora, cambioHueco): no repetir la animación de sacar el
	 * objeto cuando solo cambia la carga. En Fabric lo mismo se expresa con FabricItem#allowComponentsUpdateAnimation
	 * (solo se consulta cuando no cambia de hueco).
	 */
	@Override
	public boolean allowComponentsUpdateAnimation(Player jugador, InteractionHand mano, ItemStack antes, ItemStack ahora) {
		return !ItemStack.isSameItem(antes, ahora);
	}

	@Override
	public void appendHoverText(ItemStack s, Item.TooltipContext contexto, List<Component> lineas, TooltipFlag f) {
		lineas.add(Component.literal("Carga: " + carga(s) + " / 150" + (prendida(s) ? "  (prendida)" : "")).withStyle(ChatFormatting.GRAY));
		lineas.add(Component.literal("Mantén clic derecho 3 s para prenderla; clic para apagarla.").withStyle(ChatFormatting.DARK_GRAY));
		lineas.add(Component.literal("Batería en la otra mano + clic derecho: recarga.").withStyle(ChatFormatting.DARK_GRAY));
	}
}
