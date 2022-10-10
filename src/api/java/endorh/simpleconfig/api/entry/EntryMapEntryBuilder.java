package endorh.simpleconfig.api.entry;

import endorh.simpleconfig.api.ConfigEntryBuilder;
import endorh.simpleconfig.api.KeyEntryBuilder;
import net.minecraft.util.text.ITextComponent;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;

public interface EntryMapEntryBuilder<
  K, V, KC, C, KG, G,
  B extends ConfigEntryBuilder<V, C, G, B>,
  KB extends ConfigEntryBuilder<K, KC, KG, KB> & KeyEntryBuilder<KG>
  > extends ConfigEntryBuilder<Map<K, V>, Map<KC, C>, List<Pair<KG, G>>, EntryMapEntryBuilder<K, V, KC, C, KG, G, B, KB>> {
	/**
	 * Display this entry expanded in the GUI by default.
	 *
	 * @see #expand(boolean)
	 */
	@Contract(pure=true) @NotNull EntryMapEntryBuilder<K, V, KC, C, KG, G, B, KB> expand();
	
	/**
	 * Display this entry expanded in the GUI by default.
	 *
	 * @param expand Whether to expand this entry by default.
	 * @see #expand()
	 */
	@Contract(pure=true) @NotNull EntryMapEntryBuilder<K, V, KC, C, KG, G, B, KB> expand(boolean expand);
	
	/**
	 * Use a linked map to preserve the order of the entries.<br>
	 * In the config file it will be represented as a YAML ordered map.<br>
	 * <i>Note that if you manually set the value of this entry to a non-linked map
	 * the order will be lost.</i>
	 *
	 * @see #linked(boolean)
	 */
	@Contract(pure=true) @NotNull EntryMapEntryBuilder<K, V, KC, C, KG, G, B, KB> linked();
	
	/**
	 * Use a linked map to preserve the order of the entries.<br>
	 * In the config file it will be represented as a YAML ordered map.<br>
	 * <i>Note that if you manually set the value of this entry to a non-linked map
	 * the order will be lost.</i>
	 *
	 * @param linked Whether to use a linked map.
	 * @see #linked()
	 */
	@Contract(pure=true) @NotNull EntryMapEntryBuilder<K, V, KC, C, KG, G, B, KB> linked(boolean linked);
	
	/**
	 * Set an error supplier for each entry instead of the whole map.<br>
	 * The map will be deemed invalid if a single entry is invalid.
	 *
	 * @param supplier The supplier for the error.
	 */
	@Contract(pure=true) @NotNull EntryMapEntryBuilder<K, V, KC, C, KG, G, B, KB> entryError(
	  BiFunction<K, V, Optional<ITextComponent>> supplier
	);
	
	/**
	 * Set the minimum (inclusive) size of the map.
	 *
	 * @param minSize The inclusive minimum size of the map.
	 */
	@Contract(pure=true) @NotNull EntryMapEntryBuilder<K, V, KC, C, KG, G, B, KB> minSize(int minSize);
	
	/**
	 * Set the maximum (inclusive) size of the map.
	 *
	 * @param maxSize The inclusive maximum size of the map.
	 */
	@Contract(pure=true) @NotNull EntryMapEntryBuilder<K, V, KC, C, KG, G, B, KB> maxSize(int maxSize);
}
