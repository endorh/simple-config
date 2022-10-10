package endorh.simpleconfig.api.entry;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public interface ShortListEntryBuilder extends RangedListEntryBuilder<Short, Number, Integer, ShortListEntryBuilder> {
	/**
	 * Set the minimum allowed value for the elements of this list entry (inclusive)
	 */
	@Contract(pure=true) @NotNull ShortListEntryBuilder min(short min);
	
	/**
	 * Set the maximum allowed value for the elements of this list entry (inclusive)
	 */
	@Contract(pure=true) @NotNull ShortListEntryBuilder max(short max);
	
	/**
	 * Set the minimum and the maximum allowed for the elements of this list entry (inclusive)
	 */
	@Contract(pure=true) @NotNull ShortListEntryBuilder range(short min, short max);
}
