package endorh.simpleconfig.core.entry;

import endorh.simpleconfig.api.ConfigEntryHolder;
import endorh.simpleconfig.api.entry.SerializableEntryBuilder;
import endorh.simpleconfig.api.ui.ITextFormatter;
import endorh.simpleconfig.core.AbstractConfigEntry;
import endorh.simpleconfig.core.AbstractConfigEntryBuilder;
import endorh.simpleconfig.core.AtomicEntry;
import endorh.simpleconfig.ui.api.ConfigFieldBuilder;
import endorh.simpleconfig.ui.gui.widget.combobox.SimpleComboBoxModel;
import endorh.simpleconfig.ui.gui.widget.combobox.wrapper.ITypeWrapper;
import endorh.simpleconfig.ui.gui.widget.combobox.wrapper.StringTypeWrapper;
import endorh.simpleconfig.ui.impl.builders.ComboBoxFieldBuilder;
import endorh.simpleconfig.ui.impl.builders.FieldBuilder;
import endorh.simpleconfig.ui.impl.builders.TextFieldBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;

public abstract class AbstractSerializableEntry<V>
  extends AbstractConfigEntry<V, String, String> implements AtomicEntry<String> {
	
	protected @Nullable Supplier<List<V>> suggestionSupplier;
	
	protected AbstractSerializableEntry(
	  ConfigEntryHolder parent, String name, V value, Class<?> typeClass
	) {
		super(parent, name, value);
		this.typeClass = typeClass;
	}
	
	public static abstract class Builder<
	  V, Entry extends AbstractSerializableEntry<V>,
	  Self extends SerializableEntryBuilder<V, Self>,
	  SelfImpl extends Builder<V, Entry, Self, SelfImpl>
	> extends AbstractConfigEntryBuilder<V, String, String, Entry, Self, SelfImpl>
	  implements SerializableEntryBuilder<V, Self> {
		protected @Nullable Supplier<List<V>> suggestionSupplier = null;
		
		protected Builder(V value) {
			super(value, value.getClass());
		}
		
		protected Builder(V value, Class<?> typeClass) {
			super(value, typeClass);
		}
		
		@SafeVarargs @Override public final @NotNull Self suggest(V... suggestions) {
			return suggest(asList(suggestions));
		}
		
		@Override public @NotNull Self suggest(@NotNull List<V> suggestions) {
			return suggest(() -> suggestions);
		}
		
		@Override public @NotNull Self suggest(Supplier<List<V>> suggestionSupplier) {
			SelfImpl copy = copy();
			copy.suggestionSupplier = suggestionSupplier;
			return copy.castSelf();
		}
		
		@Override public SelfImpl copy(V value) {
			SelfImpl copy = super.copy(value);
			copy.suggestionSupplier = suggestionSupplier;
			return copy;
		}
		
		@Override protected Entry build(@NotNull ConfigEntryHolder parent, String name) {
			Entry entry = super.build(parent, name);
			entry.suggestionSupplier = suggestionSupplier;
			return entry;
		}
	}
	
	protected abstract String serialize(V value);
	protected abstract @Nullable V deserialize(String value);
	
	@Override
	public String forGui(V value) {
		return value != null? serialize(value) : "";
	}
	
	@Nullable
	@Override public V fromGui(@Nullable String value) {
		return value != null? deserialize(value) : null;
	}
	
	@Override public String forConfig(V value) {
		return value != null? serialize(value) : "";
	}
	
	@Nullable
	@Override public V fromConfig(@Nullable String value) {
		return value != null? deserialize(value) : null;
	}
	
	protected Optional<Component> getErrorMessage(String value) {
		return Optional.of(new TranslatableComponent(
		  "simpleconfig.config.error.invalid_value_generic"));
	}
	
	@Override
	public Optional<Component> getErrorFromGUI(String value) {
		final Optional<Component> opt = super.getErrorFromGUI(value);
		if (opt.isEmpty() && fromGui(value) == null && value != null) {
			return getErrorMessage(value);
		} else return opt;
	}
	
	@Override public List<String> getConfigCommentTooltips() {
		List<String> tooltips = super.getConfigCommentTooltips();
		String typeComment = getTypeComment();
		if (typeComment != null) tooltips.add(typeComment);
		return tooltips;
	}
	
	protected @Nullable String getTypeComment() {
		return typeClass.getSimpleName();
	}
	
	protected ITextFormatter getTextFormatter() {
		return ITextFormatter.DEFAULT;
	}
	
	protected ITypeWrapper<String> getTypeWrapper() {
		return new SerializableTypeWrapper<>(this);
	}
	
	@OnlyIn(Dist.CLIENT)
	@Override public Optional<FieldBuilder<String, ?, ?>> buildGUIEntry(
	  ConfigFieldBuilder builder
	) {
		if (suggestionSupplier != null) {
			ComboBoxFieldBuilder<String> valBuilder = builder
			  .startComboBox(getDisplayName(), new StringTypeWrapper(), forGui(get()))
			  .setSuggestionProvider(new SimpleComboBoxModel<>(
				 () -> suggestionSupplier.get().stream()
				   .map(this::serialize).collect(Collectors.toList())
			  ));
			return Optional.of(decorate(valBuilder));
		} else {
			final TextFieldBuilder valBuilder = builder
			  .startTextField(getDisplayName(), forGui(get()))
			  .setTextFormatter(getTextFormatter());
			return Optional.of(decorate(valBuilder));
		}
	}
	
	public static class SerializableTypeWrapper<V> extends StringTypeWrapper {
		protected final AbstractSerializableEntry<V> entry;
		
		public SerializableTypeWrapper(AbstractSerializableEntry<V> entry) {
			this.entry = entry;
		}
		
		@Override public @Nullable ITextFormatter getTextFormatter() {
			return entry.getTextFormatter();
		}
	}
}
