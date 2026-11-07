package net.cloudruleta.compat;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Sustituto de la anotación de Forge: marca un método estático que recibe un evento de {@link Bus}.
 * Mismos atributos que en Forge para que los manejadores del mod original se conserven tal cual.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface SubscribeEvent {
	EventPriority priority() default EventPriority.NORMAL;

	boolean receiveCanceled() default false;
}
