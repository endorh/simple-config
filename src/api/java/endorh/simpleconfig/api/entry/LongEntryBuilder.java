package endorh.simpleconfig.api.entry;

import endorh.simpleconfig.api.AtomicEntryBuilder;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public interface LongEntryBuilder
  extends RangedEntryBuilder<@NotNull Long, Number, Long, LongEntryBuilder>, AtomicEntryBuilder {
	/**
	 * Set min (inclusive)
	 */
	@Contract(pure=true) @NotNull LongEntryBuilder min(long min);
	
	/**
	 * Set max (inclusive)
	 */
	@Contract(pure=true) @NotNull LongEntryBuilder max(long max);
	
	/**
	 * Set range (inclusive)
	 */
	@Contract(pure=true) @NotNull LongEntryBuilder range(long min, long max);
}
