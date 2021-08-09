package endorh.simple_config.core.entry;

import endorh.simple_config.core.AbstractConfigEntry;
import endorh.simple_config.core.AbstractConfigEntryBuilder;
import endorh.simple_config.core.ISimpleConfigEntryHolder;
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.impl.builders.DropdownMenuBuilder;
import me.shedaniel.clothconfig2.impl.builders.DropdownMenuBuilder.CellCreatorBuilder;
import me.shedaniel.clothconfig2.impl.builders.DropdownMenuBuilder.TopCellElementBuilder;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.tags.ITag;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.registry.Registry;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;
import net.minecraftforge.fml.config.ModConfig.Type;
import org.jetbrains.annotations.ApiStatus.Internal;

import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class ItemEntry extends AbstractConfigEntry<Item, String, Item, ItemEntry> {
	protected final Predicate<Item> filter;
	
	@Internal public ItemEntry(
	  ISimpleConfigEntryHolder parent, String name,
	  @Nullable Item value, Predicate<Item> filter
	) {
		super(parent, name, value != null ? value : Items.AIR);
		this.filter = filter != null? filter : i -> true;
	}
	
	public static class Builder extends AbstractConfigEntryBuilder<Item, String, Item, ItemEntry, Builder> {
		protected Predicate<Item> filter = null;
		protected ITag<Item> tag = null;
		
		public Builder(Item value) {
			super(value, Item.class);
		}
		
		public Builder from(Ingredient filter) {
			return from(i -> filter.test(new ItemStack(i)));
		}
		
		public Builder from(Predicate<Item> filter) {
			this.filter = filter;
			return this;
		}
		
		public Builder from(Item... items) {
			return from(Ingredient.fromItems(items));
		}
		
		/**
		 * Restrict the selectable items to those of a tag<br>
		 * This can only be done on server configs, since tags
		 * are server-dependant
		 */
		public Builder from(ITag<Item> tag) {
			this.tag = tag;
			return this;
		}
		
		@Override
		protected ItemEntry buildEntry(ISimpleConfigEntryHolder parent, String name) {
			if (parent.getRoot().type != Type.SERVER && tag != null)
				throw new IllegalArgumentException(
				  "Cannot use tag item filters in non-server config entry");
			if (tag != null)
				filter = filter != null ? filter.and(tag::contains) : tag::contains;
			try {
				if (!filter.test(value))
					throw new IllegalArgumentException(
					  "Item entry's default value doesn't match its filter");
			} catch (IllegalStateException e) {
				if (parent.getRoot().type != Type.SERVER)
					throw e;
			}
			return new ItemEntry(parent, name, value, filter);
		}
	}
	
	protected Set<Item> getValidItems() {
		final Predicate<Item> nonNullFilter = filter != null? filter : i -> true;
		//noinspection deprecation
		return Registry.ITEM.getEntries().stream().map(Entry::getValue)
		  .filter(nonNullFilter).collect(Collectors.toSet());
	}
	
	@Override
	protected String forConfig(Item value) {
		//noinspection ConstantConditions
		return value.getRegistryName().toString();
	}
	
	@Override
	protected @Nullable Item fromConfig(@Nullable String value) {
		if (value == null) return null;
		final Item i = fromId(value);
		return i != null ? i : this.value;
	}
	
	protected @Nullable
	Item fromId(String itemId) {
		if (itemId == null || itemId.isEmpty())
			return null;
		final ResourceLocation registryName = new ResourceLocation(itemId);
		//noinspection deprecation
		final Item item = Registry.ITEM.keySet().contains(registryName) ?
		                  Registry.ITEM.getOrDefault(registryName) : null;
		return getValidItems().contains(item) ? item : null;
	}
	
	@Override
	protected Optional<ConfigValue<?>> buildConfigEntry(ForgeConfigSpec.Builder builder) {
		assert value.getRegistryName() != null;
		return Optional.of(decorate(builder).define(
		  name, value.getRegistryName().toString(), configValidator()));
	}
	
	@OnlyIn(Dist.CLIENT)
	@Override
	public Optional<AbstractConfigListEntry<Item>> buildGUIEntry(
	  ConfigEntryBuilder builder
	) {
		final DropdownMenuBuilder<Item> valBuilder = builder
		  .startDropdownMenu(
			 getDisplayName(), TopCellElementBuilder.ofItemObject(get()),
			 CellCreatorBuilder.ofItemObject())
		  .setDefaultValue(value)
		  .setSelections(
			 getValidItems().stream().sorted(
				Comparator.comparing(Item::toString)
			 ).collect(Collectors.toCollection(LinkedHashSet::new)))
		  .setSaveConsumer(saveConsumer())
		  .setTooltipSupplier(this::supplyTooltip)
		  .setErrorSupplier(this::supplyError);
		return Optional.of(decorate(valBuilder).build());
	}
}
