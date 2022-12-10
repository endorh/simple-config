package endorh.simpleconfig.api.range;

public interface SizedRange<V extends Comparable<V>, Self extends SizedRange<V, Self>>
  extends Range<V, Self> {
	/**
	 * Get the size of this range, that is, the distance between
	 * its min and max values.<br>
	 * If the max value is smaller than the min, the size will
	 * be negative.<br>
	 * If the range is empty (size 0 with at least one exclusive bound),
	 * the size will be negative ({@code -Double.MIN_VALUE}).
	 */
	double getSize();
	
	/**
	 * Check if this range is empty.<br>
	 * A range is determined to be empty when its size is
	 * 0 and at least one bound is exclusive, or when
	 * the max value is smaller than the min value.
	 */
	boolean isEmpty();
}
