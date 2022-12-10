package endorh.simpleconfig.api.range;

import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Random;

@Internal public abstract class AbstractRange<
  V extends Comparable<V>, Self extends Range<V, Self>
> implements Range<V, Self> {
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
	
	@Override public boolean contains(@NotNull V value) {
		int l = getMin().compareTo(value);
		int r = getMax().compareTo(value);
		return (isExclusiveMin() ? l < 0 : l <= 0) && (isExclusiveMax() ? r > 0 : r >= 0);
	}
	
	@Override public @NotNull Self intersect(@NotNull Self range) {
		V mn = getMin(), rmn = range.getMin(), mx = getMax(), rmx = range.getMax();
		int l = mn.compareTo(range.getMin());
		int r = mx.compareTo(range.getMax());
		return create(
		  l > 0 ? mn : rmn, r < 0 ? mx : rmx,
		  l <= 0 && range.isExclusiveMin() || l >= 0 && isExclusiveMin(),
		  r >= 0 && range.isExclusiveMax() || r <= 0 && isExclusiveMax());
	}
	
	@Override public boolean isExclusiveMin() {
		return exclusiveMin;
	}
	
	@Override public boolean isExclusiveMax() {
		return exclusiveMax;
	}
	
	@Override public @NotNull V getMin() {
		return min;
	}
	
	@Override public @NotNull V getMax() {
		return max;
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
	
	@Internal public static abstract class AbstractSizedRange<
	  V extends Comparable<V>, Self extends SizedRange<V, Self>
	> extends AbstractRange<V, Self> implements SizedRange<V, Self> {
		protected AbstractSizedRange(V min, V max, boolean exclusiveMin, boolean exclusiveMax) {
			super(min, max, exclusiveMin, exclusiveMax);
		}
		
		protected abstract double getDistance(V left, V right);
		
		@Override public double getSize() {
			double size = getDistance(getMin(), getMax());
			if (size == 0 && (isExclusiveMin() || isExclusiveMax())) return -Double.MIN_VALUE;
			return size;
		}
		
		@Override public boolean isEmpty() {
			return getSize() < 0;
		}
	}
	
	@Internal public static abstract class AbstractNumberRange<
	  N extends Number & Comparable<N>, Self extends NumberRange<N, Self>
	> extends AbstractSizedRange<N, Self> implements NumberRange<N, Self> {
		protected AbstractNumberRange(N min, N max, boolean exclusiveMin, boolean exclusiveMax) {
			super(min, max, exclusiveMin, exclusiveMax);
		}
		
		@Override public double getCenter() {
			return (getMin().doubleValue() + getMax().doubleValue()) * 0.5D;
		}
		
		@Override public double getRadius() {
			return getSize() * 0.5D;
		}
		
		@Override public @NotNull Self growRelative(@NotNull N both) {
			return growRelative(both, both);
		}
		
		@Override public @NotNull Self grow(@NotNull N both) {
			return grow(both, both);
		}
		
		@Override public @NotNull Self shrinkRelative(@NotNull N both) {
			return shrinkRelative(both, both);
		}
		
		@Override public @NotNull Self shrink(@NotNull N both) {
			return shrink(both, both);
		}
		
		@Override protected double getDistance(N left, N right) {
			if (left == null || right == null) return Double.NaN;
			return left.doubleValue() - right.doubleValue();
		}
	}
	
	@Internal public static class DoubleRangeImpl extends AbstractNumberRange<Double, DoubleRange>
	  implements DoubleRange {
		protected DoubleRangeImpl(Double min, Double max, boolean exclusiveMin, boolean exclusiveMax) {
			super(
			  min != null && !Double.isNaN(min) ? min : Double.NEGATIVE_INFINITY,
			  max != null && !Double.isNaN(max) ? max : Double.POSITIVE_INFINITY,
			  exclusiveMin, exclusiveMax);
		}
		
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
			return new DoubleRangeImpl(min, max, exclusiveMin, exclusiveMax);
		}
		
		@Override public @NotNull DoubleRange translate(@NotNull Double translation) {
			return create(getMin() + translation, getMax() + translation, isExclusiveMin(), isExclusiveMax());
		}
		
		@Override public @NotNull DoubleRange growRelative(
		  @NotNull Double left, @NotNull Double right
		) {
			double size = getSize();
			return create(
			  getMin() - left * size, getMax() + right * size,
			  isExclusiveMin(), isExclusiveMax());
		}
		
		@Override public @NotNull DoubleRange grow(@NotNull Double left, @NotNull Double right) {
			return create(getMin() - left, getMax() + right, isExclusiveMin(), isExclusiveMax());
		}
		
		@Override public @NotNull DoubleRange shrinkRelative(
		  @NotNull Double left, @NotNull Double right
		) {
			double size = getSize();
			return create(getMin() + left * size, getMax() - right * size, isExclusiveMin(), isExclusiveMax());
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
	
	@Internal public static class FloatRangeImpl extends AbstractNumberRange<Float, FloatRange>
	  implements FloatRange {
		protected FloatRangeImpl(Float min, Float max, boolean exclusiveMin, boolean exclusiveMax) {
			super(
			  min != null && !Float.isNaN(min)? min : Float.NEGATIVE_INFINITY,
			  max != null && !Float.isNaN(max)? max : Float.POSITIVE_INFINITY,
			  exclusiveMin, exclusiveMax);
		}
		
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
		
		@Override public @Nullable Float randomGaussian(Random random) {
			float min = Math.max(getMin(), -Float.MAX_VALUE);
			float max = Math.min(getMax(), Float.MAX_VALUE);
			if (max < min) return null;
			return (min + max) * 0.5F + (float) random.nextGaussian() * (max - min) * 0.5F;
		}
		
		@Override public @NotNull FloatRange create(
		  @NotNull Float min, @NotNull Float max, boolean exclusiveMin, boolean exclusiveMax
		) {
			return new FloatRangeImpl(min, max, exclusiveMin, exclusiveMax);
		}
		
		@Override public @NotNull FloatRange translate(@NotNull Float translation) {
			return create(
			  getMin() + translation, getMax() + translation,
			  isExclusiveMin(), isExclusiveMax());
		}
		
		@Override public @NotNull FloatRange growRelative(@NotNull Float left, @NotNull Float right) {
			float size = getFloatSize();
			return create(
			  getMin() - left * size, getMax() + right * size,
			  isExclusiveMin(), isExclusiveMax());
		}
		
		@Override public @NotNull FloatRange grow(@NotNull Float left, @NotNull Float right) {
			return create(getMin() - left, getMax() + right, isExclusiveMin(), isExclusiveMax());
		}
		
		@Override public @NotNull FloatRange shrinkRelative(@NotNull Float left, @NotNull Float right) {
			float size = getFloatSize();
			return create(
			  getMin() + left * size, getMax() - right * size, isExclusiveMin(), isExclusiveMax());
		}
		
		@Override public @NotNull FloatRange shrink(@NotNull Float left, @NotNull Float right) {
			return create(getMin() + left, getMax() - right, isExclusiveMin(), isExclusiveMax());
		}
		
		@Override public double getCenter() {
			return getFloatCenter();
		}
		
		@Override protected double getDistance(Float left, Float right) {
			if (left == null || right == null) return Float.NaN;
			return right - left;
		}
	}
	
	@Internal public static class LongRangeImpl extends AbstractNumberRange<Long, LongRange>
	  implements LongRange {
		protected LongRangeImpl(long min, long max, boolean exclusiveMin, boolean exclusiveMax) {
			this((Long) min, (Long) max, exclusiveMin, exclusiveMax);
		}
		
		protected LongRangeImpl(Long min, Long max, boolean exclusiveMin, boolean exclusiveMax) {
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
		
		@Override public @NotNull LongRange translate(@NotNull Long translation) {
			return create(getMin() + translation, getMax() + translation, isExclusiveMin(), isExclusiveMax());
		}
		
		@Override public @NotNull LongRange growRelative(@NotNull Long left, @NotNull Long right) {
			double size = getSize();
			return create(
			  (long) Math.floor(getMin() - left * size),
			  (long) Math.ceil(getMax() + right * size),
			  isExclusiveMin(), isExclusiveMax());
		}
		
		@Override public @NotNull LongRange grow(@NotNull Long left, @NotNull Long right) {
			return create(getMin() - left, getMax() + right, isExclusiveMin(), isExclusiveMax());
		}
		
		@Override public @NotNull LongRange shrinkRelative(@NotNull Long left, @NotNull Long right) {
			double size = getSize();
			return create(
			  (long) Math.ceil(getMin() + left * size),
			  (long) Math.floor(getMax() - right * size),
			  isExclusiveMin(), isExclusiveMax());
		}
		
		@Override public @NotNull LongRange shrink(@NotNull Long left, @NotNull Long right) {
			return create(getMin() + left, getMax() - right, isExclusiveMin(), isExclusiveMax());
		}
		
		@Override public @NotNull LongRange create(
		  @NotNull Long min, @NotNull Long max, boolean exclusiveMin, boolean exclusiveMax
		) {
			return new LongRangeImpl(min, max, exclusiveMin, exclusiveMax);
		}
		
		@Override protected double getDistance(Long left, Long right) {
			if (left == null || right == null) return Double.NaN;
			return right - left;
		}
	}
	
	@Internal public static class IntRangeImpl extends AbstractNumberRange<Integer, IntRange>
	  implements IntRange {
		protected IntRangeImpl(int min, int max, boolean exclusiveMin, boolean exclusiveMax) {
			this((Integer) min, (Integer) max, exclusiveMin, exclusiveMax);
		}
		
		protected IntRangeImpl(Integer min, Integer max, boolean exclusiveMin, boolean exclusiveMax) {
			super(
			  min != null? min : Integer.MIN_VALUE,
			  max != null? max : Integer.MAX_VALUE,
			  exclusiveMin, exclusiveMax);
		}
		
		@Override public Integer randomUniform(Random random) {
			int min = isExclusiveMin()? getMin() + 1 : getMin(),
			  max = isExclusiveMax()? getMax() - 1 : getMax();
			if (max < min) return null;
			return Math.min((int) Math.floor(min + random.nextFloat() * (max - min + 1)), max);
		}
		
		@Override public Integer randomGaussian(Random random) {
			int min = isExclusiveMin()? getMin() + 1 : getMin(),
			  max = isExclusiveMax()? getMax() - 1 : getMax();
			if (max < min) return null;
			return Math.round((min + max) * 0.5F + (float) random.nextGaussian() * (max - min));
		}
		
		@Override public @NotNull IntRange translate(@NotNull Integer translation) {
			return create(
			  getMin() + translation, getMax() + translation, isExclusiveMin(), isExclusiveMax());
		}
		
		@Override public @NotNull IntRange growRelative(@NotNull Integer left, @NotNull Integer right) {
			double size = getSize();
			return create(
			  (int) Math.floor(getMin() - left * size),
			  (int) Math.ceil(getMax() + right * size),
			  isExclusiveMin(), isExclusiveMax());
		}
		
		@Override public @NotNull IntRange grow(@NotNull Integer left, @NotNull Integer right) {
			return create(getMin() - left, getMax() + right, isExclusiveMin(), isExclusiveMax());
		}
		
		@Override public @NotNull IntRange shrinkRelative(
		  @NotNull Integer left, @NotNull Integer right
		) {
			double size = getSize();
			return create(
			  (int) Math.ceil(getMin() + left * size),
			  (int) Math.floor(getMax() - right * size),
			  isExclusiveMin(), isExclusiveMax());
		}
		
		@Override public @NotNull IntRange shrink(@NotNull Integer left, @NotNull Integer right) {
			return create(getMin() + left, getMax() - right, isExclusiveMin(), isExclusiveMax());
		}
		
		@Override public @NotNull IntRange create(
		  @NotNull Integer min, @NotNull Integer max, boolean exclusiveMin, boolean exclusiveMax
		) {
			return new IntRangeImpl(min, max, exclusiveMin, exclusiveMax);
		}
		
		@Override protected double getDistance(Integer left, Integer right) {
			if (left == null || right == null) return Double.NaN;
			return right - left;
		}
	}
}
