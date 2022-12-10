package endorh.simpleconfig.api.range;

import endorh.simpleconfig.api.range.AbstractRange.LongRangeImpl;
import org.jetbrains.annotations.NotNull;

import java.util.Random;

/**
 * A range of {@code long} values
 */
public interface LongRange extends NumberRange<Long, LongRange> {
	/**
	 * Empty range: (0, 0).
	 */
	LongRange EMPTY = new LongRangeImpl(0, 0, true, true);
	/**
	 * Full range: [Long.MIN_VALUE, Long.MAX_VALUE].
	 */
	LongRange FULL = new LongRangeImpl(Long.MIN_VALUE, Long.MAX_VALUE, false, false);
	/**
	 * Closed unit range: [0, 1]
	 */
	LongRange UNIT = new LongRangeImpl(0, 1, false, false);
	
	/**
	 * Create a closed range between min and max (inclusive).
	 */
	static @NotNull LongRange inclusive(long min, long max) {
		return new LongRangeImpl(min, max, false, false);
	}
	
	/**
	 * Create an open range between min and max (exclusive).
	 */
	static @NotNull LongRange exclusive(long min, long max) {
		return new LongRangeImpl(min, max, true, true);
	}
	
	/**
	 * Create a range with a single value.
	 */
	static @NotNull LongRange exactly(long value) {
		return new LongRangeImpl(value, value, false, false);
	}
	
	/**
	 * Create a range from 0 (inclusive) to the given value (exclusive).
	 */
	static @NotNull LongRange range(long upToExclusive) {
		return new LongRangeImpl(0, upToExclusive, false, true);
	}
	
	/**
	 * Create a range from 0 (exclusive) to the given value (inclusive).
	 */
	static @NotNull LongRange rangeFrom1(long upToInclusive) {
		return new LongRangeImpl(0, upToInclusive, true, false);
	}
	
	@Override Long randomUniform(Random random);
	
	@Override Long randomGaussian(Random random);
	
	/**
	 * Translate by the given amount.
	 */
	@NotNull default LongRange translate(long translation) {
		return translate((Long) translation);
	}
	
	@Override @NotNull LongRange translate(@NotNull Long translation);
	
	/**
	 * Grow by the given factor in each direction.
	 */
	@NotNull default LongRange growRelative(long left, long right) {
		return growRelative((Long) left, (Long) right);
	}
	
	/**
	 * Grow by the given factor in both directions.
	 */
	@NotNull default LongRange growRelative(long both) {
		return growRelative((Long) both);
	}
	
	@Override @NotNull LongRange growRelative(@NotNull Long left, @NotNull Long right);
	
	/**
	 * Grow by the given amount in each direction.
	 */
	@NotNull default LongRange grow(long left, long right) {
		return grow((Long) left, (Long) right);
	}
	
	/**
	 * Grow by the given amount in both directions.
	 */
	@NotNull default LongRange grow(long both) {
		return grow((Long) both);
	}
	
	@Override @NotNull LongRange grow(@NotNull Long left, @NotNull Long right);
	
	/**
	 * Shrink by the given factor in each direction.
	 */
	@NotNull default LongRange shrinkRelative(long left, long right) {
		return shrinkRelative((Long) left, (Long) right);
	}
	
	/**
	 * Shrink by the given factor in both directions.
	 */
	@NotNull default LongRange shrinkRelative(long both) {
		return shrinkRelative((Long) both);
	}
	
	@Override @NotNull LongRange shrinkRelative(@NotNull Long left, @NotNull Long right);
	
	/**
	 * Shrink by the given amount in each direction.
	 */
	@NotNull default LongRange shrink(long left, long right) {
		return shrink((Long) left, (Long) right);
	}
	
	/**
	 * Shrink by the given amount in both directions.
	 */
	@NotNull default LongRange shrink(long both) {
		return shrink((Long) both);
	}
	
	@Override @NotNull LongRange shrink(@NotNull Long left, @NotNull Long right);
	
	@Override @NotNull LongRange create(
	  @NotNull Long min, @NotNull Long max, boolean exclusiveMin, boolean exclusiveMax
	);
	
	default long getLongSize() {
		return (long) getSize();
	}
	
	default long getLongCenter() {
		return (long) getCenter();
	}
	
	default long getLongMin() {
		return getMin();
	}
	
	default long getLongMax() {
		return getMax();
	}
}
