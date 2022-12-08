package endorh.simpleconfig.api.annotation;


import endorh.simpleconfig.api.entry.RangedEntryBuilder.InvertibleDouble2DoubleFunction;
import org.jetbrains.annotations.Nullable;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Optional;
import java.util.function.Function;

/**
 * Configure a numeric entry to display a slider.<br>
 * @see Entry
 */
@Target({ElementType.FIELD, ElementType.TYPE_USE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Slider {
	/**
	 * Translation key used to format the slider label.<br>
	 * Receives as argument the current value of the slider.<br><br>
	 * Leaving empty will use the default translation key.<br>
	 * The following translation keys are defined by Simple Config:
	 * <ul>
	 *    <li>{@code "simpleconfig.format.slider"}</li>
	 *    <li>{@code "simpleconfig.format.slider.percentage"}</li>
	 *    <li>{@code "simpleconfig.format.slider.percentage.float"}</li>
	 *    <li>{@code "simpleconfig.format.slider.volume"}</li>
	 * </ul>
	 */
	String label() default "";
	
	/**
	 * The minimum value of the slider, which can be greater than the
	 * minimum value of the entry.<br><br>
	 *
	 * Players will be able to set values beyond the slider bounds
	 * (yet within the entry bounds) by inputting them by text.<br><br>
	 *
	 * This is useful to set recommended ranges for the entry, while
	 * still allowing advanced players to set values beyond the
	 * recommendations.
	 *
	 * @see #max()
	 */
	double min() default Double.NaN;
	
	/**
	 * The maximum value of the slider, which can be smaller than the
	 * maximum value of the entry.<br><br>
	 * <p>
	 * Players will be able to set values beyond the slider bounds
	 * (yet within the entry bounds) by inputting them by text.<br><br>
	 * <p>
	 * This is useful to set recommended ranges for the entry, while
	 * still allowing advanced players to set values beyond the
	 * recommendations.
	 *
	 * @see #min()
	 */
	double max() default Double.NaN;
	
	/**
	 * Slider type, which determines the mapping from the slider
	 * position to the values within the slider range.<br>
	 * By default, this is {@code LINEAR}.<br><br>
	 * Some types, like the {@code POW} and the {@code EXP_MAP} may
	 * accept an additional parameter, specified by {@link #arg()}.
	 */
	SliderType type() default SliderType.LINEAR;
	double arg() default Double.NaN;
	
	enum SliderType {
		LINEAR(null),
		SQRT(InvertibleDouble2DoubleFunction.sqrt()),
		POW(InvertibleDouble2DoubleFunction::pow, 2),
		EXP_MAP(InvertibleDouble2DoubleFunction::expMap, 10);
		
		private final @Nullable InvertibleDouble2DoubleFunction map;
		private final @Nullable Function<Double, InvertibleDouble2DoubleFunction> mapFactory;
		private final double def;
		
		SliderType(@Nullable InvertibleDouble2DoubleFunction map) {
			this.map = map;
			mapFactory = null;
			def = Double.NaN;
		}
		
		SliderType(@Nullable Function<Double, InvertibleDouble2DoubleFunction> mapFactory, double def) {
			this.mapFactory = mapFactory;
			map = null;
			this.def = def;
		}
		
		public static Optional<InvertibleDouble2DoubleFunction> fromAnnotation(Slider a) {
			SliderType type = a.type();
			if (type.map != null) return Optional.of(type.map);
			if (type.mapFactory != null) {
				double arg = a.arg();
				return Optional.of(type.mapFactory.apply(Double.isNaN(arg)? type.def : arg));
			}
			return Optional.empty();
		}
	}
}

