package endorh.simpleconfig.api.entry;

import endorh.simpleconfig.api.KeyEntryBuilder;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public interface ShortEntryBuilder
  extends RangedEntryBuilder<Short, Number, Integer, ShortEntryBuilder>, KeyEntryBuilder<Integer> {
	/** Set min (inclusive) */
	@Contract(pure=true) @NotNull ShortEntryBuilder min(short min);
	
	/** Set max (inclusive) */
	@Contract(pure=true) @NotNull ShortEntryBuilder max(short max);
	
	/** Set range (inclusive) */
	@Contract(pure=true) @NotNull ShortEntryBuilder range(short min, short max);
}
