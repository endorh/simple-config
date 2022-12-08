package endorh.simpleconfig.api.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Configure the allowed sizes for collection types, including
 * {@link List}s, {@link Set}s and {@link Map}s.
 * @see Entry
 */
@Target({ElementType.FIELD, ElementType.TYPE_USE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Size {
	/**
	 * Inclusive minimum size.
	 */
	int min() default 0;
	/**
	 * Inclusive maximum size.
	 */
	int max() default Integer.MAX_VALUE;
}
