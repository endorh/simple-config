package endorh.simpleconfig.api.entry;

import endorh.simpleconfig.api.ConfigEntryBuilder;
import endorh.simpleconfig.api.KeyEntryBuilder;
import endorh.simpleconfig.api.ui.icon.Icon;
import org.apache.commons.lang3.tuple.Triple;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;

public interface EntryTripleEntryBuilder<
  L, M, R, LC, MC, RC, LG, MG, RG
> extends ConfigEntryBuilder<
  Triple<L, M, R>, Triple<LC, MC, RC>, Triple<LG, MG, RG>, EntryTripleEntryBuilder<L, M, R, LC, MC, RC, LG, MG, RG>
>, KeyEntryBuilder<Triple<LG, MG, RG>> {
	@Contract(pure=true)
	EntryTripleEntryBuilder<L, M, R, LC, MC, RC, LG, MG, RG> withLeftIcon(@Nullable Icon leftIcon);
	
	@Contract(pure=true) EntryTripleEntryBuilder<L, M, R, LC, MC, RC, LG, MG, RG> withRightIcon(@Nullable Icon rightIcon);
	
	@Contract(pure=true) EntryTripleEntryBuilder<L, M, R, LC, MC, RC, LG, MG, RG> withIcons(@Nullable Icon leftIcon, @Nullable Icon rightIcon);
	
	@Contract(pure=true)
	EntryTripleEntryBuilder<L, M, R, LC, MC, RC, LG, MG, RG> withWeights(@Range(from=0, to=1) double leftWeight, @Range(from=0, to=1) double rightWeight);
}
