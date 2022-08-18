package endorh.simpleconfig.api.entry;

import endorh.simpleconfig.api.KeyEntryBuilder;
import org.jetbrains.annotations.Contract;

public interface IntegerEntryBuilder
  extends RangedEntryBuilder<Integer, Number, Integer, IntegerEntryBuilder>, KeyEntryBuilder<Integer> {
	/**
	 * Set min (inclusive)
	 */
	@Contract(pure=true) IntegerEntryBuilder min(int min);
	
	/**
	 * Set max (inclusive)
	 */
	@Contract(pure=true) IntegerEntryBuilder max(int max);
	
	/**
	 * Set range (inclusive)
	 */
	@Contract(pure=true) IntegerEntryBuilder range(int min, int max);
}
