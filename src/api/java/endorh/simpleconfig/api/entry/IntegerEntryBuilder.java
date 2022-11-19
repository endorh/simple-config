package endorh.simpleconfig.api.entry;

import endorh.simpleconfig.api.AtomicEntryBuilder;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public interface IntegerEntryBuilder
  extends RangedEntryBuilder<@NotNull Integer, Number, Integer, IntegerEntryBuilder>,
          AtomicEntryBuilder {
	/**
	 * Set min (inclusive)
	 */
	@Contract(pure=true) @NotNull default IntegerEntryBuilder min(int min) {
		return min((Integer) min);
	}
	
	/**
	 * Set max (inclusive)
	 */
	@Contract(pure=true) @NotNull default IntegerEntryBuilder max(int max) {
		return max((Integer) max);
	}
	
	/**
	 * Set range (inclusive)
	 */
	@Contract(pure=true) @NotNull default IntegerEntryBuilder range(int min, int max) {
		return range((Integer) min, (Integer) max);
	}
	
	/**
	 * Set a different range (inclusive) for the slider.<br>
	 * Will be constrained by the range set by {@link #range}.<br>
	 * Useful to display a recommended/commonly useful range of values,
	 * while allowing users to override this range by manually entering
	 * a value beyond it (but still within the normal range).<br>
	 * Implies {@link #slider()}.
	 */
	@Contract(pure=true) default @NotNull IntegerEntryBuilder sliderRange(int min, int max) {
		return sliderRange((Integer) min, (Integer) max);
	}
}
