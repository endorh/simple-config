package endorh.simpleconfig.api.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Use a method error with the given name for this entry.<br>
 * It's recommended you use instead the {@link Configure} annotation
 * to define your own annotations which will apply custom error messages.
 */
@Target({ElementType.FIELD, ElementType.TYPE_USE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Error {
	/**
	 * Method reference to the error method, either global, by prefixing
	 * a class name with '{@code #}', or relative to this entry.<br>
	 * If prefixed by '{@code $}', it'll be relativized to the entry's name.
	 */
	String method();
}
