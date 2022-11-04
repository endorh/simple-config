package endorh.simpleconfig.api.entry;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public interface IntegerListEntryBuilder
  extends RangedListEntryBuilder<@NotNull Integer, Number, Integer, IntegerListEntryBuilder> {
	/**
	 * Set the minimum allowed value for the elements of this list entry (inclusive)
	 */
	@Contract(pure=true) @NotNull IntegerListEntryBuilder min(int min);
	
	/**
	 * Set the maximum allowed value for the elements of this list entry (inclusive)
	 */
	@Contract(pure=true) @NotNull IntegerListEntryBuilder max(int max);
	
	/**
	 * Set the minimum and the maximum allowed for the elements of this list entry (inclusive)
	 */
	@Contract(pure=true) @NotNull IntegerListEntryBuilder range(int min, int max);
}
