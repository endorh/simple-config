package endorh.simpleconfig.core.entry;

import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import endorh.simpleconfig.api.ConfigEntryHolder;
import endorh.simpleconfig.api.SimpleConfig;
import endorh.simpleconfig.api.entry.FluidNameEntryBuilder;
import endorh.simpleconfig.ui.api.ConfigFieldBuilder;
import endorh.simpleconfig.ui.impl.builders.ComboBoxFieldBuilder;
import endorh.simpleconfig.ui.impl.builders.FieldBuilder;
import net.minecraft.fluid.FlowingFluid;
import net.minecraft.fluid.Fluid;
import net.minecraft.tags.ITag;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.registry.Registry;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.ForgeRegistryEntry;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static endorh.simpleconfig.ui.impl.builders.ComboBoxFieldBuilder.ofFluidName;

public class FluidNameEntry extends AbstractResourceEntry<FluidNameEntry> {
	
	@Internal public FluidNameEntry(
	  ConfigEntryHolder parent, String name,
	  @Nullable ResourceLocation value
	) {
		super(parent, name, value != null ? value : new ResourceLocation(""));
	}
	
	@Override protected @Nullable String getTypeComment() {
		return "Fluid";
	}
	
	public static class Builder extends AbstractResourceEntry.Builder<FluidNameEntry, FluidNameEntryBuilder, Builder>
	  implements FluidNameEntryBuilder {
		protected Supplier<List<ResourceLocation>> suggestionSupplier;
		protected ITag<Fluid> tag = null;
		
		public Builder(ResourceLocation value) {
			super(value, ResourceLocation.class);
			suggestionSupplier = () -> Registry.FLUID.getEntries().stream().map(Entry::getValue)
			  .filter(f -> f.getFilledBucket().getGroup() != null)
			  .filter(f -> !(f instanceof FlowingFluid) || ((FlowingFluid) f).getStillFluid() == f)
			  .map(ForgeRegistryEntry::getRegistryName).collect(Collectors.toList());
		}
		
		@Override @Contract(pure=true) public Builder suggest(ITag<Fluid> tag) {
			Builder copy = copy();
			copy.tag = tag;
			return copy;
		}
		
		@Override
		protected FluidNameEntry buildEntry(ConfigEntryHolder parent, String name) {
			if (parent.getRoot().getType() != SimpleConfig.Type.SERVER && tag != null)
				throw new IllegalArgumentException(
				  "Cannot use tag item filters in non-server config entry");
			if (tag != null) {
				final Supplier<List<ResourceLocation>> supplier = suggestionSupplier;
				suggestionSupplier = () -> Stream.concat(
				  supplier != null? supplier.get().stream() : Stream.empty(),
				  tag.getAllElements().stream().map(ForgeRegistryEntry::getRegistryName)
				).collect(Collectors.toList());
			}
			return new FluidNameEntry(parent, name, value);
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
		  builder.startComboBox(getDisplayName(), ofFluidName(), forGui(get()));
		return Optional.of(decorate(entryBuilder));
	}
	
	@Override public boolean addCommandSuggestions(SuggestionsBuilder builder) {
		super.addCommandSuggestions(builder);
		ResourceLocation current = get();
		for (ResourceLocation o: ForgeRegistries.FLUIDS.getKeys())
			if (!o.equals(current) && !o.equals(defValue) && isValidValue(o))
				builder.suggest(forCommand(o));
		return true;
	}
}
