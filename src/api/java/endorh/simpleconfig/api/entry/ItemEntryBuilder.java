package endorh.simpleconfig.api.entry;

import endorh.simpleconfig.api.AtomicEntryBuilder;
import endorh.simpleconfig.api.ConfigEntryBuilder;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Ingredient;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Predicate;

public interface ItemEntryBuilder
  extends ConfigEntryBuilder<@NotNull Item, String, Item, ItemEntryBuilder>, AtomicEntryBuilder {
	/**
	 * When true (the default), items without an item group are not accepted.<br>
	 * This excludes the AIR and BARRIER item blocks, as well as other special blocks.
	 *
	 * @deprecated Ineffective in 1.19.3+
	 */
	@Deprecated
	@Contract(pure=true) @NotNull ItemEntryBuilder setRequireGroup(boolean requireGroup);
	
	/**
	 * Restrict selectable items with an ingredient predicate.
	 */
	@Contract(pure=true) @NotNull ItemEntryBuilder from(Ingredient filter);
	
	/**
	 * Restrict selectable items with an arbitrary predicate.
	 */
	@Contract(pure=true) @NotNull ItemEntryBuilder from(Predicate<Item> filter);
	
	/**
	 * Restrict selectable items to the given choices.
	 */
	@Contract(pure=true) @NotNull ItemEntryBuilder from(List<Item> items);
	
	/**
	 * Restrict selectable items to the given choices.
	 */
	@Contract(pure=true) @NotNull ItemEntryBuilder from(Item... items);
	
	/**
	 * Restrict the selectable items to those of a tag.<br>
	 * This can only be done on server configs, since tags
	 * are server-dependant.<br>
	 * This filter can be combined with other filters.
	 */
	@Contract(pure=true) @NotNull ItemEntryBuilder from(TagKey<Item> tag);
}
