package dnj.simple_config.core.entry;

import dnj.simple_config.core.AbstractConfigEntry;
import dnj.simple_config.core.ISimpleConfigEntryHolder;
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
import net.minecraftforge.common.ForgeConfigSpec.Builder;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;
import net.minecraftforge.fml.config.ModConfig.Type;

import javax.annotation.Nullable;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class ItemEntry extends AbstractConfigEntry<Item, String, Item, ItemEntry> {
	protected Predicate<Item> filter = null;
	protected boolean usesTag = false;
	
	public ItemEntry(@Nullable Item value) {
		super(value != null ? value : Items.AIR, Item.class);
	}
	
	public ItemEntry from(Ingredient filter) {
		return from(i -> filter.test(new ItemStack(i)));
	}
	
	public ItemEntry from(Predicate<Item> filter) {
		this.filter = filter;
		if (!usesTag && filter != null) {
			if (!filter.test(value))
				throw new IllegalArgumentException(
				  "Filter for item config entry does not match the default value");
		}
		return this;
	}
	
	public ItemEntry from(Item... items) {
		return from(Ingredient.fromItems(items));
	}
	
	/**
	 * Restrict the selectable items to those of a tag<br>
	 * This can only be done on server configs, since tags
	 * are server-dependant
	 */
	public ItemEntry from(ITag<Item> tag) {
		this.usesTag = true;
		return from(tag::contains);
	}
	
	protected Set<Item> getValidItems() {
		final Predicate<Item> nonNullFilter = filter != null? filter : i -> true;
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
		final Item item = Registry.ITEM.keySet().contains(registryName) ?
		                  Registry.ITEM.getOrDefault(registryName) : null;
		return getValidItems().contains(item) ? item : null;
	}
	
	@Override
	protected Optional<ConfigValue<?>> buildConfigEntry(Builder builder) {
		if (parent.getRoot().type != Type.SERVER && usesTag)
			throw new IllegalArgumentException(
			  "Cannot use tag item filters in non-server config entry \"" + getPath() + "\"");
		assert value.getRegistryName() != null;
		return Optional.of(decorate(builder).define(
		  name, value.getRegistryName().toString(), configValidator()));
	}
	
	@OnlyIn(Dist.CLIENT)
	@Override
	protected Optional<AbstractConfigListEntry<Item>> buildGUIEntry(
	  ConfigEntryBuilder builder, ISimpleConfigEntryHolder c
	) {
		final DropdownMenuBuilder<Item> valBuilder = builder
		  .startDropdownMenu(
			 getDisplayName(), TopCellElementBuilder.ofItemObject(c.get(name)),
			 CellCreatorBuilder.ofItemObject())
		  .setDefaultValue(value)
		  .setSelections(
			 getValidItems().stream().sorted(
				Comparator.comparing(Item::toString)
			 ).collect(Collectors.toCollection(LinkedHashSet::new)))
		  .setSaveConsumer(saveConsumer(c))
		  .setTooltipSupplier(this::supplyTooltip)
		  .setErrorSupplier(this::supplyError);
		return Optional.of(decorate(valBuilder).build());
	}
}
