package endorh.simpleconfig.core.annotation;

import endorh.simpleconfig.core.entry.AbstractRangedEntry;
import endorh.simpleconfig.core.entry.RangedListEntry;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * If this field generates a {@link AbstractRangedEntry}
 * or {@link RangedListEntry}, this annotation
 * sets its minimum value (inclusive)<br>
 * The value is converted from double to the correspondent type
 * @see Entry
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Min {
	double value() default Double.NEGATIVE_INFINITY;
}
