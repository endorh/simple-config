package endorh.simpleconfig.api.entry;

import endorh.simpleconfig.api.AbstractRange.LongRange;
import endorh.simpleconfig.api.AtomicEntryBuilder;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public interface LongRangeEntryBuilder
  extends SizedRangeEntryBuilder<@NotNull Long, LongRange, LongRangeEntryBuilder>,
          AtomicEntryBuilder {
	/**
	 * Set min (inclusive)
	 */
	@Contract(pure=true) @NotNull LongRangeEntryBuilder min(long min);
	
	/**
	 * Set max (inclusive)
	 */
	@Contract(pure=true) @NotNull LongRangeEntryBuilder max(long max);
	
	/**
	 * Set bounds (inclusive)
	 */
	@Contract(pure=true) @NotNull LongRangeEntryBuilder withBounds(long min, long max);
}
