package endorh.simpleconfig.api.entry;

import endorh.simpleconfig.api.KeyEntryBuilder;
import org.jetbrains.annotations.Contract;

public interface LongEntryBuilder
  extends RangedEntryBuilder<Long, Number, Long, LongEntryBuilder>, KeyEntryBuilder<Long> {
	/**
	 * Set min (inclusive)
	 */
	@Contract(pure=true) LongEntryBuilder min(long min);
	
	/**
	 * Set max (inclusive)
	 */
	@Contract(pure=true) LongEntryBuilder max(long max);
	
	/**
	 * Set range (inclusive)
	 */
	@Contract(pure=true) LongEntryBuilder range(long min, long max);
}
