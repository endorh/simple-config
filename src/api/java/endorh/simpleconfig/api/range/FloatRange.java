package endorh.simpleconfig.api.range;

import endorh.simpleconfig.api.range.AbstractRange.FloatRangeImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Random;

/**
 * A range of {@code float} values.
 */
public interface FloatRange extends NumberRange<Float, FloatRange> {
	/**
	 * Empty range: (0, 0).
	 */
	FloatRange EMPTY = new FloatRangeImpl(0F, 0F, true, true);
	/**
	 * Full range: [-∞, ∞].
	 */
	FloatRange FULL = new FloatRangeImpl(Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY, false, false);
	/**
	 * Closed unit range: [0, 1].
	 */
	FloatRange UNIT = new FloatRangeImpl(0F, 1F, false, false);
	/**
	 * Half open unit range: [0, 1).
	 */
	FloatRange HALF_OPEN_UNIT = new FloatRangeImpl(0F, 1F, false, true);
	/**
	 * Open unit range: (0, 1).
	 */
	FloatRange OPEN_UNIT = new FloatRangeImpl(0F, 1F, true, true);
	/**
	 * Closed unitary range: [-1, 1].
	 */
	FloatRange UNITARY = new FloatRangeImpl(-1F, 1F, false, false);
	
	/**
	 * Create an inclusive range between min and max.
	 */
	static @NotNull FloatRange inclusive(float min, float max) {
		return new FloatRangeImpl(min, max, false, false);
	}
	
	/**
	 * Create an exclusive range between min and max.
	 */
	static @NotNull FloatRange exclusive(float min, float max) {
		return new FloatRangeImpl(min, max, true, true);
	}
	
	/**
	 * Create a range between min and max with the given exclusivity.
	 */
	static @NotNull FloatRange of(
	  float min, boolean exclusiveMin, float max, boolean exclusiveMax
	) {
		return new FloatRangeImpl(min, max, exclusiveMin, exclusiveMax);
	}
	
	/**
	 * Create a closed range with unbound max.
	 */
	static @NotNull FloatRange minimum(float min) {
		return minimum(min, false);
	}
	
	/**
	 * Create a range with unbound max and the given exclusivity.
	 */
	static @NotNull FloatRange minimum(float min, boolean exclusive) {
		return new FloatRangeImpl(min, Float.POSITIVE_INFINITY, exclusive, false);
	}
	
	/**
	 * Create a closed range with unbound min.
	 */
	static @NotNull FloatRange maximum(float max) {
		return maximum(max, false);
	}
	
	/**
	 * Create a range with unbound min and the given exclusivity.
	 */
	static @NotNull FloatRange maximum(float max, boolean exclusive) {
		return new FloatRangeImpl(Float.NEGATIVE_INFINITY, max, false, exclusive);
	}
	
	/**
	 * Create a range with exactly one value.
	 */
	static @NotNull FloatRange exactly(float value) {
		return new FloatRangeImpl(value, value, false, false);
	}
	
	default float getFloatMin() {
		return getMin();
	}
	
	default float getFloatMax() {
		return getMax();
	}
	
	/**
	 * Generate a uniform random value within this range.<br>
	 * Infinite bounds are clamped to
	 * [-{@link Float#MAX_VALUE}, {@link Float#MAX_VALUE}].<br>
	 * Exclusiveness of the limits is considered.<br>
	 * Uniformity of the distribution might be distorted by the
	 * density of floating point numbers for big ranges.<br>
	 * IF this range is empty, {@code null} is returned.
	 */
	@Override @Nullable Float randomUniform(Random random);
	
	/**
	 * Generate a uniform random value with gaussian distribution centered
	 * on this range, with standard deviation equal to the radius of this
	 * range.<br>
	 * In particular, the returned values can be outside range.<br>
	 * Exclusiveness of the limits is ignored.<br>
	 * If this range is empty, {@code null} is returned.
	 * Infinite bounds are clamped to
	 * [-{@link Float#MAX_VALUE}, {@link Float#MAX_VALUE}].
	 */
	@Override @Nullable Float randomGaussian(Random random);
	
	@Override @NotNull FloatRange create(
	  @NotNull Float min, @NotNull Float max, boolean exclusiveMin, boolean exclusiveMax
	);
	
	default float getFloatSize() {
		return (float) getSize();
	}
	
	/**
	 * Translate by the given amount.
	 */
	@NotNull default FloatRange translate(float translation) {
		return translate((Float) translation);
	}
	
	@Override @NotNull FloatRange translate(@NotNull Float translation);
	
	/**
	 * Grow by the given factor in each direction.
	 */
	@NotNull default FloatRange growRelative(float left, float right) {
		return growRelative((Float) left, (Float) right);
	}
	
	/**
	 * Grow by the given factor in both directions.
	 */
	@NotNull default FloatRange growRelative(float both) {
		return growRelative((Float) both);
	}
	
	@Override @NotNull FloatRange growRelative(@NotNull Float left, @NotNull Float right);
	
	/**
	 * Grow by the given amount in each direction.
	 */
	@NotNull default FloatRange grow(float left, float right) {
		return grow((Float) left, (Float) right);
	}
	
	/**
	 * Grow by the given amount in both directions.
	 */
	@NotNull default FloatRange grow(float both) {
		return grow((Float) both);
	}
	
	@Override @NotNull FloatRange grow(@NotNull Float left, @NotNull Float right);
	
	/**
	 * Shrink by the given factor in each direction.
	 */
	@NotNull default FloatRange shrinkRelative(float left, float right) {
		return shrinkRelative((Float) left, (Float) right);
	}
	
	/**
	 * Shrink by the given factor in both directions.
	 */
	@NotNull default FloatRange shrinkRelative(float both) {
		return shrinkRelative((Float) both);
	}
	
	@Override @NotNull FloatRange shrinkRelative(@NotNull Float left, @NotNull Float right);
	
	/**
	 * Shrink by the given amount in each direction.
	 */
	@NotNull default FloatRange shrink(float left, float right) {
		return shrink((Float) left, (Float) right);
	}
	
	/**
	 * Shrink by the given amount in both directions.
	 */
	@NotNull default FloatRange shrink(float both) {
		return shrink((Float) both);
	}
	
	@Override @NotNull FloatRange shrink(@NotNull Float left, @NotNull Float right);
	
	default float getFloatCenter() {
		float min = Math.max(getMin(), -Float.MAX_VALUE);
		float max = Math.min(getMax(), Float.MAX_VALUE);
		return (min + max) / 2F;
	}
	
	@Override double getCenter();
}
