package endorh.simpleconfig.api;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Objects;
import java.util.Random;

public abstract class AbstractRange<
  V extends Comparable<V>, Self extends AbstractRange<V, Self>
> implements Map.Entry<V, V> {
	private final boolean exclusiveMin;
	private final boolean exclusiveMax;
	private final @NotNull V min;
	private final @NotNull V max;
	
	protected AbstractRange(@NotNull V min, @NotNull V max, boolean exclusiveMin, boolean exclusiveMax) {
		this.min = min;
		this.max = max;
		this.exclusiveMin = exclusiveMin;
		this.exclusiveMax = exclusiveMax;
	}
	
	public boolean contains(@NotNull V value) {
		int l = getMin().compareTo(value);
		int r = getMax().compareTo(value);
		return (isExclusiveMin() ? l < 0 : l <= 0) && (isExclusiveMax() ? r > 0 : r >= 0);
	}
	
	public abstract @NotNull Self create(@NotNull V min, @NotNull V max, boolean exclusiveMin, boolean exclusiveMax);
	
	public @NotNull Self intersect(@NotNull Self range) {
		V mn = getMin(), rmn = range.getMin(), mx = getMax(), rmx = range.getMax();
		int l = mn.compareTo(range.getMin());
		int r = mx.compareTo(range.getMax());
		return create(
		  l > 0 ? mn : rmn, r < 0 ? mx : rmx,
		  l <= 0 && range.isExclusiveMin() || l >= 0 && isExclusiveMin(),
		  r >= 0 && range.isExclusiveMax() || r <= 0 && isExclusiveMax());
	}
	
	/**
	 * Whether the min value is exclusive (not included in the range)
	 */
	public boolean isExclusiveMin() {
		return exclusiveMin;
	}
	
	/**
	 * Whether the max value is exclusive (not included in the range)
	 */
	public boolean isExclusiveMax() {
		return exclusiveMax;
	}
	
	public @NotNull V getMin() {
		return min;
	}
	
	public @NotNull V getMax() {
		return max;
	}
	
	@Override public @NotNull V getKey() {
		return getMin();
	}
	@Override public @NotNull V getValue() {
		return getMax();
	}
	
	/**
	 * @return never
	 * @throws UnsupportedOperationException always
	 * @deprecated Unsupported
	 */
	@Deprecated @Override public V setValue(V value) {
		throw new UnsupportedOperationException("Immutable pair cannot be modified");
	}
	
	@Override public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		AbstractRange<?, ?> that = (AbstractRange<?, ?>) o;
		return exclusiveMin == that.exclusiveMin
		       && exclusiveMax == that.exclusiveMax
		       && min.equals(that.min) && max.equals(that.max);
	}
	
	@Override public int hashCode() {
		return Objects.hash(exclusiveMin, exclusiveMax, min, max);
	}
	
	public static abstract class AbstractSizedRange<
	  V extends Comparable<V>, Self extends AbstractSizedRange<V, Self>
	> extends AbstractRange<V, Self> {
		protected AbstractSizedRange(V min, V max, boolean exclusiveMin, boolean exclusiveMax) {
			super(min, max, exclusiveMin, exclusiveMax);
		}
		
		protected abstract double getDistance(V left, V right);
		
		/**
		 * Get the size of this range, that is, the distance between
		 * its min and max values.<br>
		 * If the max value is smaller than the min, the size will
		 * be negative.<br>
		 * If the range is empty (size 0 with at least one exclusive bound),
		 * the size will be negative ({@code -Double.MIN_VALUE}).
		 */
		public double getSize() {
			double size = getDistance(getMin(), getMax());
			if (size == 0 && (isExclusiveMin() || isExclusiveMax())) return -Double.MIN_VALUE;
			return size;
		}
		
		/**
		 * Check if this range is empty.<br>
		 * A range is determined to be empty when its size is
		 * 0 and at least one bound is exclusive, or when
		 * the max value is smaller than the min value.
		 */
		public boolean isEmpty() {
			return getSize() < 0;
		}
	}
	
	public static abstract class AbstractNumberRange<
	  N extends Number & Comparable<N>, Self extends AbstractNumberRange<N, Self>
	> extends AbstractSizedRange<N, Self> {
		protected AbstractNumberRange(N min, N max, boolean exclusiveMin, boolean exclusiveMax) {
			super(min, max, exclusiveMin, exclusiveMax);
		}
		
		/**
		 * Generate an uniform random value within this range.<br>
		 * Exclusiveness of the limits is considered.<br>
		 * If this range is empty, {@code null} is returned.
		 */
		public abstract @Nullable N randomUniform(Random random);
		
		/**
		 * Generate an uniform random value with gaussian distribution centered
		 * on this range, with standard deviation equal to the radius of this
		 * range.<br>
		 * In particular, the returned values can be outside range.<br>
		 * Exclusiveness of the limits is ignored.<br>
		 * If this range is empty, {@code null} is returned.
		 */
		public abstract @Nullable N randomGaussian(Random random);
		
		/**
		 * Get the center of this range, that is, the point at the same
		 * distance of the bounds.<br>
		 */
		public double getCenter() {
			return (getMin().doubleValue() + getMax().doubleValue()) * 0.5D;
		}
		
		/**
		 * Get the radius of this range, that is, the distance from
		 * the center to the bounds.
		 */
		public double getRadius() {
			return getSize() * 0.5D;
		}
		
		/**
		 * Translate the range by a given offset.
		 */
		public abstract @NotNull Self translate(@NotNull N translation);
		
		/**
		 * Grow the range by a given factor in each direction.
		 */
		public abstract @NotNull Self growRelative(@NotNull N left, @NotNull N right);

		/**
		 * Grow the range by a given factor in both directions.
		 */
		public @NotNull Self growRelative(@NotNull N both) {
			return growRelative(both, both);
		}
		
		/**
		 * Grow the range by a given amount in each direction.
		 */
		public abstract @NotNull Self grow(@NotNull N left, @NotNull N right);
		
		/**
		 * Grow the range by a given amount in both directions.
		 */
		public @NotNull Self grow(@NotNull N both) {
			return grow(both, both);
		}
		
		/**
		 * Shrink the range by a given factor in each direction.
		 */
		public abstract @NotNull Self shrinkRelative(@NotNull N left, @NotNull N right);
		
		/**
		 * Shrink the range by a given factor in both directions.
		 */
		public @NotNull Self shrinkRelative(@NotNull N both) {
			return shrinkRelative(both, both);
		}
		
		/**
		 * Shrink the range by a given amount in each direction.
		 */
		public abstract @NotNull Self shrink(@NotNull N left, @NotNull N right);
		
		/**
		 * Shrink the range by a given amount in both directions.
		 */
		public @NotNull Self shrink(@NotNull N both) {
			return shrink(both, both);
		}
		
		@Override protected double getDistance(N left, N right) {
			if (left == null || right == null) return Double.NaN;
			return left.doubleValue() - right.doubleValue();
		}
	}
	
	/**
	 * A range of {@code double} values.
	 */
	public static class DoubleRange extends AbstractNumberRange<Double, DoubleRange> {
		/**
		 * Empty range: (0, 0).
		 */
		public static final DoubleRange EMPTY = new DoubleRange(0D, 0D, true, true);
		/**
		 * Full range: [-∞, ∞].
		 */
		public static final DoubleRange FULL = new DoubleRange(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, false, false);
		/**
		 * Closed unit range: [0, 1].
		 */
		public static final DoubleRange UNIT = new DoubleRange(0D, 1D, false, false);
		/**
		 * Half open unit range: [0, 1).
		 */
		public static final DoubleRange HALF_OPEN_UNIT = new DoubleRange(0D, 1D, false, true);
		/**
		 * Open unit range: (0, 1).
		 */
		public static final DoubleRange OPEN_UNIT = new DoubleRange(0D, 1D, true, true);
		/**
		 * Closed unitary range: [-1, 1].
		 */
		public static final DoubleRange UNITARY = new DoubleRange(-1D, 1D, false, false);
		
		/**
		 * Create an inclusive range between min and max.
		 */
		public static @NotNull DoubleRange inclusive(double min, double max) {
			return new DoubleRange(min, max, false, false);
		}
		
		/**
		 * Create an exclusive range between min and max.
		 */
		public static @NotNull DoubleRange exclusive(double min, double max) {
			return new DoubleRange(min, max, true, true);
		}
		
		/**
		 * Create a range between min and max, with the given exclusiveness.
		 */
		public static @NotNull DoubleRange of(
		  double min, boolean exclusiveMin, double max, boolean exclusiveMax
		) {
			return new DoubleRange(min, max, exclusiveMin, exclusiveMax);
		}
		
		/**
		 * Create an inclusive range without maximum bound.
		 */
		public static @NotNull DoubleRange minimum(double min) {
			return minimum(min, false);
		}
		
		/**
		 * Create a range without maximum bound, with the given exclusiveness.
		 */
		public static @NotNull DoubleRange minimum(double min, boolean exclusive) {
			return new DoubleRange(min, Double.POSITIVE_INFINITY, exclusive, false);
		}
		
		/**
		 * Create an inclusive range without minimum bound.
		 */
		public static @NotNull DoubleRange maximum(double max) {
			return maximum(max, false);
		}
		
		/**
		 * Create a range without minimum bound, with the given exclusiveness.
		 */
		public static @NotNull DoubleRange maximum(double max, boolean exclusive) {
			return new DoubleRange(Double.NEGATIVE_INFINITY, max, false, exclusive);
		}
		
		/**
		 * Create an inclusive range with a single value.
		 */
		public static @NotNull DoubleRange exactly(double value) {
			return new DoubleRange(value, value, false, false);
		}
		
		protected DoubleRange(Double min, Double max, boolean exclusiveMin, boolean exclusiveMax) {
			super(
			  min != null && !Double.isNaN(min) ? min : Double.NEGATIVE_INFINITY,
			  max != null && !Double.isNaN(max) ? max : Double.POSITIVE_INFINITY,
			  exclusiveMin, exclusiveMax);
		}
		
		public double getDoubleMin() {
			return getMin();
		}
		
		public double getDoubleMax() {
			return getMax();
		}
		
		/**
		 * Generate an uniform random value within this range.<br>
		 * Infinite bounds are clamped to
		 * [-{@link Double#MAX_VALUE}, {@link Double#MAX_VALUE}].<br>
		 * Exclusiveness of the limits is considered.<br>
		 * Uniformity of the distribution might be distorted by the
		 * density of floating point numbers for big ranges.<br>
		 * If this range is empty, {@code null} is returned.
		 */
		@Override public @Nullable Double randomUniform(Random random) {
			double min = Math.max(getMin(), -Double.MAX_VALUE);
			double max = Math.min(getMax(), Double.MAX_VALUE);
			if (max > min || max == min && isExclusiveMin() || isExclusiveMax()) return null;
			double v = min + random.nextDouble() * (max - min);
			if (v == min) // Normalize limits
				return isExclusiveMin()
				       ? isExclusiveMax() ? (max + min) * 0.5D : max
				       : isExclusiveMax() ? min : random.nextBoolean() ? min : max;
			return v;
		}
		
		@Override public @Nullable Double randomGaussian(Random random) {
			double min = Math.max(getMin(), -Double.MAX_VALUE);
			double max = Math.min(getMax(), Double.MAX_VALUE);
			if (max < min) return null;
			return (min + max) * 0.5D + random.nextGaussian() * (max - min) * 0.5D;
		}
		
		@Override public @NotNull DoubleRange create(
		  @NotNull Double min, @NotNull Double max, boolean exclusiveMin, boolean exclusiveMax
		) {
			return new DoubleRange(min, max, exclusiveMin, exclusiveMax);
		}
		
		/**
		 * Translate by the given offset.
		 */
		public @NotNull DoubleRange translate(double translation) {
			return translate((Double) translation);
		}
		@Override public @NotNull DoubleRange translate(@NotNull Double translation) {
			return create(getMin() + translation, getMax() + translation, isExclusiveMin(), isExclusiveMax());
		}
		
		/**
		 * Grow by the given factor in each direction.
		 */
		public @NotNull DoubleRange growRelative(double left, double right) {
			return growRelative((Double) left, (Double) right);
		}
		
		/**
		 * Grow by the given factor in both directions.
		 */
		public @NotNull DoubleRange growRelative(double both) {
			return growRelative((Double) both);
		}
		@Override public @NotNull DoubleRange growRelative(@NotNull Double left, @NotNull Double right) {
			double size = getSize();
			return create(
			  getMin() - left * size, getMax() + right * size,
			  isExclusiveMin(), isExclusiveMax());
		}
		
		/**
		 * Grow by the given amount in each direction.
		 */
		public @NotNull DoubleRange grow(double left, double right) {
			return grow((Double) left, (Double) right);
		}
		
		/**
		 * Grow by the given amount in both directions.
		 */
		public @NotNull DoubleRange grow(double both) {
			return grow((Double) both);
		}
		@Override public @NotNull DoubleRange grow(@NotNull Double left, @NotNull Double right) {
			return create(getMin() - left, getMax() + right, isExclusiveMin(), isExclusiveMax());
		}
		
		/**
		 * Shrink by the given factor in each direction.
		 */
		public @NotNull DoubleRange shrinkRelative(double left, double right) {
			return shrinkRelative((Double) left, (Double) right);
		}
		
		/**
		 * Shrink by the given factor in both directions.
		 */
		public @NotNull DoubleRange shrinkRelative(double both) {
			return shrinkRelative((Double) both);
		}
		@Override public @NotNull DoubleRange shrinkRelative(@NotNull Double left, @NotNull Double right) {
			double size = getSize();
			return create(getMin() + left * size, getMax() - right * size, isExclusiveMin(), isExclusiveMax());
		}
		
		/**
		 * Shrink by the given amount in each direction.
		 */
		public @NotNull DoubleRange shrink(double left, double right) {
			return shrink((Double) left, (Double) right);
		}
		
		/**
		 * Shrink by the given amount in both directions.
		 */
		public @NotNull DoubleRange shrink(double both) {
			return shrink((Double) both);
		}
		@Override public @NotNull DoubleRange shrink(@NotNull Double left, @NotNull Double right) {
			return create(getMin() + left, getMax() - right, isExclusiveMin(), isExclusiveMax());
		}
		
		@Override public double getCenter() {
			double min = Math.max(getMin(), -Double.MAX_VALUE);
			double max = Math.min(getMax(), Double.MAX_VALUE);
			return (min + max) / 2D;
		}
		
		@Override protected double getDistance(Double left, Double right) {
			if (left == null || right == null) return Double.NaN;
			return right - left;
		}
	}
	
	/**
	 * A range of {@code float} values.
	 */
	public static class FloatRange extends AbstractNumberRange<Float, FloatRange> {
		/**
		 * Empty range: (0, 0).
		 */
		public static final FloatRange EMPTY = new FloatRange(0F, 0F, true, true);
		/**
		 * Full range: [-∞, ∞].
		 */
		public static final FloatRange FULL = new FloatRange(Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY, false, false);
		/**
		 * Closed unit range: [0, 1].
		 */
		public static final FloatRange UNIT = new FloatRange(0F, 1F, false, false);
		/**
		 * Half open unit range: [0, 1).
		 */
		public static final FloatRange HALF_OPEN_UNIT = new FloatRange(0F, 1F, false, true);
		/**
		 * Open unit range: (0, 1).
		 */
		public static final FloatRange OPEN_UNIT = new FloatRange(0F, 1F, true, true);
		/**
		 * Closed unitary range: [-1, 1].
		 */
		public static final FloatRange UNITARY = new FloatRange(-1F, 1F, false, false);
		
		/**
		 * Create an inclusive range between min and max.
		 */
		public static @NotNull FloatRange inclusive(float min, float max) {
			return new FloatRange(min, max, false, false);
		}
		
		/**
		 * Create an exclusive range between min and max.
		 */
		public static @NotNull FloatRange exclusive(float min, float max) {
			return new FloatRange(min, max, true, true);
		}
		
		/**
		 * Create a range between min and max with the given exclusivity.
		 */
		public static @NotNull FloatRange of(
		  float min, boolean exclusiveMin, float max, boolean exclusiveMax
		) {
			return new FloatRange(min, max, exclusiveMin, exclusiveMax);
		}
		
		/**
		 * Create a closed range with unbound max.
		 */
		public static @NotNull FloatRange minimum(float min) {
			return minimum(min, false);
		}
		
		/**
		 * Create a range with unbound max and the given exclusivity.
		 */
		public static @NotNull FloatRange minimum(float min, boolean exclusive) {
			return new FloatRange(min, Float.POSITIVE_INFINITY, exclusive, false);
		}
		
		/**
		 * Create a closed range with unbound min.
		 */
		public static @NotNull FloatRange maximum(float max) {
			return maximum(max, false);
		}
		
		/**
		 * Create a range with unbound min and the given exclusivity.
		 */
		public static @NotNull FloatRange maximum(float max, boolean exclusive) {
			return new FloatRange(Float.NEGATIVE_INFINITY, max, false, exclusive);
		}
		
		/**
		 * Create a range with exactly one value.
		 */
		public static @NotNull FloatRange exactly(float value) {
			return new FloatRange(value, value, false, false);
		}
		
		protected FloatRange(Float min, Float max, boolean exclusiveMin, boolean exclusiveMax) {
			super(
			  min != null && !Float.isNaN(min)? min : Float.NEGATIVE_INFINITY,
			  max != null && !Float.isNaN(max)? max : Float.POSITIVE_INFINITY,
			  exclusiveMin, exclusiveMax);
		}
		
		public float getFloatMin() {
			return getMin();
		}
		
		public float getFloatMax() {
			return getMax();
		}
		
		/**
		 * Generate an uniform random value within this range.<br>
		 * Infinite bounds are clamped to
		 * [-{@link Float#MAX_VALUE}, {@link Float#MAX_VALUE}].<br>
		 * Exclusiveness of the limits is considered.<br>
		 * Uniformity of the distribution might be distorted by the
		 * density of floating point numbers for big ranges.<br>
		 * IF this range is empty, {@code null} is returned.
		 */
		@Override public @Nullable Float randomUniform(Random random) {
			float min = Math.max(getMin(), -Float.MAX_VALUE);
			float max = Math.min(getMax(), Float.MAX_VALUE);
			if (max > min || max == min && isExclusiveMin() || isExclusiveMax()) return null;
			float v = min + random.nextFloat() * (max - min);
			if (v == min) // Normalize limits
				return isExclusiveMin()
				       ? isExclusiveMax()? (max + min) * 0.5F : max
				       : isExclusiveMax()? min : random.nextBoolean()? min : max;
			return v;
		}
		
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
		@Override public @Nullable Float randomGaussian(Random random) {
			float min = Math.max(getMin(), -Float.MAX_VALUE);
			float max = Math.min(getMax(), Float.MAX_VALUE);
			if (max < min) return null;
			return (min + max) * 0.5F + (float) random.nextGaussian() * (max - min) * 0.5F;
		}
		
		@Override public @NotNull FloatRange create(
		  @NotNull Float min, @NotNull Float max, boolean exclusiveMin, boolean exclusiveMax
		) {
			return new FloatRange(min, max, exclusiveMin, exclusiveMax);
		}
		
		public float getFloatSize() {
			return (float) getSize();
		}
		
		/**
		 * Translate by the given amount.
		 */
		public @NotNull FloatRange translate(float translation) {
			return translate((Float) translation);
		}
		@Override public @NotNull FloatRange translate(@NotNull Float translation) {
			return create(
			  getMin() + translation, getMax() + translation,
			  isExclusiveMin(), isExclusiveMax());
		}
		
		/**
		 * Grow by the given factor in each direction.
		 */
		public @NotNull FloatRange growRelative(float left, float right) {
			return growRelative((Float) left, (Float) right);
		}
		
		/**
		 * Grow by the given factor in both directions.
		 */
		public @NotNull FloatRange growRelative(float both) {
			return growRelative((Float) both);
		}
		
		@Override public @NotNull FloatRange growRelative(@NotNull Float left, @NotNull Float right) {
			float size = getFloatSize();
			return create(
			  getMin() - left * size, getMax() + right * size,
			  isExclusiveMin(), isExclusiveMax());
		}
		
		/**
		 * Grow by the given amount in each direction.
		 */
		public @NotNull FloatRange grow(float left, float right) {
			return grow((Float) left, (Float) right);
		}
		
		/**
		 * Grow by the given amount in both directions.
		 */
		public @NotNull FloatRange grow(float both) {
			return grow((Float) both);
		}
		
		@Override public @NotNull FloatRange grow(@NotNull Float left, @NotNull Float right) {
			return create(getMin() - left, getMax() + right, isExclusiveMin(), isExclusiveMax());
		}
		
		/**
		 * Shrink by the given factor in each direction.
		 */
		public @NotNull FloatRange shrinkRelative(float left, float right) {
			return shrinkRelative((Float) left, (Float) right);
		}
		
		/**
		 * Shrink by the given factor in both directions.
		 */
		public @NotNull FloatRange shrinkRelative(float both) {
			return shrinkRelative((Float) both);
		}
		
		@Override public @NotNull FloatRange shrinkRelative(@NotNull Float left, @NotNull Float right) {
			float size = getFloatSize();
			return create(
			  getMin() + left * size, getMax() - right * size, isExclusiveMin(), isExclusiveMax());
		}
		
		/**
		 * Shrink by the given amount in each direction.
		 */
		public @NotNull FloatRange shrink(float left, float right) {
			return shrink((Float) left, (Float) right);
		}
		
		/**
		 * Shrink by the given amount in both directions.
		 */
		public @NotNull FloatRange shrink(float both) {
			return shrink((Float) both);
		}
		
		@Override public @NotNull FloatRange shrink(@NotNull Float left, @NotNull Float right) {
			return create(getMin() + left, getMax() - right, isExclusiveMin(), isExclusiveMax());
		}
		
		public float getFloatCenter() {
			float min = Math.max(getMin(), -Float.MAX_VALUE);
			float max = Math.min(getMax(), Float.MAX_VALUE);
			return (min + max) / 2F;
		}
		
		@Override public double getCenter() {
			return getFloatCenter();
		}
		
		@Override protected double getDistance(Float left, Float right) {
			if (left == null || right == null) return Float.NaN;
			return right - left;
		}
	}
	
	/**
	 * A range of {@code long} values
	 */
	public static class LongRange extends AbstractNumberRange<Long, LongRange> {
		/**
		 * Empty range: (0, 0).
		 */
		public static final LongRange EMPTY = new LongRange(0, 0, true, true);
		/**
		 * Full range: [Long.MIN_VALUE, Long.MAX_VALUE].
		 */
		public static final LongRange FULL = new LongRange(Long.MIN_VALUE, Long.MAX_VALUE, false, false);
		/**
		 * Closed unit range: [0, 1]
		 */
		public static final LongRange UNIT = new LongRange(0, 1, false, false);
		
		/**
		 * Create a closed range between min and max (inclusive).
		 */
		public static @NotNull LongRange inclusive(long min, long max) {
			return new LongRange(min, max, false, false);
		}
		
		/**
		 * Create an open range between min and max (exclusive).
		 */
		public static @NotNull LongRange exclusive(long min, long max) {
			return new LongRange(min, max, true, true);
		}
		
		/**
		 * Create a range with a single value.
		 */
		public static @NotNull LongRange exactly(long value) {
			return new LongRange(value, value, false, false);
		}
		
		/**
		 * Create a range from 0 (inclusive) to the given value (exclusive).
		 */
		public static @NotNull LongRange range(long upToExclusive) {
			return new LongRange(0, upToExclusive, false, true);
		}
		
		/**
		 * Create a range from 0 (exclusive) to the given value (inclusive).
		 */
		public static @NotNull LongRange rangeFrom1(long upToInclusive) {
			return new LongRange(0, upToInclusive, true, false);
		}
		
		/**
		 * Create a range with the given bounds and exclusivity.
		 */
		protected LongRange(long min, long max, boolean exclusiveMin, boolean exclusiveMax) {
			this((Long) min, (Long) max, exclusiveMin, exclusiveMax);
		}
		
		protected LongRange(Long min, Long max, boolean exclusiveMin, boolean exclusiveMax) {
			super(
			  min != null ? min : Long.MIN_VALUE,
			  max != null ? max : Long.MAX_VALUE,
			  exclusiveMin, exclusiveMax);
		}
		
		@Override public Long randomUniform(Random random) {
			long min = isExclusiveMin() ? getMin() + 1 : getMin(),
			  max = isExclusiveMax() ? getMax() - 1 : getMax();
			if (max < min) return null;
			return Math.min((long) Math.floor(min + random.nextDouble() * (max - min + 1L)), max);
		}
		
		@Override public Long randomGaussian(Random random) {
			long min = isExclusiveMin() ? getMin() + 1 : getMin(),
			  max = isExclusiveMax() ? getMax() - 1 : getMax();
			if (max < min) return null;
			return Math.round((min + max) * 0.5D + random.nextGaussian() * (max - min));
		}
		
		/**
		 * Translate by the given amount.
		 */
		public @NotNull LongRange translate(long translation) {
			return translate((Long) translation);
		}
		@Override public @NotNull LongRange translate(@NotNull Long translation) {
			return create(getMin() + translation, getMax() + translation, isExclusiveMin(), isExclusiveMax());
		}
		
		/**
		 * Grow by the given factor in each direction.
		 */
		public @NotNull LongRange growRelative(long left, long right) {
			return growRelative((Long) left, (Long) right);
		}
		
		/**
		 * Grow by the given factor in both directions.
		 */
		public @NotNull LongRange growRelative(long both) {
			return growRelative((Long) both);
		}
		@Override public @NotNull LongRange growRelative(@NotNull Long left, @NotNull Long right) {
			double size = getSize();
			return create(
			  (long) Math.floor(getMin() - left * size),
			  (long) Math.ceil(getMax() + right * size),
			  isExclusiveMin(), isExclusiveMax());
		}
		
		/**
		 * Grow by the given amount in each direction.
		 */
		public @NotNull LongRange grow(long left, long right) {
			return grow((Long) left, (Long) right);
		}
		
		/**
		 * Grow by the given amount in both directions.
		 */
		public @NotNull LongRange grow(long both) {
			return grow((Long) both);
		}
		@Override public @NotNull LongRange grow(@NotNull Long left, @NotNull Long right) {
			return create(getMin() - left, getMax() + right, isExclusiveMin(), isExclusiveMax());
		}
		
		/**
		 * Shrink by the given factor in each direction.
		 */
		public @NotNull LongRange shrinkRelative(long left, long right) {
			return shrinkRelative((Long) left, (Long) right);
		}
		
		/**
		 * Shrink by the given factor in both directions.
		 */
		public @NotNull LongRange shrinkRelative(long both) {
			return shrinkRelative((Long) both);
		}
		@Override public @NotNull LongRange shrinkRelative(@NotNull Long left, @NotNull Long right) {
			double size = getSize();
			return create(
			  (long) Math.ceil(getMin() + left * size),
			  (long) Math.floor(getMax() - right * size),
			  isExclusiveMin(), isExclusiveMax());
		}
		
		/**
		 * Shrink by the given amount in each direction.
		 */
		public @NotNull LongRange shrink(long left, long right) {
			return shrink((Long) left, (Long) right);
		}
		
		/**
		 * Shrink by the given amount in both directions.
		 */
		public @NotNull LongRange shrink(long both) {
			return shrink((Long) both);
		}
		@Override public @NotNull LongRange shrink(@NotNull Long left, @NotNull Long right) {
			return create(getMin() + left, getMax() - right, isExclusiveMin(), isExclusiveMax());
		}
		
		@Override public @NotNull LongRange create(
		  @NotNull Long min, @NotNull Long max, boolean exclusiveMin, boolean exclusiveMax
		) {
			return new LongRange(min, max, exclusiveMin, exclusiveMax);
		}
		
		public long getLongSize() {
			return (long) getSize();
		}
		
		public long getLongCenter() {
			return (long) getCenter();
		}
		
		public long getLongMin() {
			return getMin();
		}
		
		public long getLongMax() {
			return getMax();
		}
		
		@Override protected double getDistance(Long left, Long right) {
			if (left == null || right == null) return Double.NaN;
			return right - left;
		}
	}
	
	/**
	 * A range of {@code int} values.
	 */
	public static class IntRange extends AbstractNumberRange<Integer, IntRange> {
		/**
		 * Empty range: (0, 0).
		 */
		public static final IntRange EMPTY = new IntRange(0, 0, true, true);
		/**
		 * Full range: [Integer.MIN_VALUE, Integer.MAX_VALUE].
		 */
		public static final IntRange FULL = new IntRange(Integer.MIN_VALUE, Integer.MAX_VALUE, false, false);
		/**
		 * Closed unit range: [0, 1].
		 */
		public static final IntRange UNIT = new IntRange(0, 1, false, false);
		
		/**
		 * Create a closed range with the given bounds.
		 */
		public static @NotNull IntRange inclusive(int min, int max) {
			return new IntRange(min, max, false, false);
		}
		
		/**
		 * Create an open range with the given bounds.
		 */
		public static @NotNull IntRange exclusive(int min, int max) {
			return new IntRange(min, max, true, true);
		}
		
		/**
		 * Create a range with exactly one value.
		 */
		public static @NotNull IntRange exactly(int value) {
			return new IntRange(value, value, false, false);
		}
		
		/**
		 * Create a range from 0 (inclusive) to the given value (exclusive).
		 */
		public static @NotNull IntRange range(int upToExclusive) {
			return new IntRange(0, upToExclusive, false, true);
		}
		
		/**
		 * Create a range from 0 (exclusive) to the given value (inclusive).
		 */
		public static @NotNull IntRange rangeFrom1(int upToInclusive) {
			return new IntRange(0, upToInclusive, true, false);
		}
		
		protected IntRange(int min, int max, boolean exclusiveMin, boolean exclusiveMax) {
			this((Integer) min, (Integer) max, exclusiveMin, exclusiveMax);
		}
		
		protected IntRange(Integer min, Integer max, boolean exclusiveMin, boolean exclusiveMax) {
			super(
			  min != null? min : Integer.MIN_VALUE,
			  max != null? max : Integer.MAX_VALUE,
			  exclusiveMin, exclusiveMax);
		}
		
		@Override public @NotNull Integer randomUniform(Random random) {
			int min = isExclusiveMin()? getMin() + 1 : getMin(),
			  max = isExclusiveMax()? getMax() - 1 : getMax();
			if (max < min) return null;
			return Math.min((int) Math.floor(min + random.nextFloat() * (max - min + 1)), max);
		}
		
		@Override public @NotNull Integer randomGaussian(Random random) {
			int min = isExclusiveMin()? getMin() + 1 : getMin(),
			  max = isExclusiveMax()? getMax() - 1 : getMax();
			if (max < min) return null;
			return Math.round((min + max) * 0.5F + (float) random.nextGaussian() * (max - min));
		}
		
		/**
		 * Translate by the given amount.
		 */
		public @NotNull IntRange translate(int translation) {
			return translate((Integer) translation);
		}
		@Override public @NotNull IntRange translate(@NotNull Integer translation) {
			return create(
			  getMin() + translation, getMax() + translation, isExclusiveMin(), isExclusiveMax());
		}
		
		/**
		 * Grow by the given factor in each direction.
		 */
		public @NotNull IntRange growRelative(int left, int right) {
			return growRelative((Integer) left, (Integer) right);
		}
		
		/**
		 * Grow by the given factor in both directions.
		 */
		public @NotNull IntRange growRelative(int both) {
			return growRelative((Integer) both);
		}
		@Override public @NotNull IntRange growRelative(@NotNull Integer left, @NotNull Integer right) {
			double size = getSize();
			return create(
			  (int) Math.floor(getMin() - left * size),
			  (int) Math.ceil(getMax() + right * size),
			  isExclusiveMin(), isExclusiveMax());
		}
		
		/**
		 * Grow by the given amount in each direction.
		 */
		public @NotNull IntRange grow(int left, int right) {
			return grow((Integer) left, (Integer) right);
		}
		
		/**
		 * Grow by the given amount in both directions.
		 */
		public @NotNull IntRange grow(int both) {
			return grow((Integer) both);
		}
		@Override public @NotNull IntRange grow(@NotNull Integer left, @NotNull Integer right) {
			return create(getMin() - left, getMax() + right, isExclusiveMin(), isExclusiveMax());
		}
		
		/**
		 * Shrink by the given factor in each direction.
		 */
		public @NotNull IntRange shrinkRelative(int left, int right) {
			return shrinkRelative((Integer) left, (Integer) right);
		}
		
		/**
		 * Shrink by the given factor in both directions.
		 */
		public @NotNull IntRange shrinkRelative(int both) {
			return shrinkRelative((Integer) both);
		}
		@Override public @NotNull IntRange shrinkRelative(@NotNull Integer left, @NotNull Integer right) {
			double size = getSize();
			return create(
			  (int) Math.ceil(getMin() + left * size),
			  (int) Math.floor(getMax() - right * size),
			  isExclusiveMin(), isExclusiveMax());
		}
		
		/**
		 * Shrink by the given amount in each direction.
		 */
		public @NotNull IntRange shrink(int left, int right) {
			return shrink((Integer) left, (Integer) right);
		}
		
		/**
		 * Shrink by the given amount in both directions.
		 */
		public @NotNull IntRange shrink(int both) {
			return shrink((Integer) both);
		}
		@Override public @NotNull IntRange shrink(@NotNull Integer left, @NotNull Integer right) {
			return create(getMin() + left, getMax() - right, isExclusiveMin(), isExclusiveMax());
		}
		
		@Override public @NotNull IntRange create(
		  @NotNull Integer min, @NotNull Integer max, boolean exclusiveMin, boolean exclusiveMax
		) {
			return new IntRange(min, max, exclusiveMin, exclusiveMax);
		}
		
		public int getIntSize() {
			return (int) getSize();
		}
		
		public int getIntCenter() {
			return (int) getCenter();
		}
		
		public int getIntMin() {
			return getMin();
		}
		
		public int getIntMax() {
			return getMax();
		}
		
		@Override protected double getDistance(Integer left, Integer right) {
			if (left == null || right == null) return Double.NaN;
			return right - left;
		}
	}
}
