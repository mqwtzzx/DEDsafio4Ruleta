package net.ruleta;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffectUtil;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.animal.SnowGolem;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.entity.monster.CaveSpider;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.Endermite;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.entity.monster.Stray;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.entity.monster.WitherSkeleton;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.Difficulty;
import net.ruleta.bunker.Bunkers;
import net.ruleta.compat.EventPriority;
import net.ruleta.compat.SubscribeEvent;
import net.ruleta.compat.event.BlockEvent;
import net.ruleta.compat.event.EntityEvents.EntityJoinLevelEvent;
import net.ruleta.compat.event.LivingEvents;
import net.ruleta.compat.event.PlayerEvent;
import net.ruleta.compat.event.PlayerInteractEvent;
import net.ruleta.compat.event.TickEvent;
import net.ruleta.nuclear.NuclearCreeperMod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Lo que hace cada cambio de dificultad activo en el mundo. */
public final class CambiosEfectos {
	static final String TAG_BOMBA = "ruleta_bomba_warden";
	private static final int INTERVALO_PROFUNDO = 100;
	private static final int MAX_BOMBAS = 4;
	private static final int MAX_BLAZES = 5;
	private static final float PROB_ARANA_CUEVA = 0.3F;
	static final String TAG_BOGGED = "ruleta_bogged";
	private static final Map<UUID, Long> PERLAS = new HashMap<>();
	private static EntityType<?> tipoNucleeper;
	private static final Set<UUID> ARDIENDO = new HashSet<>();
	private static final Map<UUID, BlockPos> PLACAS = new HashMap<>();
	private static final float DANO_BOTON = 4.0E27F;
	private static final float DANO_PLACA = 1.0E12F;
	private static final float DANO_PUERTA = 1.0E12F;
	private static final float DANO_ENDERMITE = 100.0F;
	private static final float DANO_ENDERMAN = 100.0F;
	private static final int TICKS_OJOS = 3600;
	private static final float PROB_PERLA_NUCLEEPER = 0.01F;
	static final String TAG_INTELIGENTE = "ruleta_zombie_inteligente";
	private static final int CAPA_INTELIGENTES = 40;
	private static final float DUREZA_ZOMBIE = 5.0F;
	private static final int INTERVALO_PICAR = 3;
	/** En 1.21 los modificadores de atributo se identifican con un ResourceLocation (antes el UUID 5d1c7a0e-…). */
	private static final ResourceLocation ID_VELOCIDAD_INTELIGENTE = Ruleta.id("zombie_inteligente");
	private static final Map<UUID, Integer> AIRE = new HashMap<>();
	private static boolean nucleeperResuelto;
	static final String TAG_TREPADOR = "ruleta_zombie_trepador";
	static final String TAG_NO_TREPADOR = "ruleta_zombie_normal";
	public static volatile Set<String> activosCliente = Set.of();

	private CambiosEfectos() {
	}

	/** Al activarse un cambio se aplica también a lo que ya hay en el mundo. */
	static void alActivar(MinecraftServer server, String id) {
		if (Cambios.ID_SIN_REGENERACION.equals(id))
			server.getGameRules().getRule(GameRules.RULE_NATURAL_REGENERATION).set(false, server);
		if (Cambios.ID_ZOMBIES_MEJORADOS.equals(id))
			for (ServerLevel nivel : server.getAllLevels())
				for (Entity entidad : nivel.getAllEntities())
					if (entidad instanceof Zombie zombie && esZombieComun(zombie))
						mejorarZombie(zombie, false);
		if (Cambios.ID_ARANAS_CUEVA.equals(id) || Cambios.ID_ARANAS_FUERZA.equals(id) || Cambios.ID_ARANAS_FUERZA_IV.equals(id))
			for (ServerLevel nivel : server.getAllLevels())
				for (Entity entidad : nivel.getAllEntities())
					if (entidad instanceof Spider arana)
						potenciarArana(arana);
		if (Cambios.ID_EXTINCION.equals(id)) {
			for (ServerLevel nivel : server.getAllLevels()) {
				List<Animal> victimas = new ArrayList<>();
				for (Entity entidad : nivel.getAllEntities())
					if (entidad instanceof Animal animal && esAnimalExtinguible(animal))
						victimas.add(animal);
				for (Animal animal : victimas)
					animal.discard();
			}
		}
		if (Cambios.ID_CREEPERS.equals(id))
			for (ServerLevel nivel : server.getAllLevels())
				for (Entity entidad : nivel.getAllEntities())
					if (entidad instanceof Creeper creeper)
						electrificar(creeper);
	}

	@SubscribeEvent
	public static void alUnirseAlMundo(EntityJoinLevelEvent e) {
		if (e.getLevel().isClientSide())
			return;
		Entity entidad = e.getEntity();
		if (entidad instanceof Creeper creeper) {
			if (Ruleta.cambioActivo(Cambios.ID_CREEPERS))
				electrificar(creeper);
		} else if (entidad instanceof SnowGolem || entidad instanceof IronGolem golem && golem.isPlayerCreated()) {
			if (!e.loadedFromDisk() && Ruleta.cambioActivo(Cambios.ID_SIN_GOLEMS))
				e.setCanceled(true);
		} else if (entidad instanceof Animal animal) {
			if (esAnimalExtinguible(animal) && Ruleta.cambioActivo(Cambios.ID_EXTINCION))
				e.setCanceled(true);
		} else if (entidad instanceof Spider arana) {
			potenciarArana(arana);
		} else if (entidad instanceof Arrow flecha) {
			if (!e.loadedFromDisk() && flecha.getOwner() instanceof Skeleton tirador && tirador.getTags().contains(TAG_BOGGED))
				flecha.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 0));
		} else if (entidad instanceof Zombie zombie) {
			if (!esZombieComun(zombie))
				return;
			if (Ruleta.cambioActivo(Cambios.ID_ZOMBIES_MEJORADOS))
				mejorarZombie(zombie, !e.loadedFromDisk());
			else if (!e.loadedFromDisk() && !zombie.getTags().contains(TAG_TREPADOR) && !zombie.getTags().contains(TAG_NO_TREPADOR)
					&& Ruleta.cambioActivo(Cambios.ID_ZOMBIES_TREPADORES))
				zombie.addTag(zombie.getRandom().nextBoolean() ? TAG_TREPADOR : TAG_NO_TREPADOR);
			if (!e.loadedFromDisk() && Ruleta.cambioActivo(Cambios.ID_ZOMBIES_PICO))
				ponerPico(zombie);
		} else if (entidad instanceof ItemEntity item) {
			if (!e.loadedFromDisk() && item.getItem().is(ItemTags.SAPLINGS) && item.getOwner() == null && Ruleta.cambioActivo(Cambios.ID_SIN_BROTES)
					&& hayHojasCerca(e.getLevel(), item.blockPosition()))
				e.setCanceled(true);
		}
	}

	private static EntityType<?> tipoNucleeper() {
		if (!nucleeperResuelto) {
			nucleeperResuelto = true;
			tipoNucleeper = NuclearCreeperMod.NUCLEAR_CREEPER.get();
		}
		return tipoNucleeper;
	}

	private static boolean esAnimalExtinguible(Animal animal) {
		if (animal instanceof Enemy || animal.hasCustomName() || animal.isLeashed())
			return false;
		if (animal instanceof TamableAnimal mascota && mascota.isTame())
			return false;
		return !(animal instanceof AbstractHorse caballo && caballo.isTamed());
	}

	@SubscribeEvent
	public static void alHerir(LivingEvents.LivingHurtEvent e) {
		LivingEntity victima = e.getEntity();
		if (victima.level().isClientSide())
			return;
		if (e.getSource().is(DamageTypes.IN_FIRE) || e.getSource().is(DamageTypes.ON_FIRE) || e.getSource().is(DamageTypes.LAVA)) {
			if (Ruleta.cambioActivo(Cambios.ID_LAVA_FUEGO))
				e.setAmount(e.getAmount() * 3.0F);
			if (Ruleta.cambioActivo(Cambios.ID_FUEGO_LAVA_X10))
				e.setAmount(e.getAmount() * 10.0F);
		} else if (e.getSource().is(DamageTypes.FALL)) {
			if (!eraPerla(victima) && Ruleta.cambioActivo(Cambios.ID_DANO_CAIDA))
				e.setAmount(e.getAmount() * 2.0F);
		} else if (e.getSource().is(DamageTypes.DROWN)) {
			if (Ruleta.cambioActivo(Cambios.ID_AHOGAMIENTO))
				e.setAmount(e.getAmount() * 10.0F);
		}
		if (e.getSource().getDirectEntity() instanceof Endermite && victima instanceof Player && e.getAmount() > 0.0F && Ruleta.cambioActivo(Cambios.ID_ENDERMITE))
			e.setAmount(DANO_ENDERMITE);
		if (e.getSource().getDirectEntity() instanceof EnderMan && victima instanceof Player && e.getAmount() > 0.0F && Ruleta.cambioActivo(Cambios.ID_ENDERMAN))
			e.setAmount(DANO_ENDERMAN);
		if (e.getSource().getDirectEntity() instanceof Spider && victima instanceof Player && e.getAmount() > 0.0F && Ruleta.cambioActivo(Cambios.ID_ARANAS_TELARANA))
			ponerTelarana(victima.level(), victima.blockPosition());
	}

	@SubscribeEvent
	public static void alMorir(LivingEvents.LivingDeathEvent e) {
		if (e.getEntity() instanceof ServerPlayer jugador) {
			ARDIENDO.remove(jugador.getUUID());
			PLACAS.remove(jugador.getUUID());
		}
		if (e.getEntity() instanceof Spider arana && !arana.level().isClientSide() && Ruleta.cambioActivo(Cambios.ID_ARANAS_TELARANA))
			ponerTelarana(arana.level(), arana.blockPosition());
	}

	private static void ponerTelarana(Level nivel, BlockPos pos) {
		if (!nivel.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING))
			return;
		BlockState actual = nivel.getBlockState(pos);
		if ((actual.isAir() || actual.canBeReplaced()) && actual.getFluidState().isEmpty())
			nivel.setBlockAndUpdate(pos, Blocks.COBWEB.defaultBlockState());
	}

	static void marcarPerla(ServerPlayer jugador) {
		PERLAS.put(jugador.getUUID(), jugador.level().getGameTime());
	}

	private static boolean eraPerla(LivingEntity victima) {
		if (!(victima instanceof ServerPlayer jugador))
			return false;
		Long tick = PERLAS.remove(jugador.getUUID());
		return tick != null && tick == jugador.level().getGameTime();
	}

	private static void potenciarArana(Spider arana) {
		if (Ruleta.cambioActivo(Cambios.ID_ARANAS_CUEVA))
			darEfectoInfinito(arana, MobEffects.MOVEMENT_SPEED, 0);
		if (Ruleta.cambioActivo(Cambios.ID_ARANAS_FUERZA))
			darEfectoInfinito(arana, MobEffects.DAMAGE_BOOST, 2);
		if (Ruleta.cambioActivo(Cambios.ID_ARANAS_FUERZA_IV))
			darEfectoInfinito(arana, MobEffects.DAMAGE_BOOST, 3);
	}

	private static void darEfectoInfinito(LivingEntity criatura, Holder<MobEffect> efecto, int amplificador) {
		MobEffectInstance actual = criatura.getEffect(efecto);
		if (actual == null || actual.getAmplifier() < amplificador || actual.getDuration() != -1)
			criatura.addEffect(new MobEffectInstance(efecto, -1, amplificador, false, false));
	}

	@SubscribeEvent
	public static void alMedirVisibilidad(LivingEvents.LivingVisibilityEvent e) {
		LivingEntity visto = e.getEntity();
		if (!visto.isInvisible() || visto.level().isClientSide() || !Ruleta.cambioActivo(Cambios.ID_INVISIBLES))
			return;
		int piezas = 0;
		int total = 0;
		for (ItemStack pieza : visto.getArmorSlots()) {
			total++;
			if (!pieza.isEmpty())
				piezas++;
		}
		double cobertura = total > 0 ? (double) piezas / (double) total : 0.0D;
		e.modifyVisibility(1.0D / (0.7D * Math.max(0.1D, cobertura)));
	}

	private static void electrificar(Creeper creeper) {
		if (creeper.isPowered() || creeper.getType() == tipoNucleeper())
			return;
		CompoundTag tag = new CompoundTag();
		creeper.addAdditionalSaveData(tag);
		tag.putBoolean("powered", true);
		creeper.readAdditionalSaveData(tag);
	}

	private static boolean hayHojasCerca(Level nivel, BlockPos centro) {
		for (BlockPos pos : BlockPos.betweenClosed(centro.offset(-1, -1, -1), centro.offset(1, 1, 1)))
			if (nivel.getBlockState(pos).is(BlockTags.LEAVES))
				return true;
		return false;
	}

	/** Calabaza que forma un gólem con «sin golems» activo: se cancela y se avisa. */
	@SubscribeEvent
	public static void alColocarBloques(BlockEvent.EntityPlaceEvent e) {
		if (!(e instanceof BlockEvent.EntityMultiPlaceEvent multi) || e.getLevel().isClientSide())
			return;
		if (!Ruleta.cambioActivo(Cambios.ID_SIN_GOLEMS))
			return;
		for (BlockEvent.BlockSnapshot instantanea : multi.getReplacedBlockSnapshots()) {
			BlockState antes = instantanea.getReplacedBlock();
			if (antes.is(Blocks.IRON_BLOCK) || antes.is(Blocks.SNOW_BLOCK)) {
				e.setCanceled(true);
				if (e.getEntity() instanceof ServerPlayer jugador)
					jugador.displayClientMessage(Component.literal("Reviil lanzó una maldición: las calabazas ya no dan vida a los golems."), true);
				return;
			}
		}
	}

	@SubscribeEvent
	public static void alSoltarBotin(LivingEvents.LivingDropsEvent e) {
		LivingEntity victima = e.getEntity();
		if (victima instanceof Warden && !victima.level().isClientSide() && Ruleta.cambioActivo(Cambios.ID_WARDEN_SHRIEKER)) {
			ItemEntity shrieker = new ItemEntity(victima.level(), victima.getX(), victima.getY() + 0.5D, victima.getZ(), new ItemStack(Items.SCULK_SHRIEKER));
			shrieker.setDefaultPickUpDelay();
			e.getDrops().add(shrieker);
		}
	}

	private static boolean sinLeche(Level nivel) {
		return nivel.isClientSide() ? activosCliente.contains(Cambios.ID_SIN_LECHE) : Ruleta.cambioActivo(Cambios.ID_SIN_LECHE);
	}

	@SubscribeEvent
	public static void alInteractuarConEntidadEspecifica(PlayerInteractEvent.EntityInteractSpecific e) {
		if (e.getTarget() instanceof Cow && e.getItemStack().is(Items.BUCKET) && sinLeche(e.getLevel())) {
			e.setCanceled(true);
			e.setCancellationResult(InteractionResult.FAIL);
		}
	}

	@SubscribeEvent
	public static void alInteractuarConEntidad(PlayerInteractEvent.EntityInteract e) {
		if (!(e.getTarget() instanceof Cow) || !e.getItemStack().is(Items.BUCKET) || !sinLeche(e.getLevel()))
			return;
		e.setCanceled(true);
		e.setCancellationResult(InteractionResult.FAIL);
		if (e.getEntity() instanceof ServerPlayer jugador) {
			jugador.displayClientMessage(Component.literal("Reviil ordeñó a las vacas: ya no se puede sacar leche."), true);
			jugador.inventoryMenu.sendAllDataToRemote();
		}
	}

	@SubscribeEvent
	public static void alTickJugador(TickEvent.PlayerTickEvent e) {
		if (e.phase != TickEvent.Phase.END || !(e.player instanceof ServerPlayer jugador))
			return;
		respirarBajoElAgua(jugador);
		ahogamientoAumentado(jugador);
		aparicionesEnElProfundo(jugador);
		aparicionesEnElNether(jugador);
		aparicionesTerrestres(jugador);
		fuegoEnElNether(jugador);
		sinRegeneracion(jugador);
		placaDePresion(jugador);
		misOjos(jugador);
	}

	@SubscribeEvent
	public static void alDesconectarse(PlayerEvent.PlayerLoggedOutEvent e) {
		ARDIENDO.remove(e.getEntity().getUUID());
		PLACAS.remove(e.getEntity().getUUID());
		AIRE.remove(e.getEntity().getUUID());
	}

	private static void fuegoEnElNether(ServerPlayer jugador) {
		UUID id = jugador.getUUID();
		if (jugador.level().dimension() != Level.NETHER || !jugador.isAlive() || jugador.isCreative() || jugador.isSpectator()) {
			ARDIENDO.remove(id);
			return;
		}
		boolean arde = false;
		if (Ruleta.cambioActivo(Cambios.ID_FUEGO_SIN_ARMADURA)) {
			for (ItemStack pieza : jugador.getArmorSlots()) {
				if (pieza.isEmpty()) {
					arde = true;
					break;
				}
			}
		}
		if (Ruleta.cambioActivo(Cambios.ID_FUEGO_INFINITO)) {
			if (jugador.isOnFire())
				ARDIENDO.add(id);
			arde |= ARDIENDO.contains(id);
		}
		if (arde && jugador.getRemainingFireTicks() < 40)
			jugador.setRemainingFireTicks(200);
	}

	private static void sinRegeneracion(ServerPlayer jugador) {
		MinecraftServer server = jugador.getServer();
		if (server == null || jugador.tickCount % 100 != 0 || !Ruleta.cambioActivo(Cambios.ID_SIN_REGENERACION))
			return;
		GameRules.BooleanValue regla = server.getGameRules().getRule(GameRules.RULE_NATURAL_REGENERATION);
		if (regla.get())
			regla.set(false, server);
	}

	private static void placaDePresion(ServerPlayer jugador) {
		UUID id = jugador.getUUID();
		if (!jugador.isAlive() || jugador.isCreative() || jugador.isSpectator() || !Ruleta.cambioActivo(Cambios.ID_PLACA)) {
			PLACAS.remove(id);
			return;
		}
		BlockPos pos = jugador.blockPosition();
		if (!jugador.level().getBlockState(pos).is(BlockTags.PRESSURE_PLATES)) {
			PLACAS.remove(id);
			return;
		}
		if (!pos.equals(PLACAS.get(id))) {
			PLACAS.put(id, pos.immutable());
			jugador.hurt(Ruleta.fuenteLetal(jugador, "ruleta.placa", " murió al pisar una placa de presión"), DANO_PLACA);
		}
	}

	@SubscribeEvent
	public static void alUsarBloque(PlayerInteractEvent.RightClickBlock e) {
		if (e.getLevel().isClientSide() || e.getHand() != InteractionHand.MAIN_HAND || !(e.getEntity() instanceof ServerPlayer jugador))
			return;
		BlockState estado = e.getLevel().getBlockState(e.getPos());
		boolean boton = estado.is(BlockTags.BUTTONS);
		boolean puerta = !boton && esPuertaDeMano(estado);
		if (jugador.isCreative() || jugador.isSpectator() || !boton && !puerta)
			return;
		// agachado y con algo en la mano no se interactúa con el bloque
		if (jugador.isSecondaryUseActive() && (!jugador.getMainHandItem().isEmpty() || !jugador.getOffhandItem().isEmpty()))
			return;
		if (boton && Ruleta.cambioActivo(Cambios.ID_BOTON))
			jugador.hurt(Ruleta.fuenteLetal(jugador, "ruleta.boton", " murió al pulsar un botón"), DANO_BOTON);
		else if (puerta && Ruleta.cambioActivo(Cambios.ID_PUERTAS))
			jugador.hurt(Ruleta.fuenteLetal(jugador, "ruleta.puerta", " murió al abrir una puerta"), DANO_PUERTA);
	}

	/** Puertas que se abren a mano (no de hierro ni de SecurityCraft). */
	private static boolean esPuertaDeMano(BlockState estado) {
		Block bloque = estado.getBlock();
		if (!(bloque instanceof DoorBlock) || bloque == Blocks.IRON_DOOR)
			return false;
		ResourceLocation clave = BuiltInRegistries.BLOCK.getKey(bloque);
		return clave == null || !"securitycraft".equals(clave.getNamespace());
	}

	@SubscribeEvent
	public static void alTerminarDeComer(LivingEvents.LivingUseItemFinish e) {
		if (e.getEntity().level().isClientSide() || !(e.getEntity() instanceof ServerPlayer jugador))
			return;
		ItemStack comida = e.getItem();
		if ((comida.is(Items.GOLDEN_APPLE) || comida.is(Items.ENCHANTED_GOLDEN_APPLE)) && Ruleta.cambioActivo(Cambios.ID_SIN_ABSORCION))
			jugador.removeEffect(MobEffects.ABSORPTION);
	}

	private static void ahogamientoAumentado(ServerPlayer jugador) {
		UUID id = jugador.getUUID();
		if (!jugador.isAlive() || jugador.isCreative() || jugador.isSpectator() || !Ruleta.cambioActivo(Cambios.ID_AHOGAMIENTO)) {
			AIRE.remove(id);
			return;
		}
		int aire = jugador.getAirSupply();
		Integer antes = AIRE.get(id);
		if (antes != null && aire < antes) {
			aire = Math.max(-19, aire - 4);
			jugador.setAirSupply(aire);
		}
		AIRE.put(id, aire);
	}

	private static void ponerPico(Zombie zombie) {
		EquipmentSlot mano = Ruleta.cambioActivo(Cambios.ID_ZOMBIES_MEJORADOS) ? EquipmentSlot.OFFHAND : EquipmentSlot.MAINHAND;
		if (!zombie.getItemBySlot(mano).is(Items.DIAMOND_PICKAXE))
			zombie.setItemSlot(mano, new ItemStack(Items.DIAMOND_PICKAXE));
	}

	/** Con la cabeza en una puerta o trampilla bajo el agua ya no se respira. */
	private static void respirarBajoElAgua(ServerPlayer jugador) {
		if (!Ruleta.cambioActivo(Cambios.ID_RESPIRAR) || !jugador.isAlive() || jugador.isCreative() || jugador.isSpectator())
			return;
		Level nivel = jugador.level();
		BlockPos ojos = BlockPos.containing(jugador.getEyePosition());
		BlockState estado = nivel.getBlockState(ojos);
		if (!(estado.getBlock() instanceof DoorBlock) && !(estado.getBlock() instanceof TrapDoorBlock))
			return;
		if (!estado.getFluidState().isEmpty() || !nivel.getFluidState(ojos.above()).is(FluidTags.WATER) || MobEffectUtil.hasWaterBreathing(jugador))
			return;
		// en 1.21 la Respiración es el atributo OXYGEN_BONUS (antes EnchantmentHelper.getRespiration)
		int respiracion = (int) jugador.getAttributeValue(Attributes.OXYGEN_BONUS);
		boolean pierde = respiracion <= 0 || jugador.getRandom().nextInt(respiracion + 1) == 0;
		int aire = jugador.getAirSupply() - (pierde ? 5 : 4);
		if (aire <= -20) {
			aire = 0;
			jugador.hurt(jugador.damageSources().drown(), 2.0F);
		}
		jugador.setAirSupply(aire);
	}

	private static void aparicionesEnElProfundo(ServerPlayer jugador) {
		if (jugador.tickCount % INTERVALO_PROFUNDO != 0 || jugador.isSpectator())
			return;
		ServerLevel nivel = jugador.serverLevel();
		if (nivel.getDifficulty() == Difficulty.PEACEFUL || !nivel.getGameRules().getBoolean(GameRules.RULE_DOMOBSPAWNING)
				|| !nivel.getBiome(jugador.blockPosition()).is(Biomes.DEEP_DARK))
			return;
		RandomSource azar = nivel.getRandom();
		if (azar.nextFloat() < 0.25F && Ruleta.cambioActivo(Cambios.ID_BOMBAS_WARDEN))
			aparecerBomba(nivel, jugador, azar);
		if (azar.nextFloat() < 0.04F && Ruleta.cambioActivo(Cambios.ID_WARDEN_DEEP_DARK))
			aparecerWarden(nivel, jugador, azar);
	}

	private static void aparecerWarden(ServerLevel nivel, ServerPlayer jugador, RandomSource azar) {
		if (!nivel.getEntitiesOfClass(Warden.class, jugador.getBoundingBox().inflate(64.0D)).isEmpty())
			return;
		for (int intento = 0; intento < 12; intento++) {
			double angulo = azar.nextDouble() * Math.PI * 2.0D;
			double distancia = 18.0D + azar.nextDouble() * 14.0D;
			int x = Mth.floor(jugador.getX() + Math.cos(angulo) * distancia);
			int z = Mth.floor(jugador.getZ() + Math.sin(angulo) * distancia);
			BlockPos suelo = buscarSuelo(nivel, x, Mth.floor(jugador.getY()) + 6, z);
			if (suelo == null)
				continue;
			Warden warden = EntityType.WARDEN.create(nivel);
			if (warden == null)
				return;
			warden.moveTo((double) suelo.getX() + 0.5D, suelo.getY(), (double) suelo.getZ() + 0.5D, azar.nextFloat() * 360.0F, 0.0F);
			warden.finalizeSpawn(nivel, nivel.getCurrentDifficultyAt(suelo), MobSpawnType.TRIGGERED, null);
			nivel.addFreshEntityWithPassengers(warden);
			return;
		}
	}

	private static BlockPos buscarSuelo(ServerLevel nivel, int x, int yInicio, int z) {
		for (int y = yInicio; y > yInicio - 16; y--) {
			BlockPos pos = new BlockPos(x, y, z);
			BlockPos debajo = pos.below();
			if (nivel.getBlockState(debajo).isFaceSturdy(nivel, debajo, Direction.UP) && nivel.getFluidState(pos).isEmpty()
					&& nivel.noCollision(EntityType.WARDEN.getSpawnAABB((double) x + 0.5D, y, (double) z + 0.5D)))
				return nivel.getBiome(pos).is(Biomes.DEEP_DARK) && !nivel.canSeeSky(pos) ? pos : null;
		}
		return null;
	}

	private static void aparecerBomba(ServerLevel nivel, ServerPlayer jugador, RandomSource azar) {
		if (nivel.getEntitiesOfClass(Vex.class, jugador.getBoundingBox().inflate(48.0D), v -> v.getTags().contains(TAG_BOMBA)).size() >= MAX_BOMBAS)
			return;
		for (int intento = 0; intento < 12; intento++) {
			double angulo = azar.nextDouble() * Math.PI * 2.0D;
			double distancia = 12.0D + azar.nextDouble() * 14.0D;
			double x = jugador.getX() + Math.cos(angulo) * distancia;
			double y = jugador.getY() + (double) azar.nextInt(7);
			double z = jugador.getZ() + Math.sin(angulo) * distancia;
			BlockPos pos = BlockPos.containing(x, y, z);
			if (!nivel.getBiome(pos).is(Biomes.DEEP_DARK) || !nivel.noCollision(EntityType.VEX.getSpawnAABB(x, y, z)))
				continue;
			Vex bomba = EntityType.VEX.create(nivel);
			if (bomba == null)
				return;
			bomba.moveTo(x, y, z, azar.nextFloat() * 360.0F, 0.0F);
			bomba.addTag(TAG_BOMBA);
			bomba.setCustomName(Component.literal("Bomba Warden Voladora").withStyle(ChatFormatting.DARK_AQUA));
			bomba.setBoundOrigin(pos);
			bomba.setTarget(jugador);
			nivel.addFreshEntity(bomba);
			return;
		}
	}

	@SubscribeEvent
	public static void alTickCriatura(LivingEvents.LivingTickEvent e) {
		LivingEntity criatura = e.getEntity();
		if (criatura.level().isClientSide())
			return;
		if (criatura instanceof Zombie zombie) {
			tickZombieTrepador(zombie);
			tickZombieInteligente(zombie);
			return;
		}
		if (criatura instanceof Mob mob && mob.getType() == tipoNucleeper()) {
			tickNucleeper(mob);
			return;
		}
		if (!(criatura instanceof Vex bomba) || !bomba.getTags().contains(TAG_BOMBA) || bomba.isRemoved())
			return;
		ServerLevel nivel = (ServerLevel) bomba.level();
		LivingEntity objetivo = bomba.getTarget();
		if ((objetivo == null || !objetivo.isAlive()) && bomba.tickCount % 20 == 0) {
			objetivo = nivel.getNearestPlayer(bomba, 32.0D);
			bomba.setTarget(objetivo);
		}
		if (objetivo != null && objetivo.isAlive() && bomba.distanceToSqr(objetivo) < 6.25D)
			detonar(nivel, bomba);
	}

	private static boolean esZombieComun(Zombie zombie) {
		return zombie.getType() == EntityType.ZOMBIE || zombie.getType() == EntityType.ZOMBIE_VILLAGER;
	}

	private static void mejorarZombie(Zombie zombie, boolean conEspada) {
		zombie.removeTag(TAG_NO_TREPADOR);
		zombie.addTag(TAG_TREPADOR);
		if (!conEspada)
			return;
		// en 1.21 los encantamientos son datos del mundo: se buscan en su registro
		Holder<Enchantment> filo = zombie.level().registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolderOrThrow(Enchantments.SHARPNESS);
		if (EnchantmentHelper.getItemEnchantmentLevel(filo, zombie.getMainHandItem()) < 5) {
			ItemStack espada = new ItemStack(Items.IRON_SWORD);
			espada.enchant(filo, 5);
			zombie.setItemSlot(EquipmentSlot.MAINHAND, espada);
		}
	}

	private static void tickZombieTrepador(Zombie zombie) {
		if (!zombie.isAlive() || !zombie.getTags().contains(TAG_TREPADOR))
			return;
		Level nivel = zombie.level();
		if (zombie.getRemainingFireTicks() > 0 && nivel.isDay() && !zombie.isInLava() && nivel.canSeeSky(zombie.blockPosition()))
			zombie.clearFire();
		LivingEntity objetivo = zombie.getTarget();
		if (objetivo == null || !objetivo.isAlive() || zombie.distanceToSqr(objetivo) > 576.0D)
			return;
		if (zombie.getNavigation().isDone())
			zombie.getMoveControl().setWantedPosition(objetivo.getX(), objetivo.getY(), objetivo.getZ(), 1.0D);
		if (zombie.horizontalCollision) {
			Vec3 movimiento = zombie.getDeltaMovement();
			zombie.setDeltaMovement(movimiento.x, 0.2D, movimiento.z);
			zombie.resetFallDistance();
		}
	}

	private static void tickNucleeper(Mob nucleeper) {
		if (!nucleeper.isAlive() || !Ruleta.cambioActivo(Cambios.ID_NUCLEEPERS))
			return;
		ServerLevel nivel = (ServerLevel) nucleeper.level();
		LivingEntity objetivo = nucleeper.getTarget();
		if ((objetivo == null || !objetivo.isAlive()) && nucleeper.tickCount % 20 == 0) {
			objetivo = nivel.getNearestPlayer(nucleeper, 48.0D);
			nucleeper.setTarget(objetivo);
		}
		if (objetivo == null || !objetivo.isAlive() || nucleeper.distanceToSqr(objetivo) > 2304.0D)
			return;
		if (nucleeper.getNavigation().isDone())
			nucleeper.getMoveControl().setWantedPosition(objetivo.getX(), objetivo.getY(), objetivo.getZ(), 1.0D);
		if (nucleeper.tickCount % 5 == 0 && nivel.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING))
			abrirCamino(nivel, nucleeper, objetivo);
	}

	private static void abrirCamino(ServerLevel nivel, Mob criatura, LivingEntity objetivo) {
		abrirCamino(nivel, criatura, objetivo, Float.MAX_VALUE, true);
	}

	/** Rompe los bloques que hay entre la criatura y su objetivo (sin tocar los búnkeres). */
	private static void abrirCamino(ServerLevel nivel, Mob criatura, LivingEntity objetivo, float durezaMax, boolean soltar) {
		double dx = objetivo.getX() - criatura.getX();
		double dz = objetivo.getZ() - criatura.getZ();
		double dy = objetivo.getY() - criatura.getY();
		double largo = Math.sqrt(dx * dx + dz * dz);
		List<BlockPos> candidatos = new ArrayList<>();
		if (largo > 0.5D) {
			double ux = dx / largo;
			double uz = dz / largo;
			candidatos.add(BlockPos.containing(criatura.getX() + ux, criatura.getY(), criatura.getZ() + uz));
			candidatos.add(BlockPos.containing(criatura.getX() + ux, criatura.getY() + 1.0D, criatura.getZ() + uz));
			if (dy > 1.5D)
				candidatos.add(BlockPos.containing(criatura.getX() + ux, criatura.getY() + 2.0D, criatura.getZ() + uz));
		}
		if (dy > 1.5D)
			candidatos.add(BlockPos.containing(criatura.getX(), criatura.getY() + 2.0D, criatura.getZ()));
		else if (dy < -1.5D)
			candidatos.add(BlockPos.containing(criatura.getX(), criatura.getY() - 1.0D, criatura.getZ()));
		for (BlockPos pos : candidatos) {
			BlockState estado = nivel.getBlockState(pos);
			float dureza = estado.getDestroySpeed(nivel, pos);
			if (!estado.isAir() && estado.getFluidState().isEmpty() && dureza >= 0.0F && dureza <= durezaMax && !Bunkers.protegido(nivel, pos, estado))
				nivel.destroyBlock(pos, soltar, criatura);
		}
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void alRomperBloque(BlockEvent.BreakEvent e) {
		if (!e.getState().is(Blocks.COBWEB) || !(e.getLevel() instanceof ServerLevel nivel) || nivel.getDifficulty() == Difficulty.PEACEFUL)
			return;
		if (!Ruleta.cambioActivo(Cambios.ID_ARANAS_TELARANAS) || !(nivel.getRandom().nextFloat() < PROB_ARANA_CUEVA))
			return;
		CaveSpider arana = EntityType.CAVE_SPIDER.create(nivel);
		if (arana == null)
			return;
		BlockPos pos = e.getPos();
		arana.moveTo((double) pos.getX() + 0.5D, pos.getY(), (double) pos.getZ() + 0.5D, nivel.getRandom().nextFloat() * 360.0F, 0.0F);
		arana.setTarget(e.getPlayer());
		nivel.addFreshEntity(arana);
	}

	private static void aparicionesTerrestres(ServerPlayer jugador) {
		if (jugador.tickCount % 100 != 0 || jugador.isSpectator())
			return;
		ServerLevel nivel = jugador.serverLevel();
		if (nivel.getDifficulty() == Difficulty.PEACEFUL || !nivel.getGameRules().getBoolean(GameRules.RULE_DOMOBSPAWNING))
			return;
		RandomSource azar = nivel.getRandom();
		AABB zona = jugador.getBoundingBox().inflate(48.0D);
		if (nivel.dimension() == Level.NETHER) {
			if (azar.nextFloat() < 0.35F && Ruleta.cambioActivo(Cambios.ID_ZOMBIES_NETHER)
					&& nivel.getEntitiesOfClass(Zombie.class, zona, z -> z.getType() == EntityType.ZOMBIE).size() < 8)
				aparecerCerca(nivel, jugador, azar, EntityType.ZOMBIE, false);
			if (azar.nextFloat() < 0.25F && Ruleta.cambioActivo(Cambios.ID_WITHER_NETHER)
					&& nivel.getEntitiesOfClass(WitherSkeleton.class, zona).size() < 4)
				aparecerCerca(nivel, jugador, azar, EntityType.WITHER_SKELETON, false);
			EntityType<?> nucleeper = tipoNucleeper();
			if (nucleeper != null && jugador.getY() >= 128.0D && azar.nextFloat() < 0.25F && Ruleta.cambioActivo(Cambios.ID_NUCLEEPERS)
					&& nivel.getEntitiesOfClass(Mob.class, zona, m -> m.getType() == nucleeper).size() < 3) {
				@SuppressWarnings("unchecked")
				EntityType<Mob> tipo = (EntityType<Mob>) nucleeper;
				aparecerCerca(nivel, jugador, azar, tipo, false);
			}
		} else if (nivel.dimension() == Level.OVERWORLD && nivel.isNight()) {
			if (azar.nextFloat() < 0.25F && Ruleta.cambioActivo(Cambios.ID_STRAY) && nivel.getEntitiesOfClass(Stray.class, zona).size() < 4)
				aparecerCerca(nivel, jugador, azar, EntityType.STRAY, true);
			if (azar.nextFloat() < 0.25F && Ruleta.cambioActivo(Cambios.ID_ARANAS_CUEVA) && nivel.getEntitiesOfClass(CaveSpider.class, zona).size() < 4)
				aparecerCerca(nivel, jugador, azar, EntityType.CAVE_SPIDER, true);
			if (azar.nextFloat() < 0.25F && Ruleta.cambioActivo(Cambios.ID_WITHER_NOCHE) && nivel.getEntitiesOfClass(WitherSkeleton.class, zona).size() < 4)
				aparecerCerca(nivel, jugador, azar, EntityType.WITHER_SKELETON, true);
			if (azar.nextFloat() < 0.25F && Ruleta.cambioActivo(Cambios.ID_BOGGED)
					&& nivel.getEntitiesOfClass(Skeleton.class, zona, s -> s.getTags().contains(TAG_BOGGED)).size() < 4) {
				Skeleton bogged = aparecerCerca(nivel, jugador, azar, EntityType.SKELETON, true);
				if (bogged != null) {
					bogged.addTag(TAG_BOGGED);
					bogged.setCustomName(Component.literal("Bogged"));
				}
			}
		}
		if (nivel.dimension() != Level.OVERWORLD)
			return;
		EntityType<?> nucleeperNieve = tipoNucleeper();
		if (nucleeperNieve != null && azar.nextFloat() < 0.2F && Ruleta.cambioActivo(Cambios.ID_MOB_NUCLEEPER_NIEVE)
				&& nivel.getBiome(jugador.blockPosition()).value().coldEnoughToSnow(jugador.blockPosition())
				&& nivel.getEntitiesOfClass(Mob.class, zona, m -> m.getType() == nucleeperNieve).size() < 2) {
			@SuppressWarnings("unchecked")
			EntityType<Mob> tipo = (EntityType<Mob>) nucleeperNieve;
			Mob nuevo = aparecerCerca(nivel, jugador, azar, tipo, false);
			if (nuevo != null && !nivel.getBiome(nuevo.blockPosition()).value().coldEnoughToSnow(nuevo.blockPosition()))
				nuevo.discard();
		}
		if (azar.nextFloat() < 0.25F && Ruleta.cambioActivo(Cambios.ID_ARANAS_FUERZA_IV) && nivel.getEntitiesOfClass(CaveSpider.class, zona).size() < 4)
			aparecerCerca(nivel, jugador, azar, EntityType.CAVE_SPIDER, false, true, Integer.MAX_VALUE);
		if (jugador.getY() <= CAPA_INTELIGENTES && azar.nextFloat() < 0.25F && Ruleta.cambioActivo(Cambios.ID_ZOMBIES_INTELIGENTES)
				&& nivel.getEntitiesOfClass(Zombie.class, zona, z -> z.getTags().contains(TAG_INTELIGENTE)).size() < 5) {
			Zombie inteligente = aparecerCerca(nivel, jugador, azar, EntityType.ZOMBIE, true, false, CAPA_INTELIGENTES);
			if (inteligente != null)
				inteligente.addTag(TAG_INTELIGENTE);
		}
	}

	private static <T extends Mob> T aparecerCerca(ServerLevel nivel, ServerPlayer jugador, RandomSource azar, EntityType<T> tipo, boolean exigirOscuridad) {
		return aparecerCerca(nivel, jugador, azar, tipo, exigirOscuridad, false, Integer.MAX_VALUE);
	}

	/** Busca hasta 12 sitios a 16–32 bloques del jugador donde quepa la criatura y la hace aparecer. */
	private static <T extends Mob> T aparecerCerca(ServerLevel nivel, ServerPlayer jugador, RandomSource azar, EntityType<T> tipo, boolean exigirOscuridad,
												   boolean soloHielo, int maxY) {
		for (int intento = 0; intento < 12; intento++) {
			double angulo = azar.nextDouble() * Math.PI * 2.0D;
			double distancia = 16.0D + azar.nextDouble() * 16.0D;
			int x = Mth.floor(jugador.getX() + Math.cos(angulo) * distancia);
			int z = Mth.floor(jugador.getZ() + Math.sin(angulo) * distancia);
			int yInicio = Mth.floor(jugador.getY()) + 6;
			if (!nivel.isLoaded(new BlockPos(x, yInicio, z)))
				continue;
			for (int y = yInicio; y > yInicio - 16; y--) {
				BlockPos pos = new BlockPos(x, y, z);
				BlockPos debajo = pos.below();
				boolean sueloValido = soloHielo ? nivel.getBlockState(debajo).is(BlockTags.ICE) : nivel.getBlockState(debajo).isFaceSturdy(nivel, debajo, Direction.UP);
				if (!sueloValido || y > maxY || !nivel.getFluidState(pos).isEmpty()
						|| !nivel.noCollision(tipo.getSpawnAABB((double) x + 0.5D, y, (double) z + 0.5D)))
					continue;
				if (exigirOscuridad && !Monster.isDarkEnoughToSpawn(nivel, pos, azar))
					break;
				T criatura = tipo.create(nivel);
				if (criatura == null)
					return null;
				criatura.moveTo((double) x + 0.5D, y, (double) z + 0.5D, azar.nextFloat() * 360.0F, 0.0F);
				criatura.finalizeSpawn(nivel, nivel.getCurrentDifficultyAt(pos), MobSpawnType.NATURAL, null);
				nivel.addFreshEntityWithPassengers(criatura);
				return criatura;
			}
		}
		return null;
	}

	private static void aparicionesEnElNether(ServerPlayer jugador) {
		if (jugador.tickCount % 100 != 0 || jugador.isSpectator())
			return;
		ServerLevel nivel = jugador.serverLevel();
		if (nivel.dimension() != Level.NETHER || nivel.getDifficulty() == Difficulty.PEACEFUL
				|| !nivel.getGameRules().getBoolean(GameRules.RULE_DOMOBSPAWNING) || !Ruleta.cambioActivo(Cambios.ID_BLAZES_NETHER))
			return;
		RandomSource azar = nivel.getRandom();
		if (azar.nextFloat() >= 0.3F || nivel.getEntitiesOfClass(Blaze.class, jugador.getBoundingBox().inflate(48.0D)).size() >= MAX_BLAZES)
			return;
		for (int intento = 0; intento < 12; intento++) {
			double angulo = azar.nextDouble() * Math.PI * 2.0D;
			double distancia = 14.0D + azar.nextDouble() * 14.0D;
			double x = jugador.getX() + Math.cos(angulo) * distancia;
			double y = jugador.getY() + (double) azar.nextInt(9) - 3.0D;
			double z = jugador.getZ() + Math.sin(angulo) * distancia;
			BlockPos pos = BlockPos.containing(x, y, z);
			if (!nivel.isLoaded(pos) || !nivel.getFluidState(pos).isEmpty() || !nivel.noCollision(EntityType.BLAZE.getSpawnAABB(x, y, z)))
				continue;
			Blaze blaze = EntityType.BLAZE.create(nivel);
			if (blaze == null)
				return;
			blaze.moveTo(x, y, z, azar.nextFloat() * 360.0F, 0.0F);
			blaze.finalizeSpawn(nivel, nivel.getCurrentDifficultyAt(pos), MobSpawnType.NATURAL, null);
			nivel.addFreshEntityWithPassengers(blaze);
			return;
		}
	}

	/** Mirar al Sol con «Mis Ojos» activo: Miopía y Daltonismo 3 minutos. */
	private static void misOjos(ServerPlayer jugador) {
		if (jugador.tickCount % 5 != 0 || !jugador.isAlive() || jugador.isCreative() || jugador.isSpectator() || !Ruleta.cambioActivo(Cambios.ID_MIS_OJOS))
			return;
		ServerLevel nivel = jugador.serverLevel();
		if (nivel.dimension() != Level.OVERWORLD || nivel.isRaining() || jugador.hasEffect(MobEffects.BLINDNESS))
			return;
		float angulo = nivel.getTimeOfDay(1.0F) * 6.2831855F;
		Vec3 sol = new Vec3(-Mth.sin(angulo), Mth.cos(angulo), 0.0D);
		if (sol.y < 0.05D || !(jugador.getViewVector(1.0F).dot(sol) >= 0.985D))
			return;
		Vec3 ojos = jugador.getEyePosition();
		BlockHitResult golpe = nivel.clip(new ClipContext(ojos, ojos.add(sol.scale(128.0D)), ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, jugador));
		if (golpe.getType() != HitResult.Type.MISS)
			return;
		jugador.addEffect(new MobEffectInstance(Efectos.MIOPIA.holder(), TICKS_OJOS, 0, false, false, false));
		jugador.addEffect(new MobEffectInstance(Efectos.DALTONISMO.holder(), TICKS_OJOS, 0, false, false, false));
	}

	/** «Creeper Nuclear al usar Ender Pearl»: 1 % de que aparezca uno junto al jugador al aterrizar. */
	static void perlaAterrizo(ServerPlayer jugador, double x, double y, double z) {
		if (!Ruleta.cambioActivo(Cambios.ID_PERLA_NUCLEEPER) || !(jugador.getRandom().nextFloat() < PROB_PERLA_NUCLEEPER))
			return;
		EntityType<?> tipo = tipoNucleeper();
		ServerLevel nivel = jugador.serverLevel();
		if (tipo == null || nivel.getDifficulty() == Difficulty.PEACEFUL || !(tipo.create(nivel) instanceof Mob nucleeper))
			return;
		double[][] desvios = {{2.0D, 0.0D}, {-2.0D, 0.0D}, {0.0D, 2.0D}, {0.0D, -2.0D}, {0.0D, 0.0D}};
		for (double[] d : desvios) {
			if (nivel.noCollision(tipo.getSpawnAABB(x + d[0], y, z + d[1]))) {
				nucleeper.moveTo(x + d[0], y, z + d[1], nivel.getRandom().nextFloat() * 360.0F, 0.0F);
				nucleeper.finalizeSpawn(nivel, nivel.getCurrentDifficultyAt(BlockPos.containing(x, y, z)), MobSpawnType.EVENT, null);
				nucleeper.setTarget(jugador);
				nivel.addFreshEntityWithPassengers(nucleeper);
				return;
			}
		}
	}

	/** Zombies inteligentes: más rápidos, rompen y ponen bloques para llegar al jugador. */
	private static void tickZombieInteligente(Zombie zombie) {
		if (!zombie.isAlive() || !zombie.getTags().contains(TAG_INTELIGENTE))
			return;
		AttributeInstance velocidad = zombie.getAttribute(Attributes.MOVEMENT_SPEED);
		if (velocidad != null && velocidad.getModifier(ID_VELOCIDAD_INTELIGENTE) == null)
			velocidad.addPermanentModifier(new AttributeModifier(ID_VELOCIDAD_INTELIGENTE, 0.5D, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
		ServerLevel nivel = (ServerLevel) zombie.level();
		LivingEntity objetivo = zombie.getTarget();
		if ((objetivo == null || !objetivo.isAlive()) && zombie.tickCount % 20 == 0) {
			objetivo = nivel.getNearestPlayer(zombie.getX(), zombie.getY(), zombie.getZ(), 24.0D, true);
			zombie.setTarget(objetivo);
		}
		if (objetivo == null || !objetivo.isAlive() || zombie.distanceToSqr(objetivo) > 576.0D)
			return;
		if (zombie.getNavigation().isDone())
			zombie.getMoveControl().setWantedPosition(objetivo.getX(), objetivo.getY(), objetivo.getZ(), 1.0D);
		if (!nivel.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING))
			return;
		double dx = objetivo.getX() - zombie.getX();
		double dz = objetivo.getZ() - zombie.getZ();
		double dy = objetivo.getY() - zombie.getY();
		double horizontal = Math.sqrt(dx * dx + dz * dz);
		BlockState piedra = Blocks.COBBLESTONE.defaultBlockState();
		boolean subiendo = dy > 1.5D;
		if (subiendo && horizontal >= 1.5D) {
			if (zombie.tickCount % INTERVALO_PICAR == 0) {
				Direction rumbo = Direction.getNearest(dx, 0.0D, dz);
				BlockPos pies = zombie.blockPosition();
				BlockPos escalon = pies.relative(rumbo);
				romperBloque(nivel, zombie, pies.above(2));
				romperBloque(nivel, zombie, escalon.above(1));
				romperBloque(nivel, zombie, escalon.above(2));
				colocarBloque(nivel, escalon, piedra);
				if (zombie.onGround()) {
					zombie.getMoveControl().setWantedPosition((double) escalon.getX() + 0.5D, (double) escalon.getY() + 1.0D, (double) escalon.getZ() + 0.5D, 1.0D);
					zombie.getJumpControl().jump();
				}
			}
			return;
		}
		if (zombie.tickCount % INTERVALO_PICAR == 0)
			abrirCamino(nivel, zombie, objetivo, DUREZA_ZOMBIE, false);
		if (subiendo) {
			if (zombie.onGround()) {
				if (nivel.getBlockState(zombie.blockPosition().above(2)).getCollisionShape(nivel, zombie.blockPosition().above(2)).isEmpty())
					zombie.getJumpControl().jump();
			} else {
				colocarBloque(nivel, BlockPos.containing(zombie.getX(), zombie.getY() - 1.0D, zombie.getZ()), piedra);
			}
		} else if (zombie.onGround() && zombie.tickCount % 2 == 0 && dy > -2.0D && horizontal > 1.5D) {
			BlockPos delante = BlockPos.containing(zombie.getX() + dx / horizontal, zombie.getY(), zombie.getZ() + dz / horizontal);
			if (nivel.getBlockState(delante).isAir() && nivel.getBlockState(delante.below()).isAir())
				colocarBloque(nivel, delante.below(), piedra);
		}
	}

	private static void romperBloque(ServerLevel nivel, Mob criatura, BlockPos pos) {
		BlockState estado = nivel.getBlockState(pos);
		float dureza = estado.getDestroySpeed(nivel, pos);
		if (!estado.isAir() && estado.getFluidState().isEmpty() && dureza >= 0.0F && dureza <= DUREZA_ZOMBIE && !Bunkers.protegido(nivel, pos, estado))
			nivel.destroyBlock(pos, false, criatura);
	}

	private static void colocarBloque(ServerLevel nivel, BlockPos pos, BlockState estado) {
		BlockState actual = nivel.getBlockState(pos);
		if ((actual.isAir() || actual.canBeReplaced()) && actual.getFluidState().isEmpty()) {
			nivel.setBlockAndUpdate(pos, estado);
			nivel.playSound(null, pos, estado.getSoundType().getPlaceSound(), SoundSource.HOSTILE, 1.0F, 0.8F);
		}
	}

	/** La bomba warden explota con el grito sónico y da Oscuridad a los jugadores cercanos. */
	private static void detonar(ServerLevel nivel, Vex bomba) {
		double x = bomba.getX();
		double y = bomba.getY(0.5D);
		double z = bomba.getZ();
		nivel.explode(bomba, x, y, z, 3.5F, Level.ExplosionInteraction.MOB);
		nivel.sendParticles(ParticleTypes.SONIC_BOOM, x, y, z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
		nivel.playSound(null, x, y, z, SoundEvents.WARDEN_SONIC_BOOM, SoundSource.HOSTILE, 3.0F, 1.0F);
		for (ServerPlayer cercano : nivel.getEntitiesOfClass(ServerPlayer.class, new AABB(x - 12.0D, y - 12.0D, z - 12.0D, x + 12.0D, y + 12.0D, z + 12.0D)))
			cercano.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 200, 0));
		bomba.discard();
	}
}
