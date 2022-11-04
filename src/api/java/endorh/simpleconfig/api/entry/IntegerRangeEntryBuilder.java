package endorh.simpleconfig.api.entry;

import endorh.simpleconfig.api.AbstractRange.IntRange;
import endorh.simpleconfig.api.AtomicEntryBuilder;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public interface IntegerRangeEntryBuilder
  extends SizedRangeEntryBuilder<@NotNull Integer, IntRange, IntegerRangeEntryBuilder>,
          AtomicEntryBuilder {
	@Contract(pure=true) @NotNull IntegerRangeEntryBuilder min(int min);
	
	@Contract(pure=true) @NotNull IntegerRangeEntryBuilder max(int max);
	
	@Contract(pure=true) @NotNull IntegerRangeEntryBuilder withBounds(int min, int max);
}
