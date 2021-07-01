package dnj.simple_config.core.annotation;

import dnj.simple_config.core.SimpleConfigBuilder;

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
 * </ul>
 * A config entry with the name and type of this field will
 * be created on this context.<br><br>
 *
 * A sibling method with the same name followed by '{@code $error}'
 * may be defined, accepting the type of this field and
 * returning an {@code Optional<ITextComponent>} with an
 * optional error message<br>
 * Likewise, a sibling method with the suffix '{@code $tooltip}'
 * returning instead an {@code Optional<ITextComponent[]>} with
 * an optional tooltip may be defined as well<br><br>
 *
 * <b>You're encouraged</b> to generate your entries with
 * the {@link SimpleConfigBuilder} methods
 * instead, because it provides more options and is less prone
 * to errors<br>
 * However, for simpler configs, you might want to check out other
 * annotations for different entry types<br>
 * @see Entry.Long
 * @see Entry.Double
 * @see Entry.Color
 * @see Entry.List
 * @see Entry.List.Long
 * @see Entry.List.Double
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Entry {
	
	/**
	 * Mark a field as a long config entry<br>
	 * The type of the field must be {@code long} or {@code Long}
	 *
	 * A config entry with name of this field will be created on this context<br><br>
	 *
	 * A sibling method with the same name followed by '{@code $error}'
	 * may be defined, accepting the type of this field and
	 * returning an {@code Optional<ITextComponent>} with an
	 * optional error message<br>
	 * Likewise, a sibling method with the suffix '{@code $tooltip}'
	 * returning instead an {@code Optional<ITextComponent[]>} with
	 * an optional tooltip may be defined as well<br><br>
	 *
	 * <b>You're encouraged</b> to generate your entries with
	 * the {@link SimpleConfigBuilder} methods
	 * instead, because it provides more options and is less prone
	 * to errors<br>
	 * However, for simpler configs, you might want to check out other
	 * annotations for different entry types<br>
	 * @see Entry
	 * @see Entry.Double
	 */
	@Target(ElementType.FIELD)
	@Retention(RetentionPolicy.RUNTIME)
	@interface Long {
		/**
		 * The minimum value (inclusive) that this entry may take
		 */
		long min() default java.lang.Long.MIN_VALUE;
		
		/**
		 * The maximum value (inclusive) that this entry may take<br>
		 */
		long max() default java.lang.Long.MAX_VALUE;
		
		/**
		 * Display this entry as a slider in the GUI
		 */
		boolean slider() default false;
	}
	
	/**
	 * Mark a <b>static</b> field as a double config entry<br>
	 * The type of the field must be {@code double} or {@code Double}
	 *
	 * A config entry with name of this field will be created on this context<br>
	 * Double entries do not currently support sliders<br><br>
	 *
	 * A sibling method with the same name followed by '{@code $error}'
	 * may be defined, accepting the type of this field and
	 * returning an {@code Optional<ITextComponent>} with an
	 * optional error message<br>
	 * Likewise, a sibling method with the suffix '{@code $tooltip}'
	 * returning instead an {@code Optional<ITextComponent[]>} with
	 * an optional tooltip may be defined as well<br><br>
	 *
	 * <b>You're encouraged</b> to generate your entries with
	 * the {@link SimpleConfigBuilder} methods
	 * instead, because it provides more options and is less prone
	 * to errors<br>
	 * However, for simpler configs, you might want to check out other
	 * annotations for different entry types<br>
	 * @see Entry
	 * @see Entry.Long
	 */
	@Target(ElementType.FIELD)
	@Retention(RetentionPolicy.RUNTIME)
	@interface Double {
		/**
		 * The minimum value (inclusive) that this entry may take
		 */
		double min() default java.lang.Double.NEGATIVE_INFINITY;
		
		/**
		 * The maximum value (inclusive) that this entry may take<br>
		 */
		double max() default java.lang.Double.POSITIVE_INFINITY;
	}
	
	
	
	/**
	 * Mark a <b>static</b> field as a color config entry<br>
	 * The type of the field must be {@link java.awt.Color}
	 *
	 * A config entry with name of this field will be created on this context<br><br>
	 *
	 * A sibling method with the same name followed by '{@code $error}'
	 * may be defined, accepting the type of this field and
	 * returning an {@code Optional<ITextComponent>} with an
	 * optional error message<br>
	 * Likewise, a sibling method with the suffix '{@code $tooltip}'
	 * returning instead an {@code Optional<ITextComponent[]>} with
	 * an optional tooltip may be defined as well<br><br>
	 *
	 * <b>You're encouraged</b> to generate your entries with
	 * the {@link SimpleConfigBuilder} methods
	 * instead, because it provides more options and is less prone
	 * to errors<br>
	 * However, for simpler configs, you might want to check out other
	 * annotations for different entry types<br>
	 * @see Entry
	 */
	@Target(ElementType.FIELD)
	@Retention(RetentionPolicy.RUNTIME)
	@interface Color {
		/**
		 * Allow an alpha component in this color
		 */
		boolean alpha() default false;
	}
	
	/**
	 * Mark a <b>static</b> field as a list config entry<br>
	 * Only a few list types are supported like this:
	 * <ul>
	 *    <li>{@code long} lists</li>
	 *    <li>{@code double} lists</li>
	 *    <li>{@code String} lists</li>
	 * </ul>
	 * You may also use the specific annotations {@link Entry.List.Long}
	 * and {@link Entry.List.Double} to provide a minimum and maximum
	 * value for the allowed elements<br>
	 * A config entry with name of this field will be created on this context<br>
	 * It is also possible to have a sibling method with the same name,
	 * followed by '{@code $validate}' which accepts list elements and may
	 * return either a boolean (true for valid) or an
	 * {@code Optional<ITextComponent>} with an optional error message<br><br>
	 *
	 * A sibling method with the same name followed by '{@code $error}'
	 * may be defined, accepting the type of this field and
	 * returning an {@code Optional<ITextComponent>} with an
	 * optional error message<br>
	 * Likewise, a sibling method with the suffix '{@code $tooltip}'
	 * returning instead an {@code Optional<ITextComponent[]>} with
	 * an optional tooltip may be defined as well<br><br>
	 *
	 * <b>You're encouraged</b> to generate your entries with
	 * the {@link SimpleConfigBuilder} methods
	 * instead, because it provides more options and is less prone
	 * to errors<br>
	 * However, for simpler configs, you might want to check out other
	 * annotations for different entry types<br>
	 * @see Entry
	 * @see Entry.List.Long
	 * @see Entry.List.Double
	 */
	@Target(ElementType.FIELD)
	@Retention(RetentionPolicy.RUNTIME)
	@interface List {
		
		/**
		 * Mark a <b>static</b> field as a long list config entry<br>
		 * The field must be of type {@code List<Long>}<br>
		 * A config entry with name of this field will be created on this context<br>
		 * It is also possible to have a sibling method with the same name,
		 * followed by '{@code $validate}' which accepts list elements and may
		 * return either a boolean (true for valid) or an
		 * {@code Optional<ITextComponent>} with an optional error message<br><br>
		 *
		 * A sibling method with the same name followed by '{@code $error}'
		 * may be defined, accepting the type of this field and
		 * returning an {@code Optional<ITextComponent>} with an
		 * optional error message<br>
		 * Likewise, a sibling method with the suffix '{@code $tooltip}'
		 * returning instead an {@code Optional<ITextComponent[]>} with
		 * an optional tooltip may be defined as well<br><br>
		 *
		 * <b>You're encouraged</b> to generate your entries with
		 * the {@link SimpleConfigBuilder} methods
		 * instead, because it provides more options and is less prone
		 * to errors<br>
		 * However, for simpler configs, you might want to check out other
		 * annotations for different entry types<br>
		 * @see Entry
		 * @see Entry.List
		 * @see Entry.List.Double
		 */
		@Target(ElementType.FIELD)
		@Retention(RetentionPolicy.RUNTIME)
		@interface Long {
			/**
			 * The minimum (inclusive) allowed value for the elements of the list
			 */
			long min() default java.lang.Long.MIN_VALUE;
			
			/**
			 * The maximum (inclusive) allowed value for the elements of the list
			 */
			long max() default java.lang.Long.MAX_VALUE;
		}
		
		
		/**
		 * Mark a <b>static</b> field as a double list config entry<br>
		 * The field must be of type {@code List<Double>}<br>
		 * A config entry with name of this field will be created on this context<br>
		 * It is also possible to have a sibling method with the same name,
		 * followed by '{@code $validate}' which accepts list elements
		 * (of type {@code double} or {@code Double}) and may
		 * return either a boolean (true for valid) or an
		 * {@code Optional<ITextComponent>} with an optional error message<br><br>
		 *
		 * A sibling method with the same name followed by '{@code $error}'
		 * may be defined, accepting the type of this field and
		 * returning an {@code Optional<ITextComponent>} with an
		 * optional error message<br>
		 * Likewise, a sibling method with the suffix '{@code $tooltip}'
		 * returning instead an {@code Optional<ITextComponent[]>} with
		 * an optional tooltip may be defined as well<br><br>
		 *
		 * <b>You're encouraged</b> to generate your entries with
		 * the {@link SimpleConfigBuilder} methods
		 * instead, because it provides more options and is less prone
		 * to errors<br>
		 * However, for simpler configs, you might want to check out other
		 * annotations for different entry types<br>
		 * @see Entry
		 * @see Entry.List
		 * @see Entry.List.Long
		 */
		@Target(ElementType.FIELD)
		@Retention(RetentionPolicy.RUNTIME)
		@interface Double {
			/**
			 * The minimum (inclusive) allowed value for the elements of the list
			 */
			double min() default java.lang.Double.MIN_VALUE;
			/**
			 * The maximum (inclusive) allowed value for the elements of the list
			 */
			double max() default java.lang.Double.MAX_VALUE;
		}
	}
}
