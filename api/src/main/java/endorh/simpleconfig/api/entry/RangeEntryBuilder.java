package endorh.simpleconfig.api.entry;

import endorh.simpleconfig.api.AbstractRange;
import endorh.simpleconfig.api.ConfigEntryBuilder;
import org.jetbrains.annotations.Contract;

public interface RangeEntryBuilder<
  V extends Comparable<V>, R extends AbstractRange<V, R>,
  Self extends RangeEntryBuilder<V, R, Self>
> extends ConfigEntryBuilder<R, String, R, Self> {
	
	@Contract(pure=true) Self min(V min);
	
	@Contract(pure=true) Self max(V max);
	
	@Contract(pure=true) Self withBounds(V min, V max);
	
	@Contract(pure=true) Self canEditMinExclusive();
	
	@Contract(pure=true) Self canEditMinExclusive(boolean exclusive);
	
	@Contract(pure=true) Self canEditMaxExclusive();
	
	@Contract(pure=true) Self canEditMaxExclusive(boolean exclusive);
	
	@Contract(pure=true) Self canEditExclusiveness(boolean min, boolean max);
	
	@Contract(pure=true) Self canEditExclusiveness();
	
	@Contract(pure=true) Self canEditExclusiveness(boolean canEdit);
}
