package endorh.simpleconfig.api.range;

import java.util.Random;

public interface NumberRange<N extends Number & Comparable<N>, Self extends NumberRange<N, Self>>
  extends SizedRange<N, Self> {
	/**
	 * Generate a uniform random value within this range.<br>
	 * Exclusiveness of the limits is considered.<br>
	 * If this range is empty, {@code null} is returned.
	 */
	N randomUniform(Random random);
	
	/**
	 * Generate a uniform random value with gaussian distribution centered
	 * on this range, with standard deviation equal to the radius of this
	 * range.<br>
	 * In particular, the returned values can be outside range.<br>
	 * Exclusiveness of the limits is ignored.<br>
	 * If this range is empty, {@code null} is returned.
	 */
	N randomGaussian(Random random);
	
	/**
	 * Get the center of this range, that is, the point at the same
	 * distance of the bounds.<br>
	 */
	double getCenter();
	
	/**
	 * Get the radius of this range, that is, the distance from
	 * the center to the bounds.
	 */
	double getRadius();
	
	/**
	 * Translate the range by a given offset.
	 */
	Self translate(N translation);
	
	/**
	 * Grow the range by a given factor in each direction.
	 */
	Self growRelative(N left, N right);
	
	/**
	 * Grow the range by a given factor in both directions.
	 */
	Self growRelative(N both);
	
	/**
	 * Grow the range by a given amount in each direction.
	 */
	Self grow(N left, N right);
	
	/**
	 * Grow the range by a given amount in both directions.
	 */
	Self grow(N both);
	
	/**
	 * Shrink the range by a given factor in each direction.
	 */
	Self shrinkRelative(N left, N right);
	
	/**
	 * Shrink the range by a given factor in both directions.
	 */
	Self shrinkRelative(N both);
	
	/**
	 * Shrink the range by a given amount in each direction.
	 */
	Self shrink(N left, N right);
	
	/**
	 * Shrink the range by a given amount in both directions.
	 */
	Self shrink(N both);
}
