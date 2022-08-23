package endorh.simpleconfig.api.entry;

import endorh.simpleconfig.api.KeyEntryBuilder;
import org.jetbrains.annotations.Contract;

public interface ByteEntryBuilder
  extends RangedEntryBuilder<Byte, Number, Integer, ByteEntryBuilder>, KeyEntryBuilder<Integer> {
	/**
	 * Set min (inclusive)
	 */
	@Contract(pure=true) ByteEntryBuilder min(byte min);
	
	/**
	 * Set max (inclusive)
	 */
	@Contract(pure=true) ByteEntryBuilder max(byte max);
	
	/**
	 * Set range (inclusive)
	 */
	@Contract(pure=true) ByteEntryBuilder range(byte min, byte max);
}
