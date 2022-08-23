package endorh.simpleconfig.api.entry;

import org.jetbrains.annotations.Contract;

public interface DoubleListEntryBuilder extends RangedListEntryBuilder<Double, Number, Double, DoubleListEntryBuilder> {
	/**
	 * Set the minimum allowed value for the elements of this list entry (inclusive)
	 */
	@Contract(pure=true) DoubleListEntryBuilder min(double min);
	
	/**
	 * Set the maximum allowed value for the elements of this list entry (inclusive)
	 */
	@Contract(pure=true) DoubleListEntryBuilder max(double max);
	
	/**
	 * Set the minimum and the maximum allowed for the elements of this list entry (inclusive)
	 */
	@Contract(pure=true) DoubleListEntryBuilder range(double min, double max);
}
