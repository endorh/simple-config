package endorh.simpleconfig.api.entry;

import endorh.simpleconfig.api.AtomicEntryBuilder;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public interface LongEntryBuilder
  extends RangedEntryBuilder<@NotNull Long, Number, Long, LongEntryBuilder>, AtomicEntryBuilder {
	/**
	 * Set min (inclusive)
	 */
	@Contract(pure=true) @NotNull default LongEntryBuilder min(long min) {
		return min((Long) min);
	}
	
	/**
	 * Set max (inclusive)
	 */
	@Contract(pure=true) @NotNull default LongEntryBuilder max(long max) {
		return max((Long) max);
	}
	
	/**
	 * Set range (inclusive)
	 */
	@Contract(pure=true) @NotNull default LongEntryBuilder range(long min, long max) {
		return range((Long) min, (Long) max);
	}
	
	/**
	 * Set a different range (inclusive) for the slider.<br>
	 * Will be constrained by the range set by {@link #range}.<br>
	 * Useful to display a recommended/commonly useful range of values,
	 * while allowing users to override this range by manually entering
	 * a value beyond it (but still within the normal range).<br>
	 * Implies {@link #slider()}.
	 */
	@Contract(pure=true) default @NotNull LongEntryBuilder sliderRange(long min, long max) {
		return sliderRange((Long) min, (Long) max);
	}
}
