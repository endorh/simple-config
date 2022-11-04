package endorh.simpleconfig.api.entry;

import endorh.simpleconfig.api.ConfigEntryBuilder;
import endorh.simpleconfig.api.ErrorEntryBuilder;
import net.minecraft.util.text.ITextComponent;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

public interface EntrySetEntryBuilder<
  V, C, G, B extends ConfigEntryBuilder<V, C, G, B>
> extends ConfigEntryBuilder<@NotNull Set<V>, Set<C>, List<G>, EntrySetEntryBuilder<V, C, G, B>> {
	/**
	 * Expand this entry by default in the config menu.
	 */
	@Contract(pure=true) @NotNull EntrySetEntryBuilder<V, C, G, B> expand();
	
	/**
	 * Configure whether this entry should appear expanded by default in the config menu.
	 */
	@Contract(pure=true) @NotNull EntrySetEntryBuilder<V, C, G, B> expand(boolean expand);
	
	/**
	 * Set the minimum (inclusive) allowed set size.
	 *
	 * @param minSize Inclusive minimum size
	 */
	@Contract(pure=true) @NotNull EntrySetEntryBuilder<V, C, G, B> minSize(int minSize);
	
	/**
	 * Set the maximum (inclusive) allowed set size.
	 *
	 * @param maxSize Inclusive maximum size
	 */
	@Contract(pure=true) @NotNull EntrySetEntryBuilder<V, C, G, B> maxSize(int maxSize);
	
	/**
	 * Set an error message supplier for the elements of this set entry<br>
	 * You may also use {@link ErrorEntryBuilder#error(Function)} to check
	 * instead the whole set<br>
	 * If a single element is deemed invalid, the whole set is considered invalid.
	 *
	 * @param errorSupplier Error message supplier. Empty return values indicate
	 *   correct values
	 */
	@Contract(pure=true) @NotNull EntrySetEntryBuilder<V, C, G, B> elemError(
	  Function<V, Optional<ITextComponent>> errorSupplier
	);
}
