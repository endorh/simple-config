package endorh.simpleconfig.ui.gui.entries;

import endorh.simpleconfig.ui.api.AbstractConfigListEntry;
import endorh.simpleconfig.ui.api.IChildListEntry;
import org.jetbrains.annotations.Nullable;

public interface IEntryHoldingListEntry {
	<E extends AbstractConfigListEntry<?> & IChildListEntry> @Nullable E getHeldEntry();
	<E extends AbstractConfigListEntry<?> & IChildListEntry> void setHeldEntry(E entry);
}
