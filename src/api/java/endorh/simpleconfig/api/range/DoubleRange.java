package endorh.simpleconfig.api.range;

import endorh.simpleconfig.api.range.AbstractRange.DoubleRangeImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Random;

/**
 * A range of {@code double} values.
 */
public interface DoubleRange extends NumberRange<Double, DoubleRange> {
	/**
	 * Empty range: (0, 0).
	 */
	DoubleRange EMPTY = new DoubleRangeImpl(0D, 0D, true, true);
	/**
	 * Full range: [-∞, ∞].
	 */
	DoubleRange FULL = new DoubleRangeImpl(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, false, false);
	/**
	 * Closed unit range: [0, 1].
	 */
	DoubleRange UNIT = new DoubleRangeImpl(0D, 1D, false, false);
	/**
	 * Half open unit range: [0, 1).
	 */
	DoubleRange HALF_OPEN_UNIT = new DoubleRangeImpl(0D, 1D, false, true);
	/**
	 * Open unit range: (0, 1).
	 */
	DoubleRange OPEN_UNIT = new DoubleRangeImpl(0D, 1D, true, true);
	/**
	 * Closed unitary range: [-1, 1].
	 */
	DoubleRange UNITARY = new DoubleRangeImpl(-1D, 1D, false, false);
	
	/**
	 * Create an inclusive range between min and max.
	 */
	static @NotNull DoubleRange inclusive(double min, double max) {
		return new DoubleRangeImpl(min, max, false, false);
	}
	
	/**
	 * Create an exclusive range between min and max.
	 */
	static @NotNull DoubleRange exclusive(double min, double max) {
		return new DoubleRangeImpl(min, max, true, true);
	}
	
	/**
	 * Create a range between min and max, with the given exclusiveness.
	 */
	static @NotNull DoubleRange of(
	  double min, boolean exclusiveMin, double max, boolean exclusiveMax
	) {
		return new DoubleRangeImpl(min, max, exclusiveMin, exclusiveMax);
	}
	
	/**
	 * Create an inclusive range without maximum bound.
	 */
	static @NotNull DoubleRange minimum(double min) {
		return minimum(min, false);
	}
	
	/**
	 * Create a range without maximum bound, with the given exclusiveness.
	 */
	static @NotNull DoubleRange minimum(double min, boolean exclusive) {
		return new DoubleRangeImpl(min, Double.POSITIVE_INFINITY, exclusive, false);
	}
	
	/**
	 * Create an inclusive range without minimum bound.
	 */
	static @NotNull DoubleRange maximum(double max) {
		return maximum(max, false);
	}
	
	/**
	 * Create a range without minimum bound, with the given exclusiveness.
	 */
	static @NotNull DoubleRange maximum(double max, boolean exclusive) {
		return new DoubleRangeImpl(Double.NEGATIVE_INFINITY, max, false, exclusive);
	}
	
	/**
	 * Create an inclusive range with a single value.
	 */
	static @NotNull DoubleRange exactly(double value) {
		return new DoubleRangeImpl(value, value, false, false);
	}
	
	default double getDoubleMin() {
		return getMin();
	}
	
	default double getDoubleMax() {
		return getMax();
	}
	
	/**
	 * Generate a uniform random value within this range.<br>
	 * Infinite bounds are clamped to
	 * [-{@link Double#MAX_VALUE}, {@link Double#MAX_VALUE}].<br>
	 * Exclusiveness of the limits is considered.<br>
	 * Uniformity of the distribution might be distorted by the
	 * density of floating point numbers for big ranges.<br>
	 * If this range is empty, {@code null} is returned.
	 */
	@Override @Nullable Double randomUniform(Random random);
	
	@Override @Nullable Double randomGaussian(Random random);
	
	@Override @NotNull DoubleRange create(
	  @NotNull Double min, @NotNull Double max, boolean exclusiveMin, boolean exclusiveMax
	);
	
	/**
	 * Translate by the given offset.
	 */
	@NotNull default DoubleRange translate(double translation) {
		return translate((Double) translation);
	}
	
	@Override @NotNull DoubleRange translate(@NotNull Double translation);
	
	/**
	 * Grow by the given factor in each direction.
	 */
	@NotNull default DoubleRange growRelative(double left, double right) {
		return growRelative((Double) left, (Double) right);
	}
	
	/**
	 * Grow by the given factor in both directions.
	 */
	@NotNull default DoubleRange growRelative(double both) {
		return growRelative((Double) both);
	}
	
	@Override @NotNull DoubleRange growRelative(@NotNull Double left, @NotNull Double right);
	
	/**
	 * Grow by the given amount in each direction.
	 */
	@NotNull default DoubleRange grow(double left, double right) {
		return grow((Double) left, (Double) right);
	}
	
	/**
	 * Grow by the given amount in both directions.
	 */
	@NotNull default DoubleRange grow(double both) {
		return grow((Double) both);
	}
	
	@Override @NotNull DoubleRange grow(@NotNull Double left, @NotNull Double right);
	
	/**
	 * Shrink by the given factor in each direction.
	 */
	@NotNull default DoubleRange shrinkRelative(double left, double right) {
		return shrinkRelative((Double) left, (Double) right);
	}
	
	/**
	 * Shrink by the given factor in both directions.
	 */
	@NotNull default DoubleRange shrinkRelative(double both) {
		return shrinkRelative((Double) both);
	}
	
	@Override @NotNull DoubleRange shrinkRelative(@NotNull Double left, @NotNull Double right);
	
	/**
	 * Shrink by the given amount in each direction.
	 */
	@NotNull default DoubleRange shrink(double left, double right) {
		return shrink((Double) left, (Double) right);
	}
	
	/**
	 * Shrink by the given amount in both directions.
	 */
	@NotNull default DoubleRange shrink(double both) {
		return shrink((Double) both);
	}
	
	@Override @NotNull DoubleRange shrink(@NotNull Double left, @NotNull Double right);
	
	@Override double getCenter();
}
