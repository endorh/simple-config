package endorh.simpleconfig.api.annotation;


import endorh.simpleconfig.api.entry.RangedEntryBuilder;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Configure the minimum value of this entry.<br>
 * Only applicable to numeric entries (subclasses of {@link RangedEntryBuilder}).<br><br>
 * The value is converted from double to the correspondent type
 * @see Entry
 */
@Target({ElementType.FIELD, ElementType.TYPE_USE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Max {
	double value() default Double.POSITIVE_INFINITY;
}
