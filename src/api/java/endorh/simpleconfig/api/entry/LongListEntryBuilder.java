package endorh.simpleconfig.api.entry;

import org.jetbrains.annotations.Contract;

public interface LongListEntryBuilder extends RangedListEntryBuilder<Long, Number, Long, LongListEntryBuilder> {
	/**
	 * Set the minimum allowed value for the elements of this list entry (inclusive)
	 */
	@Contract(pure=true) LongListEntryBuilder min(long min);
	
	/**
	 * Set the maximum allowed value for the elements of this list entry (inclusive)
	 */
	@Contract(pure=true) LongListEntryBuilder max(long max);
	
	/**
	 * Set the minimum and the maximum allowed for the elements of this list entry (inclusive)
	 */
	@Contract(pure=true) LongListEntryBuilder range(long min, long max);
}
