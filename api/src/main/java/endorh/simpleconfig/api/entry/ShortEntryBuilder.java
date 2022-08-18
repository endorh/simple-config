package endorh.simpleconfig.api.entry;

import endorh.simpleconfig.api.KeyEntryBuilder;
import org.jetbrains.annotations.Contract;

public interface ShortEntryBuilder
  extends RangedEntryBuilder<Short, Number, Integer, ShortEntryBuilder>, KeyEntryBuilder<Integer> {
	/** Set min (inclusive) */
	@Contract(pure=true) ShortEntryBuilder min(short min);
	
	/** Set max (inclusive) */
	@Contract(pure=true) ShortEntryBuilder max(short max);
	
	/** Set range (inclusive) */
	@Contract(pure=true) ShortEntryBuilder range(short min, short max);
}
