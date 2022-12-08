package endorh.simpleconfig.api.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a config entry as the caption of its group/bean.<br><br>
 *
 * Only one caption can be defined per group/bean.<br>
 * If a config entry is annotated with @Caption it doesn't need to be
 * annotated with @Entry.
 */
@Target({ElementType.FIELD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Caption {}
