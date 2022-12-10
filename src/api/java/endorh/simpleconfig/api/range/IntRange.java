package endorh.simpleconfig.api.range;

import endorh.simpleconfig.api.range.AbstractRange.IntRangeImpl;
import org.jetbrains.annotations.NotNull;

import java.util.Random;

/**
 * A range of {@code int} values.
 */
public interface IntRange extends NumberRange<Integer, IntRange> {
	/**
	 * Empty range: (0, 0).
	 */
	IntRange EMPTY = new IntRangeImpl(0, 0, true, true);
	/**
	 * Full range: [Integer.MIN_VALUE, Integer.MAX_VALUE].
	 */
	IntRange FULL = new IntRangeImpl(Integer.MIN_VALUE, Integer.MAX_VALUE, false, false);
	/**
	 * Closed unit range: [0, 1].
	 */
	IntRange UNIT = new IntRangeImpl(0, 1, false, false);
	
	/**
	 * Create a closed range with the given bounds.
	 */
	static @NotNull IntRange inclusive(int min, int max) {
		return new IntRangeImpl(min, max, false, false);
	}
	
	/**
	 * Create an open range with the given bounds.
	 */
	static @NotNull IntRange exclusive(int min, int max) {
		return new IntRangeImpl(min, max, true, true);
	}
	
	/**
	 * Create a range with exactly one value.
	 */
	static @NotNull IntRange exactly(int value) {
		return new IntRangeImpl(value, value, false, false);
	}
	
	/**
	 * Create a range from 0 (inclusive) to the given value (exclusive).
	 */
	static @NotNull IntRange range(int upToExclusive) {
		return new IntRangeImpl(0, upToExclusive, false, true);
	}
	
	/**
	 * Create a range from 0 (exclusive) to the given value (inclusive).
	 */
	static @NotNull IntRange rangeFrom1(int upToInclusive) {
		return new IntRangeImpl(0, upToInclusive, true, false);
	}
	
	@Override Integer randomUniform(Random random);
	
	@Override Integer randomGaussian(Random random);
	
	/**
	 * Translate by the given amount.
	 */
	@NotNull default IntRange translate(int translation) {
		return translate((Integer) translation);
	}
	
	@Override @NotNull IntRange translate(@NotNull Integer translation);
	
	/**
	 * Grow by the given factor in each direction.
	 */
	@NotNull default IntRange growRelative(int left, int right) {
		return growRelative((Integer) left, (Integer) right);
	}
	
	/**
	 * Grow by the given factor in both directions.
	 */
	@NotNull default IntRange growRelative(int both) {
		return growRelative((Integer) both);
	}
	
	@Override @NotNull IntRange growRelative(@NotNull Integer left, @NotNull Integer right);
	
	/**
	 * Grow by the given amount in each direction.
	 */
	@NotNull default IntRange grow(int left, int right) {
		return grow((Integer) left, (Integer) right);
	}
	
	/**
	 * Grow by the given amount in both directions.
	 */
	@NotNull default IntRange grow(int both) {
		return grow((Integer) both);
	}
	
	@Override @NotNull IntRange grow(@NotNull Integer left, @NotNull Integer right);
	
	/**
	 * Shrink by the given factor in each direction.
	 */
	@NotNull default IntRange shrinkRelative(int left, int right) {
		return shrinkRelative((Integer) left, (Integer) right);
	}
	
	/**
	 * Shrink by the given factor in both directions.
	 */
	@NotNull default IntRange shrinkRelative(int both) {
		return shrinkRelative((Integer) both);
	}
	
	@Override @NotNull IntRange shrinkRelative(@NotNull Integer left, @NotNull Integer right);
	
	/**
	 * Shrink by the given amount in each direction.
	 */
	@NotNull default IntRange shrink(int left, int right) {
		return shrink((Integer) left, (Integer) right);
	}
	
	/**
	 * Shrink by the given amount in both directions.
	 */
	@NotNull default IntRange shrink(int both) {
		return shrink((Integer) both);
	}
	
	@Override @NotNull IntRange shrink(@NotNull Integer left, @NotNull Integer right);
	
	@Override @NotNull IntRange create(
	  @NotNull Integer min, @NotNull Integer max, boolean exclusiveMin, boolean exclusiveMax
	);
	
	default int getIntSize() {
		return (int) getSize();
	}
	
	default int getIntCenter() {
		return (int) getCenter();
	}
	
	default int getIntMin() {
		return getMin();
	}
	
	default int getIntMax() {
		return getMax();
	}
}
