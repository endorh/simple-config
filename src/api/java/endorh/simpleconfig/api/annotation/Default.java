package endorh.simpleconfig.api.annotation;

import org.intellij.lang.annotations.Language;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Set a default value for a generated config entry in YAML format.<br><br>
 * This is only useful to set the default of inner entries, such as the default
 * value of the inner type of a list entry, which is used to create elements added
 * to the list.<br><br>
 * The default value must match the type of the entry.
 *
 * <pre>{@code
 *    public static List<@Default("[1, 2, 3]") List<@Default("1") Integer>> list =
 *      asList(asList(1, 2, 3), asList(4, 5, 6));
 * }</pre>
 */
@Target({ElementType.FIELD, ElementType.TYPE_USE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Default {
	/**
	 * The default value of the entry in YAML format.<br>
	 * It must match the type of the entry.
	 */
	@Language("yaml") String value();
}
