package endorh.simpleconfig.ui.gui.entries;

import com.google.common.collect.Lists;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public abstract class AbstractEntryHoldingEntry<T> extends TooltipListEntry<T> {
	protected final List<TooltipListEntry<?>> subEntries;
	
	public AbstractEntryHoldingEntry(Component fieldName, TooltipListEntry<?>... subEntries) {
		super(fieldName);
		this.subEntries = Lists.newArrayList(subEntries);
	}
	
	@Override public void updateFocused(boolean isFocused) {
		super.updateFocused(isFocused);
	}
	
	@Override protected @NotNull List<? extends GuiEventListener> getEntryListeners() {
		return subEntries;
	}
}
