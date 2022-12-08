package endorh.simpleconfig.api.annotation;

import endorh.simpleconfig.api.ConfigEntryBuilder;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Mark a field to prevent it from being automatically assigned
 * as the backing field of a config entry with matching name/path.<br><br>
 * This is useful in the case you may want to transform the
 * type of the config value during baking, instead of using the
 * type of the entry, however, you should consider using a secondary
 * backing field instead.
 * @see Entry
 * @see ConfigEntryBuilder#addField
 */
@Target({ElementType.FIELD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface NotEntry {}
