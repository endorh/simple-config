package endorh.simpleconfig.core.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Mark this field to throw a fatal error at load time if it belongs
 * to the backing class of a Simple Config, but wasn't bound to a
 * config entry.
 */
@Target({ElementType.FIELD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Bind {}
