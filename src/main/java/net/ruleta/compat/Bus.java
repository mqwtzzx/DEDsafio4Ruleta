package net.ruleta.compat;

import net.ruleta.Ruleta;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Bus de eventos que sustituye a MinecraftForge.EVENT_BUS. Los manejadores del mod original
 * (métodos estáticos con {@link SubscribeEvent}) se registran con {@link #register(Class)} y los eventos
 * los publican los puntos de enganche de Fabric (callbacks de Fabric API y mixins) con {@link #post}.
 * Respeta la prioridad y receiveCanceled como Forge.
 */
public final class Bus {
	private Bus() {}

	private record Listener(EventPriority priority, boolean receiveCanceled, int order, Consumer<Event> handler, String name) {
	}

	private static final Map<Class<?>, List<Listener>> LISTENERS = new HashMap<>();
	/** Lista ya resuelta por clase concreta de evento (incluye los manejadores de sus superclases, como en Forge). */
	private static final Map<Class<?>, List<Listener>> RESUELTOS = new ConcurrentHashMap<>();
	private static int order;

	/** Registra todos los métodos estáticos con @SubscribeEvent de la clase (como EVENT_BUS.register(Clase.class)). */
	public static synchronized void register(Class<?> holder) {
		MethodHandles.Lookup lookup = MethodHandles.lookup();
		for (Method m : holder.getDeclaredMethods()) {
			SubscribeEvent ann = m.getAnnotation(SubscribeEvent.class);
			if (ann == null)
				continue;
			if (!Modifier.isStatic(m.getModifiers()) || m.getParameterCount() != 1 || !Event.class.isAssignableFrom(m.getParameterTypes()[0]))
				throw new IllegalStateException("Manejador no válido: " + holder.getName() + "." + m.getName());
			try {
				m.setAccessible(true);
				MethodHandle mh = lookup.unreflect(m);
				String name = holder.getSimpleName() + "." + m.getName();
				Consumer<Event> c = e -> {
					try {
						mh.invoke(e);
					} catch (RuntimeException | Error ex) {
						throw ex;
					} catch (Throwable t) {
						throw new RuntimeException(t);
					}
				};
				add(m.getParameterTypes()[0], new Listener(ann.priority(), ann.receiveCanceled(), order++, c, name));
			} catch (IllegalAccessException ex) {
				throw new IllegalStateException(ex);
			}
		}
	}

	/** Equivalente a EVENT_BUS.addListener(Consumer) con el tipo explícito. */
	@SuppressWarnings("unchecked")
	public static synchronized <T extends Event> void addListener(Class<T> type, Consumer<T> listener) {
		add(type, new Listener(EventPriority.NORMAL, false, order++, e -> listener.accept((T) e), type.getSimpleName()));
	}

	private static void add(Class<?> type, Listener l) {
		LISTENERS.computeIfAbsent(type, k -> new ArrayList<>()).add(l);
		RESUELTOS.clear();
	}

	/** Manejadores de la clase del evento y de todas sus superclases, ordenados por prioridad y orden de registro. */
	private static synchronized List<Listener> resolver(Class<?> tipo) {
		List<Listener> todos = new ArrayList<>();
		for (Class<?> c = tipo; c != null && Event.class.isAssignableFrom(c); c = c.getSuperclass()) {
			List<Listener> l = LISTENERS.get(c);
			if (l != null)
				todos.addAll(l);
		}
		todos.sort(Comparator.comparing(Listener::priority).thenComparingInt(Listener::order));
		return List.copyOf(todos);
	}

	/**
	 * Publica el evento. Como en Forge, lo reciben también los manejadores registrados para una superclase
	 * (p. ej. un manejador de EntityPlaceEvent recibe los EntityMultiPlaceEvent). Devuelve si quedó cancelado.
	 */
	public static <T extends Event> boolean post(T event) {
		List<Listener> list = RESUELTOS.computeIfAbsent(event.getClass(), Bus::resolver);
		if (list.isEmpty())
			return event.isCanceled();
		for (Listener l : list) {
			if (event.isCanceled() && !l.receiveCanceled())
				continue;
			try {
				l.handler().accept(event);
			} catch (RuntimeException ex) {
				// como en Forge, un manejador que falla no debe tumbar el juego entero sin rastro
				Ruleta.LOG.error("Error en el manejador {} de {}", l.name(), event.getClass().getSimpleName(), ex);
				throw ex;
			}
		}
		return event.isCanceled();
	}

	/** ¿Hay algún manejador para este tipo? (evita crear eventos en bucles calientes). */
	public static boolean hasListeners(Class<? extends Event> type) {
		return !RESUELTOS.computeIfAbsent(type, Bus::resolver).isEmpty();
	}
}
