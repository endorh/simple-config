package endorh.simpleconfig.api.entry;

import endorh.simpleconfig.api.KeyEntryBuilder;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public interface LongEntryBuilder
  extends RangedEntryBuilder<Long, Number, Long, LongEntryBuilder>, KeyEntryBuilder<Long> {
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
