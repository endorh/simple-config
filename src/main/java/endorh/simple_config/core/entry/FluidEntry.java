package endorh.simple_config.core.entry;

import endorh.simple_config.clothconfig2.api.AbstractConfigListEntry;
import endorh.simple_config.clothconfig2.api.ConfigEntryBuilder;
import endorh.simple_config.clothconfig2.impl.builders.ComboBoxFieldBuilder;
import endorh.simple_config.core.AbstractConfigEntry;
import endorh.simple_config.core.AbstractConfigEntryBuilder;
import endorh.simple_config.core.ISimpleConfigEntryHolder;
import endorh.simple_config.core.entry.StringEntry.SupplierSuggestionProvider;
import net.minecraft.fluid.FlowingFluid;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.Fluids;
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

import static endorh.simple_config.clothconfig2.impl.builders.ComboBoxFieldBuilder.ofFluid;

public class FluidEntry extends AbstractConfigEntry<Fluid, String, Fluid, FluidEntry> {
	protected @NotNull Predicate<Fluid> filter;
	
	@Internal public FluidEntry(
	  ISimpleConfigEntryHolder parent, String name,
	  @Nullable Fluid value, Predicate<Fluid> filter
	) {
		super(parent, name, value != null ? value : Fluids.WATER);
		this.filter = filter != null? filter : f -> true;
	}
	
	public static class Builder extends AbstractConfigEntryBuilder<Fluid, String, Fluid, FluidEntry, Builder> {
		private static final Logger LOGGER = LogManager.getLogger();
		protected @Nullable Predicate<Fluid> filter = null;
		protected @Nullable ITag<Fluid> tag = null;
		protected boolean requireGroup = true;
		protected boolean excludeFlowing = true;
		
		public Builder(Fluid value) {
			super(value, Fluid.class);
		}
		
		/**
		 * When true (the default), requires the filled bucket item to have an item group.<br>
		 * This excludes the empty fluid.
		 */
		public Builder setRequireGroup(boolean requireGroup) {
			Builder copy = copy();
			copy.requireGroup = requireGroup;
			return copy;
		}
		
		/**
		 * When true (the default), excludes the flowing variants of fluids, that is,
		 * FlowingFluids whose still fluids are different from themselves.<br>
		 * This excludes FLOWING_WATER and FLOWING_LAVA.
		 */
		public Builder setExcludeFlowing(boolean excludeFlowing) {
			Builder copy = copy();
			copy.excludeFlowing = excludeFlowing;
			return copy;
		}
		
		public Builder from(Predicate<Fluid> filter) {
			Builder copy = copy();
			copy.filter = filter;
			return copy;
		}
		
		public Builder from(List<Fluid> choices) {
			List<Fluid> listCopy = new ArrayList<>(choices);
			return from(listCopy::contains);
		}
		
		public Builder from(Fluid... choices) {
			List<Fluid> listCopy = Arrays.asList(choices);
			return from(listCopy::contains);
		}
		
		/**
		 * Restrict the selectable items to those of a tag<br>
		 * This can only be done on server configs, since tags
		 * are server-dependant
		 */
		public Builder from(ITag<Fluid> tag) {
			Builder copy = copy();
			copy.tag = tag;
			return copy;
		}
		
		@Override protected FluidEntry buildEntry(ISimpleConfigEntryHolder parent, String name) {
			if (parent.getRoot().type != Type.SERVER && tag != null)
				throw new IllegalArgumentException(
				  "Cannot use tag item filters in non-server config entry");
			if (tag != null)
				filter = tag::contains;
			if (filter != null && !filter.test(value))
				LOGGER.warn("Fluid entry's default value doesn't match its filter");
			Predicate<Fluid> filter = this.filter != null ? this.filter : f -> true;
			if (requireGroup) filter = filter.and(f -> f.getFilledBucket().getGroup() != null);
			if (excludeFlowing) filter = filter.and(f -> !(f instanceof FlowingFluid) || ((FlowingFluid) f).getStillFluid() == f);
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
		return value.getRegistryName().toString();
	}
	
	@Override @Nullable public Fluid fromConfig(@Nullable String value) {
		if (value == null) return null;
		try {
			final ResourceLocation registryName = new ResourceLocation(value);
			//noinspection deprecation
			final Fluid item = Registry.FLUID.keySet().contains(registryName) ?
			                   Registry.FLUID.getOrDefault(registryName) : null;
			// Prevent unnecessary config resets adding an exception for the default value
			return filter.test(item) || item == this.value ? item : null;
		} catch (ResourceLocationException e) {
			return null;
		}
	}
	
	protected List<Fluid> supplyOptions() {
		return Registry.FLUID.getEntries().stream().map(Entry::getValue).filter(filter)
		  .collect(Collectors.toList());
	}
	
	@Override
	protected Optional<ConfigValue<?>> buildConfigEntry(ForgeConfigSpec.Builder builder) {
		assert value.getRegistryName() != null;
		return Optional.of(decorate(builder).define(
		  name, value.getRegistryName().toString(), configValidator()));
	}
	
	@OnlyIn(Dist.CLIENT) @Override public Optional<AbstractConfigListEntry<Fluid>> buildGUIEntry(
	  ConfigEntryBuilder builder
	) {
		final ComboBoxFieldBuilder<Fluid> entryBuilder =
		  builder.startComboBox(getDisplayName(), ofFluid(), forGui(get()))
			 .setSuggestionProvider(new SupplierSuggestionProvider<>(this::supplyOptions))
			 .setSuggestionMode(false);
		return Optional.of(decorate(entryBuilder).build());
	}
}

