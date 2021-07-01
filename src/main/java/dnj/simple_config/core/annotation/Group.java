package dnj.simple_config.core.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Mark a <b>static</b> inner class as the backing
 * field for a generated config group<br>
 * If the group is already defined in the config builder, this
 * annotation is unnecessary (and ignored), and the class will
 * be parsed for backing fields anyways
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Group {
	/**
	 * Expand this config group in the GUI automatically
	 */
	boolean expand() default false;
}
