package endorh.simpleconfig.api.entry;

import org.jetbrains.annotations.Contract;

public interface ByteListEntryBuilder extends RangedListEntryBuilder<Byte, Number, Integer, ByteListEntryBuilder> {
	/**
	 * Set the minimum allowed value for the elements of this list entry (inclusive)
	 */
	@Contract(pure=true) ByteListEntryBuilder min(byte min);
	
	/**
	 * Set the maximum allowed value for the elements of this list entry (inclusive)
	 */
	@Contract(pure=true) ByteListEntryBuilder max(byte max);
	
	/**
	 * Set the minimum and the maximum allowed for the elements of this list entry (inclusive)
	 */
	@Contract(pure=true) ByteListEntryBuilder range(byte min, byte max);
}
