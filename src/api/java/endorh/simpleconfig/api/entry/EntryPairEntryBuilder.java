package endorh.simpleconfig.api.entry;

import endorh.simpleconfig.api.ConfigEntryBuilder;
import endorh.simpleconfig.api.KeyEntryBuilder;
import endorh.simpleconfig.ui.icon.Icon;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;

public interface EntryPairEntryBuilder<
  L, R, LC, RC, LG, RG
> extends ConfigEntryBuilder<
  Pair<L, R>, Pair<LC, RC>, Pair<LG, RG>, EntryPairEntryBuilder<L, R, LC, RC, LG, RG>
>, KeyEntryBuilder<Pair<LG, RG>> {
	@Contract(pure=true) EntryPairEntryBuilder<L, R, LC, RC, LG, RG> withMiddleIcon(
	  @Nullable Icon icon
	);
	
	@Contract(pure=true) EntryPairEntryBuilder<L, R, LC, RC, LG, RG> withSplitPosition(
	  @Range(from=0, to=1) double splitPosition
	);
}
