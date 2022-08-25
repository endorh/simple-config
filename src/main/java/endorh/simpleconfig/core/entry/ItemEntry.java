package endorh.simpleconfig.core.entry;

import endorh.simpleconfig.api.ConfigEntryHolder;
import endorh.simpleconfig.api.SimpleConfig;
import endorh.simpleconfig.api.entry.ItemEntryBuilder;
import endorh.simpleconfig.core.AbstractConfigEntry;
import endorh.simpleconfig.core.AbstractConfigEntryBuilder;
import endorh.simpleconfig.core.IKeyEntry;
import endorh.simpleconfig.ui.api.ConfigFieldBuilder;
import endorh.simpleconfig.ui.gui.widget.combobox.SimpleComboBoxModel;
import endorh.simpleconfig.ui.impl.builders.ComboBoxFieldBuilder;
import endorh.simpleconfig.ui.impl.builders.FieldBuilder;
import net.minecraft.ResourceLocationException;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static endorh.simpleconfig.ui.impl.builders.ComboBoxFieldBuilder.ofItem;

public class ItemEntry extends AbstractConfigEntry<Item, String, Item>
  implements IKeyEntry<Item> {
	protected @NotNull Predicate<Item> filter;
	
	@Internal public ItemEntry(
	  ConfigEntryHolder parent, String name,
	  @Nullable Item value, Predicate<Item> filter
	) {
		super(parent, name, value != null ? value : Items.AIR);
		this.filter = filter != null? filter : i -> true;
	}
	
	public static class Builder extends AbstractConfigEntryBuilder<Item, String, Item, ItemEntry, ItemEntryBuilder, Builder>
	  implements ItemEntryBuilder {
		private static final Logger LOGGER = LogManager.getLogger();
		protected @Nullable Predicate<Item> filter = null;
		protected @Nullable TagKey<Item> tag = null;
		protected boolean requireGroup = true;
		
		public Builder(Item value) {
			super(value, Item.class);
		}
		
		@Override @Contract(pure=true) public Builder setRequireGroup(boolean requireGroup) {
			Builder copy = copy();
			copy.requireGroup = requireGroup;
			return copy;
		}
		
		@Override @Contract(pure=true) public Builder from(Ingredient filter) {
			return from(i -> filter.test(new ItemStack(i)));
		}
		
		@Override @Contract(pure=true) public Builder from(Predicate<Item> filter) {
			Builder copy = copy();
			copy.filter = filter;
			return copy;
		}
		
		@Override @Contract(pure=true) public Builder from(List<Item> items) {
			List<Item> listCopy = new ArrayList<>(items);
			return from(listCopy::contains);
		}
		
		@Override @Contract(pure=true) public Builder from(Item... items) {
			return from(Ingredient.of(items));
		}
		
		@Override @Contract(pure=true) public Builder from(TagKey<Item> tag) {
			Builder copy = copy();
			copy.tag = tag;
			return copy;
		}
		
		@Override protected ItemEntry buildEntry(ConfigEntryHolder parent, String name) {
			if (parent.getRoot().getType() != SimpleConfig.Type.SERVER && tag != null)
				throw new IllegalArgumentException(
				  "Cannot use tag item filters in non-server config entry");
			if (tag != null) {
				//noinspection ConstantConditions
				Predicate<Item> inTag = i -> ForgeRegistries.ITEMS.tags().getTag(tag).contains(i);
				filter = filter != null? filter.and(inTag) : inTag;
			}
			if (filter != null && !filter.test(value))
				LOGGER.warn("Item entry's default value doesn't match its filter");
			Predicate<Item> filter = this.filter != null ? this.filter : i -> true;
			if (requireGroup) filter = filter.and(i -> i.getItemCategory() != null);
			return new ItemEntry(parent, name, value, filter);
		}
		
		@Override protected Builder createCopy() {
			final Builder copy = new Builder(value);
			copy.filter = filter;
			copy.tag = tag;
			copy.requireGroup = requireGroup;
			return copy;
		}
	}
	
	protected List<Item> supplyOptions() {
		return ForgeRegistries.ITEMS.getValues().stream().filter(filter).collect(Collectors.toList());
	}
	
	@Override public String forConfig(Item value) {
		//noinspection ConstantConditions
		return ForgeRegistries.ITEMS.getKey(value).toString();
	}
	
	@Override @Nullable public Item fromConfig(@Nullable String value) {
		if (value == null) return null;
		try {
			final ResourceLocation name = new ResourceLocation(value);
			//noinspection deprecation
			final Item item = Registry.ITEM.keySet().contains(name) ?
			                  Registry.ITEM.get(name) : null;
			// Prevent unnecessary config resets adding the default value as exception
			return filter.test(item) || item == this.defValue? item : null;
		} catch (ResourceLocationException e) {
			return null;
		}
	}
	
	@Override public List<String> getConfigCommentTooltips() {
		List<String> tooltips = super.getConfigCommentTooltips();
		tooltips.add("Item: namespace:path");
		return tooltips;
	}
	
	@OnlyIn(Dist.CLIENT) @Override
	public Optional<FieldBuilder<Item, ?, ?>> buildGUIEntry(ConfigFieldBuilder builder) {
		final ComboBoxFieldBuilder<Item> entryBuilder =
		  builder.startComboBox(getDisplayName(), ofItem(), forGui(get()))
		    .setSuggestionProvider(new SimpleComboBoxModel<>(this::supplyOptions))
			 .setSuggestionMode(false);
		return Optional.of(decorate(entryBuilder));
	}
}
