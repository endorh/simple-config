package endorh.simpleconfig.core.entry;

import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import endorh.simpleconfig.api.ConfigEntryHolder;
import endorh.simpleconfig.api.SimpleConfig;
import endorh.simpleconfig.api.entry.BlockEntryBuilder;
import endorh.simpleconfig.core.AbstractConfigEntry;
import endorh.simpleconfig.core.AbstractConfigEntryBuilder;
import endorh.simpleconfig.core.AtomicEntry;
import endorh.simpleconfig.core.EntryType;
import endorh.simpleconfig.ui.api.ConfigFieldBuilder;
import endorh.simpleconfig.ui.gui.widget.combobox.SimpleComboBoxModel;
import endorh.simpleconfig.ui.impl.builders.ComboBoxFieldBuilder;
import endorh.simpleconfig.ui.impl.builders.FieldBuilder;
import net.minecraft.ResourceLocationException;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
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
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static endorh.simpleconfig.ui.impl.builders.ComboBoxFieldBuilder.ofBlock;

public class BlockEntry extends AbstractConfigEntry<Block, String, Block>
  implements AtomicEntry<Block> {
	protected @NotNull Predicate<Block> filter;
	
	@Internal public BlockEntry(
	  ConfigEntryHolder parent, String name,
	  @Nullable Block value, Predicate<Block> filter
	) {
		super(parent, name, value != null ? value : Blocks.AIR);
		this.filter = filter != null? filter : b -> true;
	}
	
	public static class Builder extends AbstractConfigEntryBuilder<
	  Block, String, Block, BlockEntry, BlockEntryBuilder, Builder
	> implements BlockEntryBuilder {
		private static final Logger LOGGER = LogManager.getLogger();
		protected @Nullable Predicate<Block> filter = null;
		protected @Nullable TagKey<Block> tag = null;
		
		public Builder(Block value) {
			super(value, EntryType.of(Block.class));
		}
		
		@Override @Contract(pure=true) public @NotNull Builder setRequireGroup(boolean requireGroup) {
			return copy();
		}
		
		@Override @Contract(pure=true) public @NotNull Builder from(Predicate<Block> filter) {
			Builder copy = copy();
			copy.filter = filter;
			return copy;
		}
		
		@Override @Contract(pure=true) public @NotNull Builder from(List<Block> choices) {
			List<Block> listCopy = new ArrayList<>(choices);
			return from(listCopy::contains);
		}
		
		@Override @Contract(pure=true) public @NotNull Builder from(Block... choices) {
			List<Block> listCopy = Arrays.asList(choices);
			return from(listCopy::contains);
		}
		
		@Override @Contract(pure=true) public @NotNull Builder from(TagKey<Block> tag) {
			Builder copy = copy();
			copy.tag = tag;
			return copy;
		}
		
		@Override protected BlockEntry buildEntry(ConfigEntryHolder parent, String name) {
			if (parent.getRoot().getType() != SimpleConfig.Type.SERVER && tag != null)
				throw new IllegalArgumentException(
				  "Cannot use tag item filters in non-server config entry");
			if (tag != null) {
				//noinspection ConstantConditions
				Predicate<Block> inTag = i -> ForgeRegistries.BLOCKS.tags().getTag(tag).contains(i);
				filter = filter != null? filter.and(inTag) : inTag;
			}
			if (filter != null && !filter.test(value))
				LOGGER.warn("Block entry's default value doesn't match its filter");
			Predicate<Block> filter = this.filter != null ? this.filter : b -> true;
			return new BlockEntry(parent, name, value, filter);
		}
		
		@Override protected Builder createCopy(Block value) {
			final Builder copy = new Builder(value);
			copy.filter = filter;
			copy.tag = tag;
			return copy;
		}
	}
	
	@Override public String forConfig(Block value) {
		//noinspection ConstantConditions
		return ForgeRegistries.BLOCKS.getKey(value).toString();
	}
	
	@Override @Nullable public Block fromConfig(@Nullable String value) {
		if (value == null) return null;
		try {
			final ResourceLocation registryName = new ResourceLocation(value);
			final Block item = ForgeRegistries.BLOCKS.containsKey(registryName) ?
			                   ForgeRegistries.BLOCKS.getValue(registryName) : null;
			// Prevent unnecessary config resets adding an exception for the default value
			return filter.test(item) || item == this.defValue? item : null;
		} catch (ResourceLocationException e) {
			return null;
		}
	}
	
	protected List<Block> supplyOptions() {
		return ForgeRegistries.BLOCKS.getValues().stream().filter(filter)
		  .collect(Collectors.toList());
	}
	
	@Override public List<String> getConfigCommentTooltips() {
		List<String> tooltips = super.getConfigCommentTooltips();
		tooltips.add("Block: namespace:path");
		return tooltips;
	}
	
	@OnlyIn(Dist.CLIENT) @Override public Optional<FieldBuilder<Block, ?, ?>> buildGUIEntry(
	  ConfigFieldBuilder builder
	) {
		final ComboBoxFieldBuilder<Block> entryBuilder =
		  builder.startComboBox(getDisplayName(), ofBlock(), forGui(get()))
		    .setSuggestionProvider(new SimpleComboBoxModel<>(this::supplyOptions))
			 .setSuggestionMode(false);
		return Optional.of(decorate(entryBuilder));
	}
	
	@Override public boolean addCommandSuggestions(SuggestionsBuilder builder) {
		super.addCommandSuggestions(builder);
		Block current = get();
		for (Block o: supplyOptions())
			if (!o.equals(current) && !o.equals(defValue) && isValidValue(o))
				builder.suggest(forCommand(o));
		return true;
	}
}
