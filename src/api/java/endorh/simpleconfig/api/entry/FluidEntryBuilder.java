package endorh.simpleconfig.api.entry;

import endorh.simpleconfig.api.ConfigEntryBuilder;
import endorh.simpleconfig.api.KeyEntryBuilder;
import net.minecraft.tags.Tag;
import net.minecraft.world.level.material.Fluid;
import org.jetbrains.annotations.Contract;

import java.util.List;
import java.util.function.Predicate;

public interface FluidEntryBuilder
  extends ConfigEntryBuilder<Fluid, String, Fluid, FluidEntryBuilder>, KeyEntryBuilder<Fluid> {
	/**
	 * When true (the default), requires the filled bucket item to have an item group.<br>
	 * This excludes the empty fluid.
	 */
	@Contract(pure=true) FluidEntryBuilder setRequireGroup(boolean requireGroup);
	
	/**
	 * When true (the default), excludes the flowing variants of fluids, that is,
	 * FlowingFluids whose still fluids are different from themselves.<br>
	 * This excludes FLOWING_WATER and FLOWING_LAVA.
	 */
	@Contract(pure=true) FluidEntryBuilder setExcludeFlowing(boolean excludeFlowing);
	
	@Contract(pure=true) FluidEntryBuilder from(Predicate<Fluid> filter);
	
	@Contract(pure=true) FluidEntryBuilder from(List<Fluid> choices);
	
	@Contract(pure=true) FluidEntryBuilder from(Fluid... choices);
	
	/**
	 * Restrict the selectable items to those of a tag<br>
	 * This can only be done on server configs, since tags
	 * are server-dependant
	 */
	@Contract(pure=true) FluidEntryBuilder from(Tag<Fluid> tag);
}
