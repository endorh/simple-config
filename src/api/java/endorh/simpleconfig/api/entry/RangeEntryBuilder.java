package endorh.simpleconfig.api.entry;

import endorh.simpleconfig.api.AbstractRange;
import endorh.simpleconfig.api.ConfigEntryBuilder;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public interface RangeEntryBuilder<
  V extends Comparable<V>, R extends AbstractRange<V, R>,
  Self extends RangeEntryBuilder<V, R, Self>
> extends ConfigEntryBuilder<R, String, R, Self> {
	
	@Contract(pure=true) @NotNull Self min(V min);
	
	@Contract(pure=true) @NotNull Self max(V max);
	
	@Contract(pure=true) @NotNull Self withBounds(V min, V max);
	
	@Contract(pure=true) @NotNull Self canEditMinExclusive();
	
	@Contract(pure=true) @NotNull Self canEditMinExclusive(boolean exclusive);
	
	@Contract(pure=true) @NotNull Self canEditMaxExclusive();
	
	@Contract(pure=true) @NotNull Self canEditMaxExclusive(boolean exclusive);
	
	@Contract(pure=true) @NotNull Self canEditExclusiveness(boolean min, boolean max);
	
	@Contract(pure=true) @NotNull Self canEditExclusiveness();
	
	@Contract(pure=true) @NotNull Self canEditExclusiveness(boolean canEdit);
}
