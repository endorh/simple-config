package endorh.simpleconfig.api.entry;

import endorh.simpleconfig.api.AtomicEntryBuilder;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public interface ByteEntryBuilder
  extends RangedEntryBuilder<@NotNull Byte, Number, Integer, ByteEntryBuilder>, AtomicEntryBuilder {
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
