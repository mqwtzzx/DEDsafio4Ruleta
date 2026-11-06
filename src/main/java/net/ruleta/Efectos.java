package net.ruleta;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.ruleta.compat.DeferredRegister;
import net.ruleta.compat.RegistryObject;

/** Efectos de estado del mod. En 1.21 se usan como Holder: {@code TEMBLOR.holder()}. */
public final class Efectos {
	public static final DeferredRegister<MobEffect> EFECTOS = DeferredRegister.create(BuiltInRegistries.MOB_EFFECT, Ruleta.MOD_ID);
	public static final RegistryObject<MobEffect> TEMBLOR = EFECTOS.register("temblor", () -> new Efecto(9071947));
	public static final RegistryObject<MobEffect> RULETA_ROJA = EFECTOS.register("ruleta_roja", () -> new Efecto(12453900));
	public static final RegistryObject<MobEffect> MIOPIA = EFECTOS.register("miopia", () -> new Efecto(10526880));
	public static final RegistryObject<MobEffect> DALTONISMO = EFECTOS.register("daltonismo", () -> new Efecto(8421504));
	public static final int DURACION_PANTALLA_AZUL = 400;
	public static final RegistryObject<MobEffect> PANTALLA_AZUL = EFECTOS.register("pantalla_azul", () -> new Efecto(30935));
	public static final RegistryObject<MobEffect> ZONA_SEGURA = EFECTOS.register("zona_segura", () -> new MobEffect(MobEffectCategory.BENEFICIAL, 5230955) {
	});

	private Efectos() {
	}

	/** Fuerza la carga de la clase (y con ello el registro) al iniciar el mod. */
	static void init() {
	}

	public static boolean esDelMod(MobEffect efecto) {
		return efecto instanceof Efecto;
	}

	private static final class Efecto extends MobEffect {
		Efecto(int color) {
			super(MobEffectCategory.HARMFUL, color);
		}
	}
}
