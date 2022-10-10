package endorh.simpleconfig.api.entry;

import endorh.simpleconfig.api.AbstractRange.LongRange;
import endorh.simpleconfig.api.KeyEntryBuilder;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public interface LongRangeEntryBuilder
  extends SizedRangeEntryBuilder<Long, LongRange, LongRangeEntryBuilder>, KeyEntryBuilder<LongRange> {
	@Contract(pure=true) @NotNull LongRangeEntryBuilder min(long min);
	
	@Contract(pure=true) @NotNull LongRangeEntryBuilder max(long max);
	
	@Contract(pure=true) @NotNull LongRangeEntryBuilder withBounds(long min, long max);
}
