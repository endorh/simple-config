package endorh.simpleconfig.core.entry;

import com.google.common.collect.Lists;
import endorh.simpleconfig.ui.gui.widget.combobox.SimpleComboBoxModel;
import endorh.simpleconfig.ui.impl.builders.ComboBoxFieldBuilder;
import endorh.simpleconfig.core.AbstractConfigEntry;
import endorh.simpleconfig.core.AbstractConfigEntryBuilder;
import endorh.simpleconfig.core.IKeyEntry;
import endorh.simpleconfig.core.ISimpleConfigEntryHolder;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.ResourceLocationException;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

public abstract class AbstractResourceEntry<Self extends AbstractResourceEntry<Self>>
  extends AbstractConfigEntry<ResourceLocation, String, ResourceLocation, Self>
  implements IKeyEntry<String, ResourceLocation> {
	protected SimpleComboBoxModel<ResourceLocation> suggestionProvider;
	
	public AbstractResourceEntry(
	  ISimpleConfigEntryHolder parent, String name, @Nullable ResourceLocation value
	) {
		super(parent, name, value != null? value : new ResourceLocation(""));
	}
	
	public static abstract class Builder<
	  Entry extends AbstractResourceEntry<Entry>,
	  Self extends Builder<Entry, Self>>
	  extends AbstractConfigEntryBuilder<ResourceLocation, String, ResourceLocation, Entry, Self> {
		protected Supplier<List<ResourceLocation>> suggestionSupplier = Lists::newArrayList;
		protected boolean suggestionMode = true;
		
		public Builder(ResourceLocation value, Class<?> typeClass) {
			super(value, typeClass);
		}
		
		public Self suggest(Supplier<List<ResourceLocation>> suggestionSupplier) {
			Self copy = copy();
			copy.suggestionSupplier = suggestionSupplier;
			return copy;
		}
		
		public Self suggest(List<ResourceLocation> suggestions) {
			Self copy = copy();
			copy.suggestionSupplier = () -> suggestions;
			return copy;
		}
		
		@Override protected Entry build(ISimpleConfigEntryHolder parent, String name) {
			final Entry entry = super.build(parent, name);
			entry.suggestionProvider = new SimpleComboBoxModel<>(suggestionSupplier);
			return entry;
		}
		
		@Override protected Self copy() {
			final Self copy = super.copy();
			copy.suggestionSupplier = suggestionSupplier;
			copy.suggestionMode = suggestionMode;
			return copy;
		}
	}
	
	@Override public String forConfig(ResourceLocation value) {
		return value.toString();
	}
	
	@Nullable @Override public ResourceLocation fromConfig(@Nullable String value) {
		if (value == null) return null;
		try {
			return new ResourceLocation(value);
		} catch (ResourceLocationException e) {
			return null;
		}
	}
	
	@Override public Optional<String> deserializeStringKey(@NotNull String key) {
		return Optional.of(key);
	}
	
	@Override protected Optional<ConfigValue<?>> buildConfigEntry(ForgeConfigSpec.Builder builder) {
		return Optional.of(decorate(builder).define(name, forConfig(value), createConfigValidator()));
	}
	
	protected ComboBoxFieldBuilder<ResourceLocation> decorate(
	  ComboBoxFieldBuilder<ResourceLocation> builder
	) {
		builder = super.decorate(builder);
		builder.setSuggestionProvider(suggestionProvider);
		builder.setSuggestionMode(true);
		return builder;
	}
}
