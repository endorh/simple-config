package endorh.simpleconfig.api.entry;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public interface ByteListEntryBuilder
  extends RangedListEntryBuilder<@NotNull Byte, Number, Integer, ByteListEntryBuilder> {
	/**
	 * Set the minimum allowed value for the elements of this list entry (inclusive)
	 */
	@Contract(pure=true) @NotNull ByteListEntryBuilder min(byte min);
	
	/**
	 * Set the maximum allowed value for the elements of this list entry (inclusive)
	 */
	@Contract(pure=true) @NotNull ByteListEntryBuilder max(byte max);
	
	/**
	 * Set the minimum and the maximum allowed for the elements of this list entry (inclusive)
	 */
	@Contract(pure=true) @NotNull ByteListEntryBuilder range(byte min, byte max);
}
