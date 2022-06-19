package endorh.simpleconfig.core.annotation;

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
 * @see Category
 * @see Entry
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Group {
	/**
	 * The preferred order where to place this group relative to its siblings, including
	 * entries at the same level<br>
	 * By default, it's added in random order, since Java does not
	 * allow to travel inner classes in source order.
	 */
	int value() default 0;
	/**
	 * Expand this config group in the GUI automatically
	 */
	boolean expand() default false;
}
