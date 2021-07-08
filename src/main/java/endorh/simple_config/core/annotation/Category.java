package endorh.simple_config.core.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Mark a <b>static</b> inner class as the backing
 * field for a generated config category<br>
 * If the category is already defined in the config builder, this
 * annotation is unnecessary (and ignored), and the class will
 * be parsed for backing fields anyways<br>
 * If the category builder declares a different backing class
 * for the category, there mustn't be an inner class with the
 * same name under the main config's backing class, that is,
 * a category can not have its backing class defined in two places<br>
 * @see Group
 * @see Entry
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Category {
	int value() default 0;
}
