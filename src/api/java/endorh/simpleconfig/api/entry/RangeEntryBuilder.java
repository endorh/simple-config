package endorh.simpleconfig.api.entry;

import endorh.simpleconfig.api.AbstractRange;
import endorh.simpleconfig.api.ConfigEntryBuilder;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public interface RangeEntryBuilder<
  V extends Comparable<V>, R extends AbstractRange<V, R>,
  Self extends RangeEntryBuilder<V, R, Self>
> extends ConfigEntryBuilder<@NotNull R, String, R, Self> {
	
	/**
	 * Set min (inclusive)
	 */
	@Contract(pure=true) @NotNull Self min(V min);
	
	/**
	 * Set max (inclusive)
	 */
	@Contract(pure=true) @NotNull Self max(V max);
	
	/**
	 * Set bounds (inclusive)
	 */
	@Contract(pure=true) @NotNull Self withBounds(V min, V max);
	
	/**
	 * Make the min value exclusivity of this range editable by the user.
	 */
	@Contract(pure=true) @NotNull Self canEditMinExclusive();
	
	/**
	 * Configure if the min value exclusivity of this range should
	 * be editable by the user.
	 */
	@Contract(pure=true) @NotNull Self canEditMinExclusive(boolean exclusive);
	
	/**
	 * Make the max value exclusivity of this range editable by the user.
	 */
	@Contract(pure=true) @NotNull Self canEditMaxExclusive();
	
	/**
	 * Configure if the max value exclusivity of this range should
	 * be editable by the user.
	 */
	@Contract(pure=true) @NotNull Self canEditMaxExclusive(boolean exclusive);
	
	/**
	 * Configure if the min and max value exclusivity of this range should
	 * be editable by the user.
	 */
	@Contract(pure=true) @NotNull Self canEditExclusiveness(boolean min, boolean max);
	
	/**
	 * Make the min and max value exclusivity of this range editable by the user.
	 */
	@Contract(pure=true) @NotNull Self canEditExclusiveness();
	
	/**
	 * Configure if the min and max value exclusivity of this range should
	 * be editable by the user.
	 */
	@Contract(pure=true) @NotNull Self canEditExclusiveness(boolean canEdit);
}
