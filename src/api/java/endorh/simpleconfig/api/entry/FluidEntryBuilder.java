package endorh.simpleconfig.api.entry;

import endorh.simpleconfig.api.AtomicEntryBuilder;
import endorh.simpleconfig.api.ConfigEntryBuilder;
import net.minecraft.fluid.Fluid;
import net.minecraft.tags.ITag;
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
	
	@Contract(pure=true) @NotNull FluidEntryBuilder from(Predicate<Fluid> filter);
	
	@Contract(pure=true) @NotNull FluidEntryBuilder from(List<Fluid> choices);
	
	@Contract(pure=true) @NotNull FluidEntryBuilder from(Fluid... choices);
	
	/**
	 * Restrict the selectable items to those of a tag<br>
	 * This can only be done on server configs, since tags
	 * are server-dependant
	 */
	@Contract(pure=true) @NotNull FluidEntryBuilder from(ITag<Fluid> tag);
}
