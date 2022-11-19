package endorh.simpleconfig.api.entry;

import endorh.simpleconfig.api.AtomicEntryBuilder;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public interface ByteEntryBuilder
  extends RangedEntryBuilder<@NotNull Byte, Number, Integer, ByteEntryBuilder>, AtomicEntryBuilder {
	/**
	 * Set min (inclusive)
	 */
	@Contract(pure=true) @NotNull default ByteEntryBuilder min(byte min) {
		return min((Byte) min);
	}
	
	/**
	 * Set max (inclusive)
	 */
	@Contract(pure=true) @NotNull default ByteEntryBuilder max(byte max) {
		return max((Byte) max);
	}
	
	/**
	 * Set range (inclusive)
	 */
	@Contract(pure=true) @NotNull default ByteEntryBuilder range(byte min, byte max) {
		return range((Byte) min, (Byte) max);
	}
	
	/**
	 * Set a different range (inclusive) for the slider.<br>
	 * Will be constrained by the range set by {@link #range}.<br>
	 * Useful to display a recommended/commonly useful range of values,
	 * while allowing users to override this range by manually entering
	 * a value beyond it (but still within the normal range).<br>
	 * Implies {@link #slider()}.
	 */
	@Contract(pure=true) default @NotNull ByteEntryBuilder sliderRange(byte min, byte max) {
		return sliderRange((Byte) min, (Byte) max);
	}
}
