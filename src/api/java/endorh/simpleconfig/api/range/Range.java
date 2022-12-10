package endorh.simpleconfig.api.range;

import java.util.Map;

public interface Range<V extends Comparable<V>, Self extends Range<V, Self>>
  extends Map.Entry<V, V> {
	boolean contains(V value);
	
	Self create(V min, V max, boolean exclusiveMin, boolean exclusiveMax);
	
	Self intersect(Self range);
	
	/**
	 * Whether the min value is exclusive (not included in the range)
	 */
	boolean isExclusiveMin();
	
	/**
	 * Whether the max value is exclusive (not included in the range)
	 */
	boolean isExclusiveMax();
	
	V getMin();
	
	V getMax();
	
	@Override default V getKey() {
		return getMin();
	}
	
	@Override default V getValue() {
		return getMax();
	}
	
	/**
	 * @return never
	 * @throws UnsupportedOperationException always
	 * @deprecated Unsupported
	 */
	@Deprecated @Override default V setValue(V value) {
		throw new UnsupportedOperationException("Immutable pair cannot be modified");
	}
}
