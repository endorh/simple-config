package endorh.simpleconfig.core.entry;

import endorh.simpleconfig.ui.api.AbstractConfigListEntry;
import endorh.simpleconfig.ui.api.ConfigEntryBuilder;
import endorh.simpleconfig.ui.gui.widget.combobox.SimpleComboBoxModel;
import endorh.simpleconfig.ui.impl.builders.ComboBoxFieldBuilder;
import endorh.simpleconfig.core.AbstractConfigEntry;
import endorh.simpleconfig.core.AbstractConfigEntryBuilder;
import endorh.simpleconfig.core.ISimpleConfigEntryHolder;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
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
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static endorh.simpleconfig.ui.impl.builders.ComboBoxFieldBuilder.ofBlock;

public class BlockEntry extends AbstractConfigEntry<Block, String, Block, BlockEntry> {
	protected @NotNull Predicate<Block> filter;
	
	@Internal public BlockEntry(
	  ISimpleConfigEntryHolder parent, String name,
	  @Nullable Block value, Predicate<Block> filter
	) {
		super(parent, name, value != null ? value : Blocks.AIR);
		this.filter = filter != null? filter : b -> true;
	}
	
	public static class Builder extends AbstractConfigEntryBuilder<Block, String, Block, BlockEntry, Builder> {
		private static final Logger LOGGER = LogManager.getLogger();
		protected @Nullable Predicate<Block> filter = null;
		protected @Nullable ITag<Block> tag = null;
		protected boolean requireGroup = true;
		
		public Builder(Block value) {
			super(value, Block.class);
		}
		
		/**
		 * When true (the default), requires the block item to have a group.<br>
		 * This excludes the AIR and BARRIER blocks, as well as other special blocks.
		 */
		public Builder setRequireGroup(boolean requireGroup) {
			Builder copy = copy();
			copy.requireGroup = requireGroup;
			return copy;
		}
		
		public Builder from(Predicate<Block> filter) {
			Builder copy = copy();
			copy.filter = filter;
			return copy;
		}
		
		public Builder from(List<Block> choices) {
			List<Block> listCopy = new ArrayList<>(choices);
			return from(listCopy::contains);
		}
		
		public Builder from(Block... choices) {
			List<Block> listCopy = Arrays.asList(choices);
			return from(listCopy::contains);
		}
		
		/**
		 * Restrict the selectable items to those of a tag<br>
		 * This can only be done on server configs, since tags
		 * are server-dependant
		 */
		public Builder from(ITag<Block> tag) {
			Builder copy = copy();
			copy.tag = tag;
			return copy;
		}
		
		@Override protected BlockEntry buildEntry(ISimpleConfigEntryHolder parent, String name) {
			if (parent.getRoot().type != Type.SERVER && tag != null)
				throw new IllegalArgumentException(
				  "Cannot use tag item filters in non-server config entry");
			if (tag != null)
				filter = filter != null? filter.and(tag::contains) : tag::contains;
			if (filter != null && !filter.test(value))
				LOGGER.warn("Block entry's default value doesn't match its filter");
			Predicate<Block> filter = this.filter != null ? this.filter : b -> true;
			if (requireGroup) filter = filter.and(b -> b.asItem().getGroup() != null);
			return new BlockEntry(parent, name, value, filter);
		}
		
		@Override protected Builder createCopy() {
			final Builder copy = new Builder(value);
			copy.filter = filter;
			copy.tag = tag;
			copy.requireGroup = requireGroup;
			return copy;
		}
	}
	
	@Override public String forConfig(Block value) {
		//noinspection ConstantConditions
		return value.getRegistryName().toString();
	}
	
	@Override @Nullable public Block fromConfig(@Nullable String value) {
		if (value == null) return null;
		try {
			final ResourceLocation registryName = new ResourceLocation(value);
			//noinspection deprecation
			final Block item = Registry.BLOCK.keySet().contains(registryName) ?
			                   Registry.BLOCK.getOrDefault(registryName) : null;
			// Prevent unnecessary config resets adding an exception for the default value
			return filter.test(item) || item == this.value ? item : null;
		} catch (ResourceLocationException e) {
			return null;
		}
	}
	
	protected List<Block> supplyOptions() {
		return Registry.BLOCK.getEntries().stream().map(Entry::getValue).filter(filter)
		  .collect(Collectors.toList());
	}
	
	@Override
	protected Optional<ConfigValue<?>> buildConfigEntry(ForgeConfigSpec.Builder builder) {
		assert value.getRegistryName() != null;
		return Optional.of(decorate(builder).define(
		  name, value.getRegistryName().toString(), createConfigValidator()));
	}
	
	@OnlyIn(Dist.CLIENT) @Override public Optional<AbstractConfigListEntry<Block>> buildGUIEntry(
	  ConfigEntryBuilder builder
	) {
		final ComboBoxFieldBuilder<Block> entryBuilder =
		  builder.startComboBox(getDisplayName(), ofBlock(), forGui(get()))
		    .setSuggestionProvider(new SimpleComboBoxModel<>(this::supplyOptions))
			 .setSuggestionMode(false);
		return Optional.of(decorate(entryBuilder).build());
	}
}
