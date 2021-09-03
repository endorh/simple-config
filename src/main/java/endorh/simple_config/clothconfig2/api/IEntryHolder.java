package endorh.simple_config.clothconfig2.api;

import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public interface IEntryHolder {
	List<AbstractConfigEntry<?>> getEntries();
	
	Pattern DOT = Pattern.compile("\\.");
	default @Nullable AbstractConfigEntry<?> getEntry(String path) {
		final String[] sp = DOT.split(path, 2);
		final List<AbstractConfigEntry<?>> entries = getEntries();
		final AbstractConfigEntry<?> entry = entries.stream()
		  .filter(e -> e.getName().equals(sp[0])).findFirst().orElse(null);
		if (sp.length < 2) return entry;
		if (entry instanceof IEntryHolder)
			return ((IEntryHolder) entry).getEntry(sp[1]);
		return entry;
	}
	
	/**
	 * Get all the entries recursively
	 */
	default List<AbstractConfigEntry<?>> getAllEntries() {
		final List<AbstractConfigEntry<?>> entries = getEntries();
		entries.addAll(entries.stream().filter(e -> e instanceof IEntryHolder)
		  .flatMap(e -> ((IEntryHolder) e).getAllEntries().stream())
		  .collect(Collectors.toList()));
		return entries;
	}
}
