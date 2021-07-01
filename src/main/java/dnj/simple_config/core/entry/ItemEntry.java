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
import net.minecraftforge.common.ForgeConfigSpec.Builder;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;
import net.minecraftforge.fml.config.ModConfig.Type;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

public class ItemEntry extends AbstractConfigEntry<Item, String, Item, ItemEntry> {
	protected final ItemStack stack;
	protected Ingredient filter = null;
	protected ITag<Item> tag = null;
	protected Set<Item> validItems = null;
	
	public ItemEntry(@Nullable Item value) {
		super(value != null ? value : Items.AIR);
		this.stack = new ItemStack(this.value);
	}
	
	public ItemEntry from(Ingredient filter) {
		this.filter = filter;
		if (filter != null) {
			if (!filter.test(stack))
				throw new IllegalArgumentException(
				  "Filter for item config entry does not match the default value");
			validItems = Arrays
			  .stream(filter.getMatchingStacks()).map(ItemStack::getItem).collect(Collectors.toSet());
		} else validItems = null;
		return this;
	}
	
	public ItemEntry from(Item... items) {
		return from(Ingredient.fromItems(items));
	}
	
	public ItemEntry from(ITag<Item> tag) {
		this.tag = tag;
		return this;
	}
	
	protected Set<Item> getValidItems() {
		if (tag != null) {
			// Tags cannot be used until a world is loaded
			// Until a world is loaded we simply don't apply any restrictions
			try {
				filter = Ingredient.fromTag(tag);
				validItems = Arrays.stream(filter.getMatchingStacks()).map(ItemStack::getItem)
				  .collect(Collectors.toSet());
			} catch (IllegalStateException e) {
				filter = null;
				validItems = null;
			}
		}
		return validItems != null ? validItems : Registry.ITEM.stream().collect(Collectors.toSet());
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
		final ResourceLocation registryName = new ResourceLocation(itemId);
		final Item item = Registry.ITEM.containsKey(registryName) ?
		                  Registry.ITEM.getOrDefault(registryName) : null;
		return getValidItems().contains(item) ? item : null;
	}
	
	@Override
	protected Optional<ConfigValue<?>> buildConfigEntry(Builder builder) {
		if (parent.getRoot().type != Type.SERVER && tag != null)
			throw new IllegalArgumentException(
			  "Cannot use tag item filters in non-server config entry \"" + name + "\"");
		assert value.getRegistryName() != null;
		return Optional.of(decorate(builder).define(
		  name, value.getRegistryName().toString(), s ->
			 s instanceof String && fromId((String) s) != null));
	}
	
	@Override
	protected Optional<AbstractConfigListEntry<?>> buildGUIEntry(
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
