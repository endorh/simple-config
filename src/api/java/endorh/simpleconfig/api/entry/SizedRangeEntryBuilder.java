package endorh.simpleconfig.api.entry;

import endorh.simpleconfig.api.AbstractRange.AbstractSizedRange;
import org.jetbrains.annotations.Contract;

public interface SizedRangeEntryBuilder<
  V extends Comparable<V>, R extends AbstractSizedRange<V, R>,
  Self extends SizedRangeEntryBuilder<V, R, Self>
> extends RangeEntryBuilder<V, R, Self> {
	/**
	 * Allow empty ranges.<br>
	 * By default, empty ranges are not allowed.<br>
	 * Equivalent to {@code minSize(empty? Double.NEGATIVE_INFINITY : 0)}.
	 */
	@Contract(pure=true) Self allowEmpty(boolean empty);
	
	/**
	 * Allow only range values with at least the given size.<br>
	 * Empty ranges have negative sizes, so a minSize of 0 will
	 * only prevent empty ranges.<br>
	 * A range with only one value is not considered empty.
	 */
	@Contract(pure=true) Self minSize(double size);
	
	/**
	 * Allow only range values with at most the given size.
	 */
	@Contract(pure=true) Self maxSize(double size);
}
