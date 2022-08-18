package endorh.simpleconfig.api.entry;

import endorh.simpleconfig.api.ConfigEntryBuilder;
import endorh.simpleconfig.api.KeyEntryBuilder;
import net.minecraft.util.ResourceLocation;
import org.jetbrains.annotations.Contract;

import java.util.List;
import java.util.function.Supplier;

public interface ResourceEntryBuilder<Self extends ResourceEntryBuilder<Self>>
  extends ConfigEntryBuilder<ResourceLocation, String, ResourceLocation, Self>, KeyEntryBuilder<ResourceLocation> {
	@Contract(pure=true) Self suggest(Supplier<List<ResourceLocation>> suggestionSupplier);
	
	@Contract(pure=true) Self suggest(List<ResourceLocation> suggestions);
}
