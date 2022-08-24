package endorh.simpleconfig.api.entry;

import net.minecraft.item.Item;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.tags.ITag;
import org.jetbrains.annotations.Contract;

public interface ItemNameEntryBuilder extends ResourceEntryBuilder<ItemNameEntryBuilder> {
	
	@Contract(pure=true) ItemNameEntryBuilder suggest(Ingredient ingredient);
	
	/**
	 * Restrict the selectable items to those of a tag<br>
	 * This can only be done on server configs, since tags
	 * are server-dependant
	 */
	@Contract(pure=true) ItemNameEntryBuilder suggest(ITag<Item> tag);
}
