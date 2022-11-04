package endorh.simpleconfig.core.entry;

import endorh.simpleconfig.api.ConfigEntryHolder;
import endorh.simpleconfig.api.entry.ResourceLocationEntryBuilder;
import endorh.simpleconfig.api.ui.ITextFormatter;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.ResourceLocationException;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class ResourceLocationEntry
  extends AbstractSerializableEntry<ResourceLocation, ResourceLocationEntry> {
	
	@Internal public ResourceLocationEntry(
	  ConfigEntryHolder parent, String name, ResourceLocation value
	) {
		super(parent, name, value, ResourceLocation.class);
	}
	
	public static class Builder extends AbstractSerializableEntry.Builder<ResourceLocation,
	  ResourceLocationEntry, ResourceLocationEntryBuilder, Builder>
	  implements ResourceLocationEntryBuilder {
		public Builder(ResourceLocation value) {
			super(value, ResourceLocation.class);
		}
		
		@Override
		protected ResourceLocationEntry buildEntry(ConfigEntryHolder parent, String name) {
			return new ResourceLocationEntry(parent, name, value);
		}
		
		@Contract(value="_ -> new", pure=true) @Override protected Builder createCopy(ResourceLocation value) {
			return new Builder(value);
		}
	}
	
	@Override
	protected Optional<ITextComponent> getErrorMessage(String value) {
		try {
			new ResourceLocation(value);
			return Optional.empty();
		} catch (ResourceLocationException e) {
			return Optional.of(new TranslationTextComponent(
			  "simpleconfig.config.error.invalid_resource_location", e.getMessage()));
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
	
	@Override protected @Nullable String getTypeComment() {
		return "namespace:path";
	}
	
	@Override protected ITextFormatter getTextFormatter() {
		return ITextFormatter.forResourceLocation();
	}
}
