package endorh.simpleconfig.api.entry;

import endorh.simpleconfig.api.AtomicEntryBuilder;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public interface ShortEntryBuilder
  extends RangedEntryBuilder<@NotNull Short, Number, Integer, ShortEntryBuilder>,
          AtomicEntryBuilder {
	/** Set min (inclusive) */
	@Contract(pure=true) @NotNull ShortEntryBuilder min(short min);
	
	/** Set max (inclusive) */
	@Contract(pure=true) @NotNull ShortEntryBuilder max(short max);
	
	/** Set range (inclusive) */
	@Contract(pure=true) @NotNull ShortEntryBuilder range(short min, short max);
}
