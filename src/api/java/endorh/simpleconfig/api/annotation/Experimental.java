package endorh.simpleconfig.api.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks an entry as being experimental.<br>
 * Players will be warned when making changes to this entry.
 */
@Target({ElementType.FIELD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Experimental {
	/**
	 * Whether the annotated entry is experimental or not
	 */
	boolean value() default true;
}
