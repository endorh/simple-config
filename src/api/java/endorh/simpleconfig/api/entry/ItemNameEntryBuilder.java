package endorh.simpleconfig.api.entry;

import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Ingredient;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public interface ItemNameEntryBuilder extends ResourceEntryBuilder<ItemNameEntryBuilder> {
	@Contract(pure=true) @NotNull ItemNameEntryBuilder suggest(Ingredient ingredient);
	
	/**
	 * Suggest selectable items from a tag<br>
	 * This can only be done on server configs, since tags
	 * are server-dependant
	 */
	@Contract(pure=true) @NotNull ItemNameEntryBuilder suggest(TagKey<Item> tag);
}
