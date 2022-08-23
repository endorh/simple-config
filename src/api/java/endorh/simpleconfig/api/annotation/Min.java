package endorh.simpleconfig.api.annotation;

import endorh.simpleconfig.api.entry.RangedEntryBuilder;
import endorh.simpleconfig.api.entry.RangedListEntryBuilder;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * If this field generates a {@link RangedEntryBuilder}
 * or {@link RangedListEntryBuilder}, this annotation
 * sets its minimum value (inclusive)<br>
 * The value is converted from double to the correspondent type
 * @see Entry
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Min {
	double value() default Double.NEGATIVE_INFINITY;
}
