package endorh.simpleconfig.api.entry;

import net.minecraft.fluid.Fluid;
import net.minecraft.tags.ITag;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public interface FluidNameEntryBuilder
  extends ResourceEntryBuilder<FluidNameEntryBuilder> {
	/**
	 * Restrict the selectable items to those of a tag<br>
	 * This can only be done on server configs, since tags
	 * are server-dependant
	 */
	@Contract(pure=true) @NotNull FluidNameEntryBuilder suggest(ITag<Fluid> tag);
}
