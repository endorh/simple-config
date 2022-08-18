package endorh.simpleconfig.api.entry;

import endorh.simpleconfig.api.AbstractRange.LongRange;
import endorh.simpleconfig.api.KeyEntryBuilder;
import org.jetbrains.annotations.Contract;

public interface LongRangeEntryBuilder
  extends SizedRangeEntryBuilder<Long, LongRange, LongRangeEntryBuilder>, KeyEntryBuilder<LongRange> {
	@Contract(pure=true) LongRangeEntryBuilder min(long min);
	
	@Contract(pure=true) LongRangeEntryBuilder max(long max);
	
	@Contract(pure=true) LongRangeEntryBuilder withBounds(long min, long max);
}
