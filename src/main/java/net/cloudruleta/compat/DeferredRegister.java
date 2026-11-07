package net.cloudruleta.compat;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Supplier;

/**
 * Sustituto de DeferredRegister de Forge. En Fabric se registra en el momento (al iniciar el mod), así que
 * register() crea el objeto y lo inscribe en el registro de Minecraft enseguida.
 */
public final class DeferredRegister<T> {
	private final Registry<T> registry;
	private final String namespace;

	private DeferredRegister(Registry<T> registry, String namespace) {
		this.registry = registry;
		this.namespace = namespace;
	}

	public static <T> DeferredRegister<T> create(Registry<T> registry, String namespace) {
		return new DeferredRegister<>(registry, namespace);
	}

	public <I extends T> RegistryObject<I> register(String name, Supplier<I> supplier) {
		ResourceLocation id = ResourceLocation.fromNamespaceAndPath(namespace, name);
		Holder<T> holder = Registry.registerForHolder(registry, id, supplier.get());
		@SuppressWarnings("unchecked")
		Holder<I> h = (Holder<I>) holder;
		return new RegistryObject<>(id, h);
	}

	/** En Forge enganchaba el registro al bus del mod; aquí ya está todo registrado. */
	public void register() {
	}
}
