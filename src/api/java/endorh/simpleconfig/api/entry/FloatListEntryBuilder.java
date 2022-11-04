package endorh.simpleconfig.api.entry;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public interface FloatListEntryBuilder
  extends RangedListEntryBuilder<@NotNull Float, Number, Float, FloatListEntryBuilder> {
	/**
	 * Set the minimum allowed value for the elements of this list entry (inclusive)
	 */
	@Contract(pure=true) @NotNull FloatListEntryBuilder min(float min);
	
	/**
	 * Set the maximum allowed value for the elements of this list entry (inclusive)
	 */
	@Contract(pure=true) @NotNull FloatListEntryBuilder max(float max);
	
	/**
	 * Set the minimum and the maximum allowed for the elements of this list entry (inclusive)
	 */
	@Contract(pure=true) @NotNull FloatListEntryBuilder range(float min, float max);
}
