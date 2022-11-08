package endorh.simpleconfig.api.entry;

import endorh.simpleconfig.api.AtomicEntryBuilder;
import endorh.simpleconfig.api.ConfigEntryBuilder;
import endorh.simpleconfig.api.ui.icon.Icon;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;

public interface EntryPairEntryBuilder<
  L, R, LC, RC, LG, RG
> extends ConfigEntryBuilder<
  @NotNull Pair<@NotNull L, @NotNull R>, Pair<LC, RC>, Pair<LG, RG>,
  EntryPairEntryBuilder<L, R, LC, RC, LG, RG>
>, AtomicEntryBuilder {
	/**
	 * Set the icon displayed between the left and right entries.<br>
	 * By default, there is no icon.
	 */
	@Contract(pure=true) @NotNull EntryPairEntryBuilder<L, R, LC, RC, LG, RG> withMiddleIcon(
	  @Nullable Icon icon
	);
	
	/**
	 * Set the fraction at which the left and right entries are split.<br>
	 * @param splitPosition A value between 0 and 1 determining the amount of
	 * horizontal space dedicated to the left entry in the menu (default: 0.5)
	 */
	@Contract(pure=true) @NotNull EntryPairEntryBuilder<L, R, LC, RC, LG, RG> withSplitPosition(
	  @Range(from=0, to=1) double splitPosition
	);
}
