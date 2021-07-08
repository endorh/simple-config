package endorh.simple_config.core.entry;

import endorh.simple_config.core.ISimpleConfigEntryHolder;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.ResourceLocationException;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class ResourceLocationEntry
  extends AbstractSerializableEntry<ResourceLocation, ResourceLocationEntry> {
	
	@Internal public ResourceLocationEntry(
	  ISimpleConfigEntryHolder parent, String name, ResourceLocation value
	) {
		super(parent, name, value, ResourceLocation.class);
	}
	
	public static class Builder extends AbstractSerializableEntry.Builder<ResourceLocation, ResourceLocationEntry, Builder> {
		public Builder(ResourceLocation value) {
			super(value, ResourceLocation.class);
		}
		
		@Override
		protected ResourceLocationEntry buildEntry(ISimpleConfigEntryHolder parent, String name) {
			return new ResourceLocationEntry(parent, name, value);
		}
	}
	
	@Override
	protected Optional<ITextComponent> getErrorMessage(String value) {
		try {
			new ResourceLocation(value);
			return Optional.empty();
		} catch (ResourceLocationException e) {
			return Optional.of(new TranslationTextComponent(
			  "simple-config.config.error.invalid_resource_location", e.getMessage()));
		}
	}
	
	@Override
	protected String serialize(ResourceLocation value) {
		return value.toString();
	}
	
	@Override
	protected @Nullable ResourceLocation deserialize(String value) {
		try {
			return value != null ? new ResourceLocation(value) : null;
		} catch (ResourceLocationException ignored) {
			return null;
		}
	}
}
