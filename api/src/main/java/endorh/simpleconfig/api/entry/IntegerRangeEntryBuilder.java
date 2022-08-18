package endorh.simpleconfig.api.entry;

import endorh.simpleconfig.api.AbstractRange.IntRange;
import endorh.simpleconfig.api.KeyEntryBuilder;
import org.jetbrains.annotations.Contract;

public interface IntegerRangeEntryBuilder
  extends SizedRangeEntryBuilder<Integer, IntRange, IntegerRangeEntryBuilder>, KeyEntryBuilder<IntRange> {
	@Contract(pure=true) IntegerRangeEntryBuilder min(int min);
	
	@Contract(pure=true) IntegerRangeEntryBuilder max(int max);
	
	@Contract(pure=true) IntegerRangeEntryBuilder withBounds(int min, int max);
}
