package endorh.simpleconfig.api.entry;

import endorh.simpleconfig.api.AtomicEntryBuilder;
import endorh.simpleconfig.api.ConfigEntryBuilder;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.material.Fluid;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Predicate;

public interface FluidEntryBuilder
  extends ConfigEntryBuilder<@NotNull Fluid, String, Fluid, FluidEntryBuilder>, AtomicEntryBuilder {
	/**
	 * When true (the default), requires the filled bucket item to have an item group.<br>
	 * This excludes the empty fluid.
	 */
	@Contract(pure=true) @NotNull FluidEntryBuilder setRequireGroup(boolean requireGroup);
	
	/**
	 * When true (the default), excludes the flowing variants of fluids, that is,
	 * FlowingFluids whose still fluids are different from themselves.<br>
	 * This excludes FLOWING_WATER and FLOWING_LAVA.
	 */
	@Contract(pure=true) @NotNull FluidEntryBuilder setExcludeFlowing(boolean excludeFlowing);
	
	/**
	 * Restrict the selectable fluids with an arbitrary predicate.
	 */
	@Contract(pure=true) @NotNull FluidEntryBuilder from(Predicate<Fluid> filter);
	
	/**
	 * Restrict the selectable fluids to the given choices.
	 */
	@Contract(pure=true) @NotNull FluidEntryBuilder from(List<Fluid> choices);
	
	/**
	 * Restrict the selectable fluids to the given choices.
	 */
	@Contract(pure=true) @NotNull FluidEntryBuilder from(Fluid... choices);
	
	/**
	 * Restrict the selectable items to those of a tag<br>
	 * This can only be done on server configs, since tags
	 * are server-dependant<br>
	 * This filter can be combined with other filters.
	 */
	@Contract(pure=true) @NotNull FluidEntryBuilder from(TagKey<Fluid> tag);
}
