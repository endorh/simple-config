package endorh.simpleconfig.api.annotation;

import endorh.simpleconfig.api.SimpleConfigBuilder;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Mark a <b>static</b> field as a config entry<br>
 * The type of the field must be one of the supported
 * <ul>
 *    <li>{@code boolean} (or {@code Boolean})</li>
 *    <li>{@code String}</li>
 *    <li>{@code Enum} (any {@code enum} type)</li>
 *    <li>{@code int} (or {@code Integer})</li>
 *    <li>{@code long} (or {@code Long})</li>
 *    <li>{@code float} (or {@code Float})</li>
 *    <li>{@code double} (or {@code Double})</li>
 *    <li>{@link java.awt.Color}</li>
 * </ul>
 * Additionally, a few list types are also supported
 * <ul>
 *    <li>{@code List<String>}</li>
 *    <li>{@code List<Integer>}</li>
 *    <li>{@code List<Long>}</li>
 *    <li>{@code List<Float>}</li>
 *    <li>{@code List<Double>}</li>
 * </ul>
 * To define more complex lists, such as lists of other entry types
 * (including lists of lists), use the builder<br>
 * Also, the {@code Byte} and {@code Short} types are supported,
 * but deprecated<br>
 * A config entry with the name and type of this field will
 * be created on this context.<br><br>
 *
 * Some annotations, like {@link Min}, {@link Max}, {@link Slider},
 * {@link HasAlpha}, {@link RequireRestart} or {@link NonPersistent}
 * allow to configure some settings of the generated entries
 * for the types that support them<br>
 *
 * A sibling method with the same name followed by '{@code $error}'
 * may be defined, accepting the type of this field and
 * returning an {@code Optional<Component>} with an
 * optional error message<br>
 * Likewise, a sibling method with the suffix '{@code $tooltip}'
 * returning instead an {@code Optional<Component[]>} with
 * an optional tooltip may be defined as well<br>
 * <b>Remember that entries get automatically mapped tooltip
 * translation keys</b>. Use dynamic tooltips only when
 * necessary<br>
 * For list entry types it's also possible to have a sibling method with
 * the same name, followed by '{@code $validate}' which accepts list
 * elements and may return either a boolean (true for valid) or an
 * {@code Optional<Component>} with an optional error message,
 * instead of using the '{@code $error}' method, which takes the whole list<br><br>
 *
 * <b>You're encouraged</b> to generate your entries with
 * the {@link SimpleConfigBuilder} methods
 * instead, because it provides more options and is less prone
 * to errors<br>
 * However, for simpler configs, you might want to check out other
 * annotations for different entry types<br>
 * @see Text
 * @see Group
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Entry {
	/**
	 * The preferred index where to add this entry, relative to its siblings,
	 * including config groups on the same level<br>
	 * Should only be necessary when dealing with nested groups in between
	 * entries, but the Java language specification won't let me guarantee that
	 */
	int value() default 0;
	
}
