package endorh.simpleconfig.core.entry;

import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import endorh.simpleconfig.api.ConfigEntryHolder;
import endorh.simpleconfig.api.SimpleConfig;
import endorh.simpleconfig.api.entry.ItemNameEntryBuilder;
import endorh.simpleconfig.ui.api.ConfigFieldBuilder;
import endorh.simpleconfig.ui.impl.builders.ComboBoxFieldBuilder;
import endorh.simpleconfig.ui.impl.builders.FieldBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static endorh.simpleconfig.ui.impl.builders.ComboBoxFieldBuilder.ofItemName;

public class ItemNameEntry extends AbstractResourceEntry<ItemNameEntry> {
	
	@Internal public ItemNameEntry(
	  ConfigEntryHolder parent, String name,
	  @Nullable ResourceLocation value
	) {
		super(parent, name, value != null ? value : new ResourceLocation(""));
	}
	
	@Override protected @Nullable String getTypeComment() {
		return "Item";
	}
	
	public static class Builder extends AbstractResourceEntry.Builder<ItemNameEntry, ItemNameEntryBuilder, Builder>
	  implements ItemNameEntryBuilder {
		protected TagKey<Item> tag = null;
		
		public Builder(ResourceLocation value) {
			super(value, ResourceLocation.class);
			suggestionSupplier = () -> ForgeRegistries.ITEMS.getValues().stream()
			  .filter(i -> i.getItemCategory() != null).map(ForgeRegistries.ITEMS::getKey)
			  .filter(Objects::nonNull)
			  .collect(Collectors.toList());;
		}
		
		@Override @Contract(pure=true) public @NotNull Builder suggest(Ingredient ingredient) {
			Builder copy = copy();
			copy.suggestionSupplier =
			  () -> Arrays.stream(ingredient.getItems()).map(
			    s -> ForgeRegistries.ITEMS.getKey(s.getItem())
			  ).filter(Objects::nonNull).collect(Collectors.toList());
			return copy;
		}
		
		@Override @Contract(pure=true) public @NotNull Builder suggest(TagKey<Item> tag) {
			Builder copy = copy();
			copy.tag = tag;
			return copy;
		}
		
		@Override
		protected ItemNameEntry buildEntry(ConfigEntryHolder parent, String name) {
			if (parent.getRoot().getType() != SimpleConfig.Type.SERVER && tag != null)
				throw new IllegalArgumentException("Cannot use tag item filters in non-server config entry");
			if (tag != null) {
				final Supplier<List<ResourceLocation>> supplier = suggestionSupplier;
				//noinspection ConstantConditions
				suggestionSupplier = () -> Stream.concat(
				  supplier != null? supplier.get().stream() : Stream.empty(),
				  ForgeRegistries.ITEMS.tags().getTag(tag).stream().map(ForgeRegistries.ITEMS::getKey)
				).collect(Collectors.toList());
			}
			return new ItemNameEntry(parent, name, value);
		}
		
		@Override protected Builder createCopy() {
			final Builder copy = new Builder(value);
			copy.tag = tag;
			return copy;
		}
	}
	
	@OnlyIn(Dist.CLIENT) @Override
	public Optional<FieldBuilder<ResourceLocation, ?, ?>> buildGUIEntry(
	  ConfigFieldBuilder builder
	) {
		final ComboBoxFieldBuilder<ResourceLocation> entryBuilder =
		  builder.startComboBox(getDisplayName(), ofItemName(), forGui(get()));
		return Optional.of(decorate(entryBuilder));
	}
	
	@Override public boolean addCommandSuggestions(SuggestionsBuilder builder) {
		super.addCommandSuggestions(builder);
		ResourceLocation current = get();
		for (ResourceLocation o: ForgeRegistries.ITEMS.getKeys())
			if (!o.equals(current) && !o.equals(defValue) && isValidValue(o))
				builder.suggest(forCommand(o));
		return true;
	}
}
