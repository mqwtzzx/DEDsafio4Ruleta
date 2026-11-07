package net.cloudruleta.compat;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.cloudruleta.compat.event.EntityEvents;
import net.cloudruleta.compat.event.LivingEvents;
import net.cloudruleta.compat.event.TickEvent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/** Lo que hacen los mixins en cada punto donde Forge lanzaba un evento (el mixin solo llama aquí). */
public final class Ganchos {
	/** Viaje por portal ya avisado en Entity.handlePortal (para no avisarlo otra vez en changeDimension). */
	@Nullable
	private static Entity viajeEntidad;
	@Nullable
	private static ResourceKey<Level> viajeDestino;

	private Ganchos() {
	}

	/** LivingDeathEvent al empezar LivingEntity.die / ServerPlayer.die. Devuelve true si se cancela la muerte. */
	public static boolean alMorir(LivingEntity entidad, DamageSource fuente) {
		return Bus.post(new LivingEvents.LivingDeathEvent(entidad, fuente));
	}

	/** LivingHurtEvent al empezar actuallyHurt (si el daño no es ignorado). Devuelve la cantidad (0 = cancelado). */
	public static float alHerir(LivingEntity entidad, DamageSource fuente, float cantidad) {
		if (entidad.isInvulnerableTo(fuente) || !Bus.hasListeners(LivingEvents.LivingHurtEvent.class))
			return cantidad;
		LivingEvents.LivingHurtEvent e = new LivingEvents.LivingHurtEvent(entidad, fuente, cantidad);
		return Bus.post(e) ? 0.0F : e.getAmount();
	}

	/** LivingDamageEvent justo antes de restar la vida (tras armadura y absorción). Devuelve el daño final (0 = cancelado). */
	public static float alDanar(LivingEntity entidad, DamageSource fuente, float cantidad) {
		if (!Bus.hasListeners(LivingEvents.LivingDamageEvent.class))
			return cantidad;
		LivingEvents.LivingDamageEvent e = new LivingEvents.LivingDamageEvent(entidad, fuente, cantidad);
		return Bus.post(e) ? 0.0F : e.getAmount();
	}

	/** LivingDropsEvent al terminar dropAllDeathLoot: lo que añadan los manejadores se suelta en el mundo. */
	public static void alSoltar(LivingEntity entidad, DamageSource fuente) {
		if (!Bus.hasListeners(LivingEvents.LivingDropsEvent.class) || !(entidad.level() instanceof ServerLevel nivel))
			return;
		List<ItemEntity> extra = new ArrayList<>();
		if (Bus.post(new LivingEvents.LivingDropsEvent(entidad, fuente, extra)))
			return;
		for (ItemEntity item : extra)
			nivel.addFreshEntity(item);
	}

	/** LivingUseTotemEvent: se va a usar un tótem de la inmortalidad. Devuelve true si se puede usar. */
	public static boolean alUsarTotem(LivingEntity entidad, DamageSource fuente, ItemStack totem) {
		InteractionHand mano = entidad.getItemInHand(InteractionHand.MAIN_HAND) == totem ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
		return !Bus.post(new LivingEvents.LivingUseTotemEvent(entidad, fuente, totem, mano));
	}

	/** LivingTickEvent al empezar LivingEntity.tick. Devuelve true si se cancela el tick. */
	public static boolean alTickVivo(LivingEntity entidad) {
		return Bus.hasListeners(LivingEvents.LivingTickEvent.class) && Bus.post(new LivingEvents.LivingTickEvent(entidad));
	}

	public static double visibilidad(LivingEntity entidad, @Nullable Entity mira, double original) {
		if (!Bus.hasListeners(LivingEvents.LivingVisibilityEvent.class))
			return original;
		LivingEvents.LivingVisibilityEvent e = new LivingEvents.LivingVisibilityEvent(entidad, mira, original);
		Bus.post(e);
		return Math.max(0.0D, e.getVisibilityModifier());
	}

	public static ItemStack alTerminarDeUsar(LivingEntity entidad, ItemStack copia, ItemStack resultado) {
		LivingEvents.LivingUseItemFinish e = new LivingEvents.LivingUseItemFinish(entidad, copia, resultado);
		Bus.post(e);
		return e.getResultStack();
	}

	public static void tickJugador(Player jugador, TickEvent.Phase fase) {
		Bus.post(new TickEvent.PlayerTickEvent(fase, jugador));
	}

	/** EntityTravelToDimensionEvent; solo si de verdad cambia de dimensión. Devuelve true si se cancela. */
	public static boolean alViajar(Entity entidad, ResourceKey<Level> destino) {
		if (destino == entidad.level().dimension())
			return false;
		if (viajeEntidad == entidad && viajeDestino == destino) {
			viajeEntidad = null;
			viajeDestino = null;
			return false;
		}
		return Bus.post(new EntityEvents.EntityTravelToDimensionEvent(entidad, destino));
	}

	/** Aviso adelantado desde Entity.handlePortal (antes de que el portal de destino se genere, como en Forge). */
	public static boolean alEntrarEnPortal(Entity entidad, ResourceKey<Level> destino) {
		viajeEntidad = null;
		viajeDestino = null;
		if (destino == entidad.level().dimension())
			return false;
		if (Bus.post(new EntityEvents.EntityTravelToDimensionEvent(entidad, destino)))
			return true;
		viajeEntidad = entidad;
		viajeDestino = destino;
		return false;
	}

	public static void finPortal() {
		viajeEntidad = null;
		viajeDestino = null;
	}

	/**
	 * EntityJoinLevelEvent al añadir una entidad al mundo del servidor (nueva o cargada del disco).
	 * Devuelve true si no se debe añadir (cancelado o con la aparición anulada por MobSpawnEvent).
	 */
	public static boolean alUnirse(Entity entidad, boolean cargadaDelDisco) {
		if (entidad instanceof Mob mob && ((AparicionAnulable) mob).cloudruleta$aparicionAnulada())
			return true;
		if (!(entidad.level() instanceof ServerLevel nivel))
			return false;
		return Bus.post(new EntityEvents.EntityJoinLevelEvent(entidad, nivel, cargadaDelDisco));
	}

	/** Lo implementa MobMixin: setSpawnCancelled(true) del FinalizeSpawn de Forge. */
	public interface AparicionAnulable {
		boolean cloudruleta$aparicionAnulada();

		void cloudruleta$anularAparicion(boolean valor);
	}

	/** Lo implementa ExplosionMixin: la explosión se canceló en ExplosionEvent.Start. */
	public interface ExplosionCancelable {
		boolean cloudruleta$cancelada();

		void cloudruleta$cancelar();
	}
}
