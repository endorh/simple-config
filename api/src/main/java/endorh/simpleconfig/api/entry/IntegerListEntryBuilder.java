package endorh.simpleconfig.api.entry;

import org.jetbrains.annotations.Contract;

public interface IntegerListEntryBuilder extends RangedListEntryBuilder<Integer, Number, Integer, IntegerListEntryBuilder> {
	/**
	 * Set the minimum allowed value for the elements of this list entry (inclusive)
	 */
	@Contract(pure=true) IntegerListEntryBuilder min(int min);
	
	/**
	 * Set the maximum allowed value for the elements of this list entry (inclusive)
	 */
	@Contract(pure=true) IntegerListEntryBuilder max(int max);
	
	/**
	 * Set the minimum and the maximum allowed for the elements of this list entry (inclusive)
	 */
	@Contract(pure=true) IntegerListEntryBuilder range(int min, int max);
}
