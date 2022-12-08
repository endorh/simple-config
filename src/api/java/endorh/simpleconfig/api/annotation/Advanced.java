package endorh.simpleconfig.api.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * Marks an entry as being advanced.<br>
 * Players will be warned when making changes to this entry.
 */
@Target({ElementType.FIELD, ElementType.ANNOTATION_TYPE})
public @interface Advanced {
	/**
	 * Whether the annotated entry is advanced or not
	 */
	boolean value() default true;
}
