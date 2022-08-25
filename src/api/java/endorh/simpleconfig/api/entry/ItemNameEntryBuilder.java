package endorh.simpleconfig.api.entry;

import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Ingredient;
import org.jetbrains.annotations.Contract;

public interface ItemNameEntryBuilder extends ResourceEntryBuilder<ItemNameEntryBuilder> {
	
	@Contract(pure=true) ItemNameEntryBuilder suggest(Ingredient ingredient);
	
	/**
	 * Restrict the selectable items to those of a tag<br>
	 * This can only be done on server configs, since tags
	 * are server-dependant
	 */
	@Contract(pure=true) ItemNameEntryBuilder suggest(TagKey<Item> tag);
}
