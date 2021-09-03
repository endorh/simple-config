package endorh.simple_config.clothconfig2.gui.entries;

import endorh.simple_config.clothconfig2.api.AbstractConfigListEntry;
import endorh.simple_config.clothconfig2.api.IChildListEntry;
import org.jetbrains.annotations.Nullable;

public interface IEntryHoldingListEntry {
	<E extends AbstractConfigListEntry<?> & IChildListEntry> @Nullable E getHeldEntry();
	<E extends AbstractConfigListEntry<?> & IChildListEntry> void setHeldEntry(E entry);
}
