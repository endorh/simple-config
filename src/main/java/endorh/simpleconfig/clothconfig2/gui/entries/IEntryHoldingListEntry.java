package endorh.simpleconfig.clothconfig2.gui.entries;

import endorh.simpleconfig.clothconfig2.api.AbstractConfigListEntry;
import endorh.simpleconfig.clothconfig2.api.IChildListEntry;
import org.jetbrains.annotations.Nullable;

public interface IEntryHoldingListEntry {
	<E extends AbstractConfigListEntry<?> & IChildListEntry> @Nullable E getHeldEntry();
	<E extends AbstractConfigListEntry<?> & IChildListEntry> void setHeldEntry(E entry);
}
