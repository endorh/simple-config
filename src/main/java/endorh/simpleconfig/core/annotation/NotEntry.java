package endorh.simpleconfig.core.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Mark a field to prevent it from being automatically assigned
 * as the backing field of a config entry with matching name/path<br>
 * This is useful in the case you may want to transform the
 * type of the config value during baking, instead of using the
 * type of the entry
 * @see Entry
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface NotEntry {}
