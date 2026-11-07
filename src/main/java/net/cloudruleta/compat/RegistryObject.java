package net.cloudruleta.compat;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Supplier;

/**
 * Sustituto de RegistryObject de Forge: el objeto ya registrado, su Holder (en 1.21 los efectos y otros
 * registros se usan como Holder) y su identificador.
 */
public final class RegistryObject<T> implements Supplier<T> {
	private final ResourceLocation id;
	private final Holder<T> holder;

	public RegistryObject(ResourceLocation id, Holder<T> holder) {
		this.id = id;
		this.holder = holder;
	}

	@Override
	public T get() {
		return holder.value();
	}

	public Holder<T> holder() {
		return holder;
	}

	public ResourceLocation getId() {
		return id;
	}
}
