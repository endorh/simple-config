package endorh.simpleconfig.api.annotation;

import endorh.simpleconfig.api.entry.ColorEntryBuilder;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * If this field generates a {@link ColorEntryBuilder},
 * it will be configured to accept alpha component values
 * @see Entry
 */
@Target({ElementType.FIELD, ElementType.TYPE_USE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface HasAlpha {
	/**
	 * Whether the annotated entry accepts alpha components.
	 */
	boolean value() default true;
}
