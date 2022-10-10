package endorh.simpleconfig.api.entry;

import endorh.simpleconfig.api.KeyEntryBuilder;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public interface ByteEntryBuilder
  extends RangedEntryBuilder<Byte, Number, Integer, ByteEntryBuilder>, KeyEntryBuilder<Integer> {
	/**
	 * Set min (inclusive)
	 */
	@Contract(pure=true) @NotNull ByteEntryBuilder min(byte min);
	
	/**
	 * Set max (inclusive)
	 */
	@Contract(pure=true) @NotNull ByteEntryBuilder max(byte max);
	
	/**
	 * Set range (inclusive)
	 */
	@Contract(pure=true) @NotNull ByteEntryBuilder range(byte min, byte max);
}
