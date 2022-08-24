package endorh.simpleconfig.api.entry;

import org.jetbrains.annotations.Contract;

public interface FloatListEntryBuilder extends RangedListEntryBuilder<Float, Number, Float, FloatListEntryBuilder> {
	/**
	 * Set the minimum allowed value for the elements of this list entry (inclusive)
	 */
	@Contract(pure=true) FloatListEntryBuilder min(float min);
	
	/**
	 * Set the maximum allowed value for the elements of this list entry (inclusive)
	 */
	@Contract(pure=true) FloatListEntryBuilder max(float max);
	
	/**
	 * Set the minimum and the maximum allowed for the elements of this list entry (inclusive)
	 */
	@Contract(pure=true) FloatListEntryBuilder range(float min, float max);
}
