package endorh.simpleconfig.core;

import endorh.simpleconfig.core.entry.DoubleRangeEntry;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Objects;
import java.util.Random;

public abstract class AbstractRange<
  V extends Comparable<V>, Self extends AbstractRange<V, Self>
  > implements Map.Entry<V, V> {
	private final boolean exclusiveMin;
	private final boolean exclusiveMax;
	private final V min;
	private final V max;
	
	protected AbstractRange(V min, V max, boolean exclusiveMin, boolean exclusiveMax) {
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
	
	public abstract Self create(V min, V max, boolean exclusiveMin, boolean exclusiveMax);
	
	public Self intersect(Self range) {
		V mn = getMin(), rmn = range.getMin(), mx = getMax(), rmx = range.getMax();
		int l = mn.compareTo(range.getMin());
		int r = mx.compareTo(range.getMax());
		return create(
		  l > 0 ? mn : rmn, r < 0 ? mx : rmx,
		  l <= 0 && range.isExclusiveMin() || l >= 0 && isExclusiveMin(),
		  r >= 0 && range.isExclusiveMax() || r <= 0 && isExclusiveMax());
	}
	
	public boolean isExclusiveMin() {
		return exclusiveMin;
	}
	
	public boolean isExclusiveMax() {
		return exclusiveMax;
	}
	
	public V getMin() {
		return min;
	}
	
	public V getMax() {
		return max;
	}
	
	@Override public V getKey() {
		return getMin();
	}
	
	@Override public V getValue() {
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
		
		public double getSize() {
			double size = getDistance(getMin(), getMax());
			if (size == 0 && (isExclusiveMin() || isExclusiveMax())) return -Double.MIN_VALUE;
			return size;
		}
		
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
		public abstract N randomUniform(Random random);
		
		/**
		 * Generate an uniform random value with gaussian distribution centered
		 * on this range, with standard deviation equal to the radius of this
		 * range.<br>
		 * In particular, the returned values can be outside range.<br>
		 * Exclusiveness of the limits is ignored.<br>
		 * If this range is empty, {@code null} is returned.
		 */
		public abstract N randomGaussian(Random random);
		
		public double getCenter() {
			return (getMin().doubleValue() + getMax().doubleValue()) * 0.5D;
		}
		
		public double getRadius() {
			return getSize() * 0.5D;
		}
		
		public abstract Self translate(N translation);
		public abstract Self growRelative(N left, N right);
		public Self growRelative(N both) {
			return growRelative(both, both);
		}
		public abstract Self grow(N left, N right);
		public Self grow(N both) {
			return grow(both, both);
		}
		
		public abstract Self shrinkRelative(N left, N right);
		public Self shrinkRelative(N both) {
			return shrinkRelative(both, both);
		}
		public abstract Self shrink(N left, N right);
		public Self shrink(N both) {
			return shrink(both, both);
		}
		
		@Override protected double getDistance(N left, N right) {
			if (left == null || right == null) return Double.NaN;
			return left.doubleValue() - right.doubleValue();
		}
	}
	
	public static class DoubleRange extends AbstractNumberRange<Double, DoubleRange> {
		public static final DoubleRange EMPTY = new DoubleRange(0D, 0D, true, true);
		public static final DoubleRange FULL = new DoubleRange(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, false, false);
		public static final DoubleRange UNIT = new DoubleRange(0D, 1D, false, false);
		public static final DoubleRange HALF_OPEN_UNIT = new DoubleRange(0D, 1D, false, true);
		public static final DoubleRange OPEN_UNIT = new DoubleRange(0D, 1D, true, true);
		public static final DoubleRange UNITARY = new DoubleRange(-1D, 1D, false, false);
		
		public static DoubleRange inclusive(double min, double max) {
			return new DoubleRange(min, max, false, false);
		}
		
		public static DoubleRange exclusive(double min, double max) {
			return new DoubleRange(min, max, true, true);
		}
		
		public static DoubleRange of(
		  double min, boolean exclusiveMin, double max, boolean exclusiveMax
		) {
			return new DoubleRange(min, max, exclusiveMin, exclusiveMax);
		}
		
		public static DoubleRange minimum(double min) {
			return minimum(min, false);
		}
		
		public static DoubleRange minimum(double min, boolean exclusive) {
			return new DoubleRange(min, Double.POSITIVE_INFINITY, exclusive, false);
		}
		
		public static DoubleRange maximum(double max) {
			return maximum(max, false);
		}
		
		public static DoubleRange maximum(double max, boolean exclusive) {
			return new DoubleRange(Double.NEGATIVE_INFINITY, max, false, exclusive);
		}
		
		public static DoubleRange exactly(double value) {
			return new DoubleRange(value, value, false, false);
		}
		
		protected DoubleRange(Double min, Double max, boolean exclusiveMin, boolean exclusiveMax) {
			super(
			  min != null && !Double.isNaN(min) ? min : Double.NEGATIVE_INFINITY,
			  max != null && !Double.isNaN(max) ? max : Double.POSITIVE_INFINITY,
			  exclusiveMin, exclusiveMax);
		}
		
		/**
		 * Generate an uniform random value within this range.<br>
		 * Infinite bounds are clamped to
		 * [-{@link Double#MAX_VALUE}, {@link Double#MAX_VALUE}].<br>
		 * Exclusiveness of the limits is considered.<br>
		 * Uniformity of the distribution might be distorted by the
		 * density of floating point numbers for big ranges.<br>
		 * IF this range is empty, {@code null} is returned.
		 */
		@Override public Double randomUniform(Random random) {
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
		
		@Override public Double randomGaussian(Random random) {
			double min = Math.max(getMin(), -Double.MAX_VALUE);
			double max = Math.min(getMax(), Double.MAX_VALUE);
			if (max < min) return null;
			return (min + max) * 0.5D + random.nextGaussian() * (max - min) * 0.5D;
		}
		
		@Override public DoubleRange create(
		  Double min, Double max, boolean exclusiveMin, boolean exclusiveMax
		) {
			return new DoubleRange(min, max, exclusiveMin, exclusiveMax);
		}
		
		public DoubleRange translate(double translation) {
			return translate((Double) translation);
		}
		
		@Override public DoubleRange translate(Double translation) {
			return create(getMin() + translation, getMax() + translation, isExclusiveMin(), isExclusiveMax());
		}
		
		public DoubleRange growRelative(double left, double right) {
			return growRelative((Double) left, (Double) right);
		}
		
		public DoubleRange growRelative(double both) {
			return growRelative((Double) both);
		}
		
		@Override public DoubleRange growRelative(Double left, Double right) {
			double size = getSize();
			return create(
			  getMin() - left * size, getMax() + right * size,
			  isExclusiveMin(), isExclusiveMax());
		}
		
		public DoubleRange grow(double left, double right) {
			return grow((Double) left, (Double) right);
		}
		
		public DoubleRange grow(double both) {
			return grow((Double) both);
		}
		
		@Override public DoubleRange grow(Double left, Double right) {
			return create(getMin() - left, getMax() + right, isExclusiveMin(), isExclusiveMax());
		}
		
		public DoubleRange shrinkRelative(double left, double right) {
			return shrinkRelative((Double) left, (Double) right);
		}
		
		public DoubleRange shrinkRelative(double both) {
			return shrinkRelative((Double) both);
		}
		
		@Override public DoubleRange shrinkRelative(Double left, Double right) {
			double size = getSize();
			return create(getMin() + left * size, getMax() - right * size, isExclusiveMin(), isExclusiveMax());
		}
		
		public DoubleRange shrink(double left, double right) {
			return shrink((Double) left, (Double) right);
		}
		
		public DoubleRange shrink(double both) {
			return shrink((Double) both);
		}
		
		@Override public DoubleRange shrink(Double left, Double right) {
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
	
	public static class LongRange extends AbstractNumberRange<Long, LongRange> {
		public static final LongRange EMPTY = new LongRange(0, 0, true, true);
		public static final LongRange FULL = new LongRange(Long.MIN_VALUE, Long.MAX_VALUE, false, false);
		public static final LongRange UNIT = new LongRange(0, 1, false, false);
		
		public static LongRange inclusive(long min, long max) {
			return new LongRange(min, max, false, false);
		}
		
		public static LongRange exclusive(long min, long max) {
			return new LongRange(min, max, true, true);
		}
		
		public static LongRange exactly(long value) {
			return new LongRange(value, value, false, false);
		}
		
		public static LongRange range(long upToExclusive) {
			return new LongRange(0, upToExclusive, false, true);
		}
		
		public static LongRange rangeFrom1(long upToInclusive) {
			return new LongRange(0, upToInclusive, true, false);
		}
		
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
		
		public LongRange translate(long translation) {
			return translate((Long) translation);
		}
		
		@Override public LongRange translate(Long translation) {
			return create(getMin() + translation, getMax() + translation, isExclusiveMin(), isExclusiveMax());
		}
		
		public LongRange growRelative(long left, long right) {
			return growRelative((Long) left, (Long) right);
		}
		
		public LongRange growRelative(long both) {
			return growRelative((Long) both);
		}
		
		@Override public LongRange growRelative(Long left, Long right) {
			double size = getSize();
			return create(
			  (long) Math.floor(getMin() - left * size),
			  (long) Math.ceil(getMax() + right * size),
			  isExclusiveMin(), isExclusiveMax());
		}
		
		public LongRange grow(long left, long right) {
			return grow((Long) left, (Long) right);
		}
		
		public LongRange grow(long both) {
			return grow((Long) both);
		}
		
		@Override public LongRange grow(Long left, Long right) {
			return create(getMin() - left, getMax() + right, isExclusiveMin(), isExclusiveMax());
		}
		
		public LongRange shrinkRelative(long left, long right) {
			return shrinkRelative((Long) left, (Long) right);
		}
		
		public LongRange shrinkRelative(long both) {
			return shrinkRelative((Long) both);
		}
		
		@Override public LongRange shrinkRelative(Long left, Long right) {
			double size = getSize();
			return create(
			  (long) Math.ceil(getMin() + left * size),
			  (long) Math.floor(getMax() - right * size),
			  isExclusiveMin(), isExclusiveMax());
		}
		
		public LongRange shrink(long left, long right) {
			return shrink((Long) left, (Long) right);
		}
		
		public LongRange shrink(long both) {
			return shrink((Long) both);
		}
		
		@Override public LongRange shrink(Long left, Long right) {
			return create(getMin() + left, getMax() - right, isExclusiveMin(), isExclusiveMax());
		}
		
		@Override public LongRange create(
		  Long min, Long max, boolean exclusiveMin, boolean exclusiveMax
		) {
			return new LongRange(min, max, exclusiveMin, exclusiveMax);
		}
		
		@Override protected double getDistance(Long left, Long right) {
			if (left == null || right == null) return Double.NaN;
			return right - left;
		}
	}
	
}
