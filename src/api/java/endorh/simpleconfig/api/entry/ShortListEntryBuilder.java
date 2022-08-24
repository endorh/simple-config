package endorh.simpleconfig.api.entry;

import org.jetbrains.annotations.Contract;

public interface ShortListEntryBuilder extends RangedListEntryBuilder<Short, Number, Integer, ShortListEntryBuilder> {
	/**
	 * Set the minimum allowed value for the elements of this list entry (inclusive)
	 */
	@Contract(pure=true) ShortListEntryBuilder min(short min);
	
	/**
	 * Set the maximum allowed value for the elements of this list entry (inclusive)
	 */
	@Contract(pure=true) ShortListEntryBuilder max(short max);
	
	/**
	 * Set the minimum and the maximum allowed for the elements of this list entry (inclusive)
	 */
	@Contract(pure=true) ShortListEntryBuilder range(short min, short max);
}
