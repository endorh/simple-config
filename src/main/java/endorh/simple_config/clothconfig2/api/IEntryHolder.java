package endorh.simple_config.clothconfig2.api;

import com.google.common.collect.Lists;
import endorh.simple_config.clothconfig2.api.AbstractConfigEntry.EntryError;
import endorh.simple_config.clothconfig2.gui.entries.IEntryHoldingListEntry;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public interface IEntryHolder {
	/**
	 * The returned list may be mutable
	 */
	@Internal List<AbstractConfigEntry<?>> getEntries();
	
	Pattern DOT = Pattern.compile("\\.");
	default @Nullable AbstractConfigEntry<?> getEntry(String path) {
		final String[] sp = DOT.split(path, 2);
		final List<AbstractConfigEntry<?>> entries = getEntries();
		AbstractConfigEntry<?> entry = entries.stream()
		  .filter(e -> e.getName().equals(sp[0])).findFirst().orElse(null);
		if (entry == null && this instanceof IEntryHoldingListEntry) {
			final AbstractConfigListEntry<?> heldEntry =
			  ((IEntryHoldingListEntry) this).getHeldEntry();
			if (heldEntry != null && heldEntry.getName().equals(sp[0]))
				entry = heldEntry;
		}
		if (sp.length < 2) return entry;
		if (entry instanceof IEntryHolder)
			return ((IEntryHolder) entry).getEntry(sp[1]);
		return entry;
	}
	
	/**
	 * Get all the entries recursively
	 */
	default List<AbstractConfigEntry<?>> getAllEntries() {
		final List<AbstractConfigEntry<?>> entries = Lists.newLinkedList(getEntries());
		if (this instanceof IEntryHoldingListEntry)
			entries.add(0, ((IEntryHoldingListEntry) this).getHeldEntry());
		entries.addAll(entries.stream().filter(e -> e instanceof IEntryHolder)
		  .flatMap(e -> ((IEntryHolder) e).getAllEntries().stream())
		  .collect(Collectors.toList()));
		return entries;
	}
}
