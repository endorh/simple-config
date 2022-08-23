package endorh.simpleconfig.api.entry;

import endorh.simpleconfig.api.ConfigEntryBuilder;
import endorh.simpleconfig.api.KeyEntryBuilder;
import net.minecraft.tags.Tag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Ingredient;
import org.jetbrains.annotations.Contract;

import java.util.List;
import java.util.function.Predicate;

public interface ItemEntryBuilder
  extends ConfigEntryBuilder<Item, String, Item, ItemEntryBuilder>, KeyEntryBuilder<Item> {
	/**
	 * When true (the default), items without an item group are not accepted.<br>
	 * This excludes the AIR and BARRIER item blocks, as well as other special blocks.
	 */
	@Contract(pure=true) ItemEntryBuilder setRequireGroup(boolean requireGroup);
	
	@Contract(pure=true) ItemEntryBuilder from(Ingredient filter);
	
	@Contract(pure=true) ItemEntryBuilder from(Predicate<Item> filter);
	
	@Contract(pure=true) ItemEntryBuilder from(List<Item> items);
	
	@Contract(pure=true) ItemEntryBuilder from(Item... items);
	
	/**
	 * Restrict the selectable items to those of a tag<br>
	 * This can only be done on server configs, since tags
	 * are server-dependant
	 */
	@Contract(pure=true) ItemEntryBuilder from(Tag<Item> tag);
}
