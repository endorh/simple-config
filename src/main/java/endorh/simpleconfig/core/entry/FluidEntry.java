package endorh.simpleconfig.core.entry;

import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import endorh.simpleconfig.api.ConfigEntryHolder;
import endorh.simpleconfig.api.SimpleConfig;
import endorh.simpleconfig.api.entry.FluidEntryBuilder;
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
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
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

import static endorh.simpleconfig.ui.impl.builders.ComboBoxFieldBuilder.ofFluid;

public class FluidEntry extends AbstractConfigEntry<Fluid, String, Fluid>
  implements IKeyEntry<Fluid> {
	protected @NotNull Predicate<Fluid> filter;
	
	@Internal public FluidEntry(
	  ConfigEntryHolder parent, String name,
	  @Nullable Fluid value, Predicate<Fluid> filter
	) {
		super(parent, name, value != null ? value : Fluids.WATER);
		this.filter = filter != null? filter : f -> true;
	}
	
	public static class Builder extends AbstractConfigEntryBuilder<Fluid, String, Fluid, FluidEntry, FluidEntryBuilder, Builder>
	  implements FluidEntryBuilder {
		private static final Logger LOGGER = LogManager.getLogger();
		protected @Nullable Predicate<Fluid> filter = null;
		protected @Nullable TagKey<Fluid> tag = null;
		protected boolean requireGroup = true;
		protected boolean excludeFlowing = true;
		
		public Builder(Fluid value) {
			super(value, Fluid.class);
		}
		
		@Override @Contract(pure=true) public @NotNull Builder setRequireGroup(boolean requireGroup) {
			Builder copy = copy();
			copy.requireGroup = requireGroup;
			return copy;
		}
		
		@Override @Contract(pure=true) public @NotNull Builder setExcludeFlowing(boolean excludeFlowing) {
			Builder copy = copy();
			copy.excludeFlowing = excludeFlowing;
			return copy;
		}
		
		@Override @Contract(pure=true) public @NotNull Builder from(Predicate<Fluid> filter) {
			Builder copy = copy();
			copy.filter = filter;
			return copy;
		}
		
		@Override @Contract(pure=true) public @NotNull Builder from(List<Fluid> choices) {
			List<Fluid> listCopy = new ArrayList<>(choices);
			return from(listCopy::contains);
		}
		
		@Override @Contract(pure=true) public @NotNull Builder from(Fluid... choices) {
			List<Fluid> listCopy = Arrays.asList(choices);
			return from(listCopy::contains);
		}
		
		@Override @Contract(pure=true) public @NotNull Builder from(TagKey<Fluid> tag) {
			Builder copy = copy();
			copy.tag = tag;
			return copy;
		}
		
		@Override protected FluidEntry buildEntry(ConfigEntryHolder parent, String name) {
			if (parent.getRoot().getType() != SimpleConfig.Type.SERVER && tag != null)
				throw new IllegalArgumentException(
				  "Cannot use tag item filters in non-server config entry");
			if (tag != null) {
				//noinspection ConstantConditions
				Predicate<Fluid> inTag = f -> ForgeRegistries.FLUIDS.tags().getTag(tag).contains(f);
				filter = filter != null? filter.and(inTag) : inTag;
			}
			if (filter != null && !filter.test(value))
				LOGGER.warn("Fluid entry's default value doesn't match its filter");
			Predicate<Fluid> filter = this.filter != null ? this.filter : f -> true;
			if (requireGroup) filter = filter.and(f -> f.getBucket().getItemCategory() != null);
			if (excludeFlowing) filter = filter.and(f -> !(f instanceof FlowingFluid) || ((FlowingFluid) f).getSource() == f);
			return new FluidEntry(parent, name, value, filter);
		}
		
		@Override protected Builder createCopy() {
			final Builder copy = new Builder(value);
			copy.filter = filter;
			copy.tag = tag;
			return copy;
		}
	}
	
	@Override public String forConfig(Fluid value) {
		//noinspection ConstantConditions
		return ForgeRegistries.FLUIDS.getKey(value).toString();
	}
	
	@Override @Nullable public Fluid fromConfig(@Nullable String value) {
		if (value == null) return null;
		try {
			final ResourceLocation registryName = new ResourceLocation(value);
			//noinspection deprecation
			final Fluid item = Registry.FLUID.keySet().contains(registryName) ?
			                   Registry.FLUID.get(registryName) : null;
			// Prevent unnecessary config resets adding an exception for the default value
			return filter.test(item) || item == this.defValue? item : null;
		} catch (ResourceLocationException e) {
			return null;
		}
	}
	
	protected List<Fluid> supplyOptions() {
		return ForgeRegistries.FLUIDS.getValues().stream().filter(filter)
		  .collect(Collectors.toList());
	}
	
	@Override public List<String> getConfigCommentTooltips() {
		List<String> tooltips = super.getConfigCommentTooltips();
		tooltips.add("Fluid: namespace:path");
		return tooltips;
	}
	
	@OnlyIn(Dist.CLIENT) @Override public Optional<FieldBuilder<Fluid, ?, ?>> buildGUIEntry(
	  ConfigFieldBuilder builder
	) {
		final ComboBoxFieldBuilder<Fluid> entryBuilder =
		  builder.startComboBox(getDisplayName(), ofFluid(), forGui(get()))
			 .setSuggestionProvider(new SimpleComboBoxModel<>(this::supplyOptions))
			 .setSuggestionMode(false);
		return Optional.of(decorate(entryBuilder));
	}
	
	@Override public boolean addCommandSuggestions(SuggestionsBuilder builder) {
		super.addCommandSuggestions(builder);
		Fluid current = get();
		for (Fluid o: supplyOptions()) if (!o.equals(current) && !o.equals(defValue) && isValidValue(o))
			builder.suggest(forCommand(o));
		return true;
	}
}

