package endorh.simpleconfig.api.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;

/**
 * Suggest a list of values for a String entry.<br>
 * @see Entry
 */
@Target({ElementType.FIELD, ElementType.TYPE_USE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Suggest {
	/**
	 * List of default values for this entry.
	 */
	String[] value() default {};
	
	/**
	 * Method found in this class (or any outer classes) that returns a list
	 * of suggestions.<br>
	 * It should be a <b>static</b> method with signature
	 * {@code List<String>()}, that is, it should take no arguments
	 * and return a {@link List} of {@link String}s.<br>
	 * The method name may start with a '{@code $}', in which case it will
	 * be considered a suffix to this entry's name, or may be an absolute
	 * reference with a class reference followed by '{@code #}' and the
	 * method name.<br>
	 */
	String method() default "";
}
