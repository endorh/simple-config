package dnj.simple_config.core.annotation;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * If this field generates a {@link dnj.simple_config.core.entry.RangedEntry}
 * or {@link dnj.simple_config.core.entry.RangedListEntry}, this annotation
 * sets its maximum value (inclusive)<br>
 * The value is converted from double to the correspondent type
 * @see Entry
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Max {
	double value() default Double.POSITIVE_INFINITY;
}
