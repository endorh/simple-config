package endorh.simpleconfig.core;

import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;

import java.util.Random;

/**
 * Immutable pair of {@code double}s, representing a real number range.
 */
public class Range extends Pair<Double, Double> {
	private final double min;
	private final double max;
	
	public static final Range EMPTY = new Range(1, -1);
	public static final Range UNIT = new Range(0, 1);
	public static final Range FULL = new Range(
	  Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY) {
		@Override public double getCenter() {
			return 0.0;
		}
	};
	
	public static Range of(double min, double max) {
		return new Range(min, max);
	}
	
	public static Range of(Double min, Double max) {
		return new Range(
		  min != null ? min : Double.NEGATIVE_INFINITY,
		  max != null ? max : Double.POSITIVE_INFINITY);
	}
	
	public static Range minimum(double min) {
		return of(min, Double.POSITIVE_INFINITY);
	}
	
	public static Range maximum(double max) {
		return of(Double.NEGATIVE_INFINITY, max);
	}
	
	public static Range exactly(double value) {
		return of(value, value);
	}
	
	private Range(double min, double max) {
		this.min = min;
		this.max = max;
	}
	
	public double getMin() {
		return min;
	}
	
	public double getMax() {
		return max;
	}
	
	public boolean contains(double value) {
		return min <= value && value <= max;
	}
	
	public boolean containsStrictly(double value) {
		return min < value && value < max;
	}
	
	/**
	 * Generate an uniform random value within this range.<br>
	 * Extremes are clamped to [-{@link Double#MAX_VALUE}, {@link Double#MAX_VALUE}].
	 */
	public double randomValue(Random random) {
		if (min > max) return Double.NaN;
		double mn = Math.max(min, -Double.MAX_VALUE);
		double mx = Math.min(max, Double.MAX_VALUE);
		return mn + random.nextDouble() * (mx - mn);
	}
	
	/**
	 * Size of the range. Returns negative values for empty ranges.
	 */
	public double getSize() {
		return max - min;
	}
	
	/**
	 * Check if the range contains at least one value.
	 */
	public boolean isEmpty() {
		return min > max;
	}
	
	/**
	 * Get the center of this range.
	 */
	public double getCenter() {
		return (min + max) / 2D;
	}
	
	/**
	 * Get the radius of this range.
	 */
	public double getRadius() {
		return (max - min) / 2D;
	}
	
	/**
	 * Create a new range with the intersection of this and other.
	 */
	public Range intersection(Range other) {
		return of(Math.max(min, other.min), Math.min(max, other.max));
	}
	
	@Override public @NotNull Double getLeft() {
		return getMin();
	}
	
	@Override public @NotNull Double getRight() {
		return getMax();
	}
	
	/**
	 * @return Never
	 * @throws UnsupportedOperationException Always
	 */
	@Override public Double setValue(Double value) {
		throw new UnsupportedOperationException();
	}
}
