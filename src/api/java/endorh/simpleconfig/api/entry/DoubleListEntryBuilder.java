package endorh.simpleconfig.api.entry;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public interface DoubleListEntryBuilder extends RangedListEntryBuilder<
  @NotNull Double, Number, Double, DoubleListEntryBuilder
> {
	/**
	 * Set the minimum allowed value for the elements of this list entry (inclusive)
	 */
	@Contract(pure=true) @NotNull DoubleListEntryBuilder min(double min);
	
	/**
	 * Set the maximum allowed value for the elements of this list entry (inclusive)
	 */
	@Contract(pure=true) @NotNull DoubleListEntryBuilder max(double max);
	
	/**
	 * Set the minimum and the maximum allowed for the elements of this list entry (inclusive)
	 */
	@Contract(pure=true) @NotNull DoubleListEntryBuilder range(double min, double max);
}
