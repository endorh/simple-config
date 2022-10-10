package endorh.simpleconfig.api.entry;

import endorh.simpleconfig.api.ConfigEntryBuilder;
import endorh.simpleconfig.api.KeyEntryBuilder;
import net.minecraft.item.Item;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.tags.ITag;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Predicate;

public interface ItemEntryBuilder
  extends ConfigEntryBuilder<Item, String, Item, ItemEntryBuilder>, KeyEntryBuilder<Item> {
	/**
	 * When true (the default), items without an item group are not accepted.<br>
	 * This excludes the AIR and BARRIER item blocks, as well as other special blocks.
	 */
	@Contract(pure=true) @NotNull ItemEntryBuilder setRequireGroup(boolean requireGroup);
	
	@Contract(pure=true) @NotNull ItemEntryBuilder from(Ingredient filter);
	
	@Contract(pure=true) @NotNull ItemEntryBuilder from(Predicate<Item> filter);
	
	@Contract(pure=true) @NotNull ItemEntryBuilder from(List<Item> items);
	
	@Contract(pure=true) @NotNull ItemEntryBuilder from(Item... items);
	
	/**
	 * Restrict the selectable items to those of a tag<br>
	 * This can only be done on server configs, since tags
	 * are server-dependant
	 */
	@Contract(pure=true) @NotNull ItemEntryBuilder from(ITag<Item> tag);
}
