package endorh.simpleconfig.api.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Mark this field/method as expected to be bound by Simple Config to
 * some config entry. Failure to do so will result in a load-time error.<br>
 * Can help prevent common binding mistakes, such as misspelled names.
 */
@Target({ElementType.FIELD, ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Bind {}
