package endorh.simpleconfig.api.entry;

import endorh.simpleconfig.api.AtomicEntryBuilder;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public interface ShortEntryBuilder
  extends RangedEntryBuilder<@NotNull Short, Number, Integer, ShortEntryBuilder>,
          AtomicEntryBuilder {
	/** Set min (inclusive) */
	@Contract(pure=true) @NotNull default ShortEntryBuilder min(short min) {
		return min((Short) min);
	}
	
	/** Set max (inclusive) */
	@Contract(pure=true) @NotNull default ShortEntryBuilder max(short max) {
		return max((Short) max);
	}
	
	/** Set range (inclusive) */
	@Contract(pure=true) @NotNull default ShortEntryBuilder range(short min, short max) {
		return range((Short) min, (Short) max);
	}
	
	/**
	 * Set a different range (inclusive) for the slider.<br>
	 * Will be constrained by the range set by {@link #range}.<br>
	 * Useful to display a recommended/commonly useful range of values,
	 * while allowing users to override this range by manually entering
	 * a value beyond it (but still within the normal range).<br>
	 * Implies {@link #slider()}.
	 */
	@Contract(pure=true) default @NotNull ShortEntryBuilder sliderRange(short min, short max) {
		return sliderRange((Short) min, (Short) max);
	}
}
