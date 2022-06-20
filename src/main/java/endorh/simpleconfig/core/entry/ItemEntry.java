package endorh.simpleconfig.core.entry;

import endorh.simpleconfig.clothconfig2.api.AbstractConfigListEntry;
import endorh.simpleconfig.clothconfig2.api.ConfigEntryBuilder;
import endorh.simpleconfig.clothconfig2.gui.widget.ComboBoxWidget.SimpleSortedSuggestionProvider;
import endorh.simpleconfig.clothconfig2.impl.builders.ComboBoxFieldBuilder;
import endorh.simpleconfig.core.AbstractConfigEntry;
import endorh.simpleconfig.core.AbstractConfigEntryBuilder;
import endorh.simpleconfig.core.ISimpleConfigEntryHolder;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.tags.ITag;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.ResourceLocationException;
import net.minecraft.util.registry.Registry;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;
import net.minecraftforge.fml.config.ModConfig.Type;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static endorh.simpleconfig.clothconfig2.impl.builders.ComboBoxFieldBuilder.ofItem;

public class ItemEntry extends AbstractConfigEntry<Item, String, Item, ItemEntry> {
	protected @NotNull Predicate<Item> filter;
	
	@Internal public ItemEntry(
	  ISimpleConfigEntryHolder parent, String name,
	  @Nullable Item value, Predicate<Item> filter
	) {
		super(parent, name, value != null ? value : Items.AIR);
		this.filter = filter != null? filter : i -> true;
	}
	
	public static class Builder extends AbstractConfigEntryBuilder<Item, String, Item, ItemEntry, Builder> {
		private static final Logger LOGGER = LogManager.getLogger();
		protected @Nullable Predicate<Item> filter = null;
		protected @Nullable ITag<Item> tag = null;
		protected boolean requireGroup = true;
		
		public Builder(Item value) {
			super(value, Item.class);
		}
		
		/**
		 * When true (the default), items without an item group are not accepted.<br>
		 * This excludes the AIR and BARRIER item blocks, as well as other special blocks.
		 */
		public Builder setRequireGroup(boolean requireGroup) {
			Builder copy = copy();
			copy.requireGroup = requireGroup;
			return copy;
		}
		
		public Builder from(Ingredient filter) {
			return from(i -> filter.test(new ItemStack(i)));
		}
		
		public Builder from(Predicate<Item> filter) {
			Builder copy = copy();
			copy.filter = filter;
			return copy;
		}
		
		public Builder from(List<Item> items) {
			List<Item> listCopy = new ArrayList<>(items);
			return from(listCopy::contains);
		}
		
		public Builder from(Item... items) {
			return from(Ingredient.of(items));
		}
		
		/**
		 * Restrict the selectable items to those of a tag<br>
		 * This can only be done on server configs, since tags
		 * are server-dependant
		 */
		public Builder from(ITag<Item> tag) {
			Builder copy = copy();
			copy.tag = tag;
			return copy;
		}
		
		@Override protected ItemEntry buildEntry(ISimpleConfigEntryHolder parent, String name) {
			if (parent.getRoot().type != Type.SERVER && tag != null)
				throw new IllegalArgumentException(
				  "Cannot use tag item filters in non-server config entry");
			if (tag != null)
				filter = filter != null ? filter.and(tag::contains) : tag::contains;
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
		return Registry.ITEM.entrySet().stream().map(Entry::getValue).filter(filter)
		  .collect(Collectors.toList());
	}
	
	@Override public String forConfig(Item value) {
		//noinspection ConstantConditions
		return value.getRegistryName().toString();
	}
	
	@Override @Nullable public Item fromConfig(@Nullable String value) {
		if (value == null) return null;
		try {
			final ResourceLocation name = new ResourceLocation(value);
			//noinspection deprecation
			final Item item = Registry.ITEM.keySet().contains(name) ?
			                  Registry.ITEM.get(name) : null;
			// Prevent unnecessary config resets adding the default value as exception
			return filter.test(item) || item == this.value ? item : null;
		} catch (ResourceLocationException e) {
			return null;
		}
	}
	
	@Override
	protected Optional<ConfigValue<?>> buildConfigEntry(ForgeConfigSpec.Builder builder) {
		assert value.getRegistryName() != null;
		return Optional.of(decorate(builder).define(
		  name, value.getRegistryName().toString(), configValidator()));
	}
	
	@OnlyIn(Dist.CLIENT) @Override
	public Optional<AbstractConfigListEntry<Item>> buildGUIEntry(ConfigEntryBuilder builder) {
		final ComboBoxFieldBuilder<Item> entryBuilder =
		  builder.startComboBox(getDisplayName(), ofItem(), forGui(get()))
		    .setSuggestionProvider(new SimpleSortedSuggestionProvider<>(this::supplyOptions))
			 .setSuggestionMode(false);
		return Optional.of(decorate(entryBuilder).build());
	}
}
