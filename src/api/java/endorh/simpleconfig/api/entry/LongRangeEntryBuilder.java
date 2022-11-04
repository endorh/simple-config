package endorh.simpleconfig.api.entry;

import endorh.simpleconfig.api.AbstractRange.LongRange;
import endorh.simpleconfig.api.AtomicEntryBuilder;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public interface LongRangeEntryBuilder
  extends SizedRangeEntryBuilder<@NotNull Long, LongRange, LongRangeEntryBuilder>,
          AtomicEntryBuilder {
	@Contract(pure=true) @NotNull LongRangeEntryBuilder min(long min);
	
	@Contract(pure=true) @NotNull LongRangeEntryBuilder max(long max);
	
	@Contract(pure=true) @NotNull LongRangeEntryBuilder withBounds(long min, long max);
}
