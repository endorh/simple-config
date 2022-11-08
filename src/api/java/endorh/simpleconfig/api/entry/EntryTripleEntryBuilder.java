package endorh.simpleconfig.api.entry;

import endorh.simpleconfig.api.AtomicEntryBuilder;
import endorh.simpleconfig.api.ConfigEntryBuilder;
import endorh.simpleconfig.api.ui.icon.Icon;
import org.apache.commons.lang3.tuple.Triple;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;

public interface EntryTripleEntryBuilder<
  L, M, R, LC, MC, RC, LG, MG, RG
> extends ConfigEntryBuilder<
  @NotNull Triple<@NotNull L, @NotNull M, @NotNull R>, Triple<LC, MC, RC>, Triple<LG, MG, RG>,
  EntryTripleEntryBuilder<L, M, R, LC, MC, RC, LG, MG, RG>
>, AtomicEntryBuilder {
	/**
	 * Set the icon displayed between the left and middle entries.<br>
	 * By default, there is no icon.
	 */
	@Contract(pure=true)
	EntryTripleEntryBuilder<L, M, R, LC, MC, RC, LG, MG, RG> withLeftIcon(@Nullable Icon leftIcon);
	
	/**
	 * Set the icon displayed between the middle and right entries.<br>
	 * By default, there is no icon.
	 */
	@Contract(pure=true) @NotNull EntryTripleEntryBuilder<L, M, R, LC, MC, RC, LG, MG, RG> withRightIcon(@Nullable Icon rightIcon);
	
	/**
	 * Set the icons displayed between the left, middle and right entries.<br>
	 * By default, there are no icons.
	 */
	@Contract(pure=true) @NotNull EntryTripleEntryBuilder<L, M, R, LC, MC, RC, LG, MG, RG> withIcons(@Nullable Icon leftIcon, @Nullable Icon rightIcon);
	
	/**
	 * Set the weights for the left and right entries.<br>
	 * The horizontal space in the menu is split between the 3 entries
	 * according to their weight.<br>
	 * The weight for the middle entry is the remaining weight to reach 1.<br>
	 * By default, the weights are 0.33 and 0.33.
	 * @param leftWeight The weight for the left entry (default: 0.33)
	 * @param rightWeight The weight for the right entry (default: 0.33)
	 */
	@Contract(pure=true)
	EntryTripleEntryBuilder<L, M, R, LC, MC, RC, LG, MG, RG> withWeights(@Range(from=0, to=1) double leftWeight, @Range(from=0, to=1) double rightWeight);
}
