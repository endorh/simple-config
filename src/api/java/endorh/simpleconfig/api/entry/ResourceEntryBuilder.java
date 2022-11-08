package endorh.simpleconfig.api.entry;

import endorh.simpleconfig.api.AtomicEntryBuilder;
import endorh.simpleconfig.api.ConfigEntryBuilder;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Supplier;

public interface ResourceEntryBuilder<Self extends ResourceEntryBuilder<Self>>
  extends ConfigEntryBuilder<@NotNull ResourceLocation, String, ResourceLocation, Self>,
          AtomicEntryBuilder {
	/**
	 * Suggest resource locations.
	 */
	@Contract(pure=true) @NotNull Self suggest(Supplier<List<ResourceLocation>> suggestionSupplier);
	
	/**
	 * Suggest resource locations.
	 */
	@Contract(pure=true) @NotNull Self suggest(List<ResourceLocation> suggestions);
}
