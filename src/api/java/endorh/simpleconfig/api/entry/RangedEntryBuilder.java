package endorh.simpleconfig.api.entry;

import endorh.simpleconfig.api.ConfigEntryBuilder;
import it.unimi.dsi.fastutil.doubles.Double2DoubleFunction;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.DoubleUnaryOperator;
import java.util.function.Function;

public interface RangedEntryBuilder<
  V extends Comparable<V>, Config, Gui,
  Self extends RangedEntryBuilder<V, Config, Gui, Self>
> extends ConfigEntryBuilder<@NotNull V, Config, Gui, Self> {
	/**
	 * Set min (inclusive)
	 */
	@Contract(pure=true) @NotNull Self min(V min);
	
	/**
	 * Set max (inclusive)
	 */
	@Contract(pure=true) @NotNull Self max(V max);
	
	/**
	 * Set range (inclusive)
	 */
	@Contract(pure=true) @NotNull Self range(@Nullable V min, @Nullable V max);
	
	/**
	 * Display as slider
	 */
	@Contract(pure=true) @NotNull Self slider();
	
	/**
	 * Display or not as slider
	 */
	@Contract(pure=true) @NotNull Self slider(boolean asSlider);
	
	/**
	 * Display as slider with given translation key as slider text.
	 */
	@Contract(pure=true) @NotNull Self slider(String sliderTextTranslation);
	
	/**
	 * Display as slider with given text supplier.
	 */
	@Contract(pure=true) @NotNull Self slider(Function<V, Component> sliderTextSupplier);
	
	/**
	 * Set a different range (inclusive) for the slider.<br>
	 * Will be constrained by the range set by {@link #range}.<br>
	 * Useful to display a recommended/commonly useful range of values,
	 * while allowing users to override this range by manually entering
	 * a value beyond it (but still within the normal range).<br>
	 * Implies {@link #slider()}.
	 */
	@Contract(pure=true) @NotNull Self sliderRange(V min, V max);
	
	/**
	 * Set a map used to transform slider values.<br>
	 * Will be clamped to the [0, 1] range.<br>
	 * Can be used to use a logarithmic or sqrt scale for the slider,
	 * allowing for finer control over small values.<br>
	 * Implies {@link #slider()}.
	 * @param map Function applied to the slider value (as a double ∈ [0, 1])
	 *   before mapping it to a value in the range of this entry.
	 * @param inverse Inverse of the map function. Used to determine where the
	 *   slider should be from its value. Must be the inverse of the map.
	 */
	@Contract(pure=true) default @NotNull Self sliderMap(
	  DoubleUnaryOperator map, DoubleUnaryOperator inverse
	) {
		return sliderMap(InvertibleDouble2DoubleFunction.of(map, inverse));
	}
	
	/**
	 * Set a map used to transform slider values.<br>
	 * Will be clamped to the [0, 1] range.<br>
	 * Can be used to use a logarithmic or sqrt scale for the slider,
	 * allowing for finer control over small values.<br>
	 * Implies {@link #slider()}.
	 *
	 * @param map Invertible map applied to the slider value (as a double ∈ [0, 1])
	 *   before mapping it to a value in the range of this entry.
	 *   The inverse of the map will be used to determine where the
	 *   slider should be from its value.
	 */
	@Contract(pure=true) @NotNull Self sliderMap(@Nullable InvertibleDouble2DoubleFunction map);
	
	public interface InvertibleDouble2DoubleFunction extends Double2DoubleFunction {
		static InvertibleDouble2DoubleFunction identity() {
			Double2DoubleFunction identity = Double2DoubleFunction.identity();
			return of(identity, identity);
		}
		static InvertibleDouble2DoubleFunction of(DoubleUnaryOperator map, DoubleUnaryOperator inverse) {
			return new InvertibleDouble2DoubleFunction() {
				@Override public double get(double key) {
					return map.applyAsDouble(key);
				}
				
				@Override public double inverse(double value) {
					return inverse.applyAsDouble(value);
				}
			};
		}
		
		static InvertibleDouble2DoubleFunction neg() {
			return of(d -> -d, d -> -d);
		}
		static InvertibleDouble2DoubleFunction sum(double summand) {
			return of(d -> d + summand, d -> d - summand);
		}
		static InvertibleDouble2DoubleFunction sqrt() {
			return of(Math::sqrt, d -> d * d);
		}
		static InvertibleDouble2DoubleFunction pow(double power) {
			return of(d -> Math.pow(d, power), d -> Math.pow(d, 1 / power));
		}
		static InvertibleDouble2DoubleFunction expMap(double width) {
			double exp = Math.exp(width);
			return of(f -> (Math.exp(width * f) - 1) / (exp - 1),
			          f -> Math.log((exp - 1) * f + 1) / width);
		}
		
		double inverse(double value);
		default InvertibleDouble2DoubleFunction inverse() {
			return of(this::inverse, this);
		}
		
		default InvertibleDouble2DoubleFunction andThen(InvertibleDouble2DoubleFunction after) {
			return of(andThen((DoubleUnaryOperator) after), inverse().composeDouble(after.inverse()));
		}
		
		default InvertibleDouble2DoubleFunction compose(InvertibleDouble2DoubleFunction before) {
			return of(compose((DoubleUnaryOperator) before), inverse().andThen(before.inverse()));
		}
	}
}
