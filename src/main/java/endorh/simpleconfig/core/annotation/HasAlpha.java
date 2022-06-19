package endorh.simpleconfig.core.annotation;

import endorh.simpleconfig.core.entry.ColorEntry;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * If this field generates a {@link ColorEntry},
 * it will be configured to accept alpha component values
 * @see Entry
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface HasAlpha {}
