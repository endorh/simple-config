package endorh.simpleconfig.core.entry;

import endorh.simpleconfig.api.ISimpleConfig;
import endorh.simpleconfig.api.ISimpleConfigEntryHolder;
import endorh.simpleconfig.api.entry.ItemNameEntryBuilder;
import endorh.simpleconfig.ui.api.ConfigFieldBuilder;
import endorh.simpleconfig.ui.impl.builders.ComboBoxFieldBuilder;
import endorh.simpleconfig.ui.impl.builders.FieldBuilder;
import net.minecraft.item.Item;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.tags.ITag;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.registry.Registry;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.registries.ForgeRegistryEntry;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static endorh.simpleconfig.ui.impl.builders.ComboBoxFieldBuilder.ofItemName;

public class ItemNameEntry extends AbstractResourceEntry<ItemNameEntry> {
	
	@Internal public ItemNameEntry(
	  ISimpleConfigEntryHolder parent, String name,
	  @Nullable ResourceLocation value
	) {
		super(parent, name, value != null ? value : new ResourceLocation(""));
	}
	
	@Override protected @Nullable String getTypeComment() {
		return "Item";
	}
	
	public static class Builder extends AbstractResourceEntry.Builder<ItemNameEntry, ItemNameEntryBuilder, Builder>
	  implements ItemNameEntryBuilder {
		protected ITag<Item> tag = null;
		
		public Builder(ResourceLocation value) {
			super(value, ResourceLocation.class);
			suggestionSupplier = () -> Registry.ITEM.getEntries().stream().map(Entry::getValue)
			  .filter(i -> i.getGroup() != null).map(ForgeRegistryEntry::getRegistryName)
			  .collect(Collectors.toList());
		}
		
		@Override @Contract(pure=true) public Builder suggest(Ingredient ingredient) {
			Builder copy = copy();
			copy.suggestionSupplier =
			  () -> Arrays.stream(ingredient.getMatchingStacks()).map(
			    s -> s.getItem().getRegistryName()
			  ).filter(Objects::nonNull).collect(Collectors.toList());
			return copy;
		}
		
		@Override @Contract(pure=true) public Builder suggest(ITag<Item> tag) {
			Builder copy = copy();
			copy.tag = tag;
			return copy;
		}
		
		@Override
		protected ItemNameEntry buildEntry(ISimpleConfigEntryHolder parent, String name) {
			if (parent.getRoot().getType() != ISimpleConfig.Type.SERVER && tag != null)
				throw new IllegalArgumentException("Cannot use tag item filters in non-server config entry");
			if (tag != null) {
				final Supplier<List<ResourceLocation>> supplier = suggestionSupplier;
				suggestionSupplier = () -> Stream.concat(
				  supplier != null? supplier.get().stream() : Stream.empty(),
				  tag.getAllElements().stream().map(ForgeRegistryEntry::getRegistryName)
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
}
