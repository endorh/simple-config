package endorh.simpleconfig.api.entry;

import endorh.simpleconfig.api.ConfigEntryBuilder;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public interface CollectionEntryBuilder<
  V, C, G, Self extends ConfigEntryBuilder<V, C, G, Self> & CollectionEntryBuilder<V, C, G, Self>
> extends ConfigEntryBuilder<V, C, G, Self> {
	/**
	 * Expand this entry by default in the config menu.
	 */
	@Contract(pure=true) @NotNull Self expand();
	
	/**
	 * Configure whether this entry should appear expanded by default in the config menu.
	 */
	@Contract(pure=true) @NotNull Self expand(boolean expand);
	
	/**
	 * Set the minimum (inclusive) allowed list size.
	 *
	 * @param minSize Inclusive minimum size
	 */
	@Contract(pure=true) @NotNull Self minSize(int minSize);
	
	/**
	 * Set the maximum (inclusive) allowed list size.
	 *
	 * @param maxSize Inclusive maximum size
	 */
	@Contract(pure=true) @NotNull Self maxSize(int maxSize);
}
