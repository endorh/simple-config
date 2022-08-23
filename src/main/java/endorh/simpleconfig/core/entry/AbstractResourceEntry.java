package endorh.simpleconfig.core.entry;

import com.google.common.collect.Lists;
import endorh.simpleconfig.api.ConfigEntryHolder;
import endorh.simpleconfig.api.entry.ResourceEntryBuilder;
import endorh.simpleconfig.core.AbstractConfigEntry;
import endorh.simpleconfig.core.AbstractConfigEntryBuilder;
import endorh.simpleconfig.core.IKeyEntry;
import endorh.simpleconfig.ui.gui.widget.combobox.SimpleComboBoxModel;
import endorh.simpleconfig.ui.impl.builders.ComboBoxFieldBuilder;
import net.minecraft.ResourceLocationException;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Supplier;

public abstract class AbstractResourceEntry<Self extends AbstractResourceEntry<Self>>
  extends AbstractConfigEntry<ResourceLocation, String, ResourceLocation>
  implements IKeyEntry<ResourceLocation> {
	protected SimpleComboBoxModel<ResourceLocation> suggestionProvider;
	
	public AbstractResourceEntry(
	  ConfigEntryHolder parent, String name, @Nullable ResourceLocation value
	) {
		super(parent, name, value != null? value : new ResourceLocation(""));
	}
	
	public static abstract class Builder<
	  Entry extends AbstractResourceEntry<Entry>,
	  Self extends ResourceEntryBuilder<Self>,
	  SelfImpl extends Builder<Entry, Self, SelfImpl>
	> extends AbstractConfigEntryBuilder<ResourceLocation, String, ResourceLocation, Entry, Self, SelfImpl>
	  implements ResourceEntryBuilder<Self> {
		protected Supplier<List<ResourceLocation>> suggestionSupplier = Lists::newArrayList;
		protected boolean suggestionMode = true;
		
		public Builder(ResourceLocation value, Class<?> typeClass) {
			super(value, typeClass);
		}
		
		@Override @Contract(pure=true) public Self suggest(
		  Supplier<List<ResourceLocation>> suggestionSupplier
		) {
			SelfImpl copy = copy();
			copy.suggestionSupplier = suggestionSupplier;
			return copy.castSelf();
		}
		
		@Override @Contract(pure=true) public Self suggest(List<ResourceLocation> suggestions) {
			SelfImpl copy = copy();
			copy.suggestionSupplier = () -> suggestions;
			return copy.castSelf();
		}
		
		@Override protected Entry build(@NotNull ConfigEntryHolder parent, String name) {
			final Entry entry = super.build(parent, name);
			entry.suggestionProvider = new SimpleComboBoxModel<>(suggestionSupplier);
			return entry;
		}
		
		@Override public SelfImpl copy() {
			final SelfImpl copy = super.copy();
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
	
	protected @Nullable String getTypeComment() {
		return null;
	}
	
	@Override public List<String> getConfigCommentTooltips() {
		List<String> tooltips = super.getConfigCommentTooltips();
		String typeComment = getTypeComment();
		typeComment = typeComment != null? typeComment + ": " : "";
		tooltips.add(typeComment + "namespace:path");
		return tooltips;
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
