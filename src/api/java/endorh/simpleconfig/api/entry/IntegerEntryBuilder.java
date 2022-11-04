package endorh.simpleconfig.api.entry;

import endorh.simpleconfig.api.AtomicEntryBuilder;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public interface IntegerEntryBuilder
  extends RangedEntryBuilder<@NotNull Integer, Number, Integer, IntegerEntryBuilder>,
          AtomicEntryBuilder {
	/**
	 * Set min (inclusive)
	 */
	@Contract(pure=true) @NotNull IntegerEntryBuilder min(int min);
	
	/**
	 * Set max (inclusive)
	 */
	@Contract(pure=true) @NotNull IntegerEntryBuilder max(int max);
	
	/**
	 * Set range (inclusive)
	 */
	@Contract(pure=true) @NotNull IntegerEntryBuilder range(int min, int max);
}
