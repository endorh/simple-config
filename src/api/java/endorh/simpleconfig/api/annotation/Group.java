package endorh.simpleconfig.api.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Mark a <b>static</b> inner class as the backing class for a
 * generated config group<br><br>
 *
 * If the group is already defined in the config builder, this
 * annotation is unnecessary (and ignored), and the class will
 * be parsed for backing fields equally<br><br>
 *
 * You may want to define a <b>marker field</b> for this group, if you wish
 * to insert it between entries. A marker field is a field of
 * {@link Void} type, annotated with {@link Group}, and the same name
 * as this group, followed by '{@code $marker}'.<br>
 *
 * @see Category
 * @see Entry
 */
@Target({ElementType.TYPE, ElementType.FIELD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Group {
	/**
	 * The preferred order where to place this group relative to its siblings, including
	 * entries at the same level.<br>
	 * By default, it's added in random order, since Java does not
	 * allow to travel inner classes in source order.<br><br>
	 * It's recommended you use a marker field instead (see the documentation for {@link Group}).
	 */
	int value() default 0;
	/**
	 * Whether this config group should be expanded in the GUI automatically.
	 */
	boolean expand() default false;
}
