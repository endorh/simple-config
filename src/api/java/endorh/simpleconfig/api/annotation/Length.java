package endorh.simpleconfig.api.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Configure the allowed String lengths for String config entries.
 */
@Target({ElementType.ANNOTATION_TYPE, ElementType.FIELD, ElementType.TYPE_USE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Length {
	/**
	 * Minimum (inclusive) length of the entry
	 */
	int min() default 0;

	/**
	 * Maximum (inclusive) length of the entry
	 */
	int max() default Integer.MAX_VALUE;
}
