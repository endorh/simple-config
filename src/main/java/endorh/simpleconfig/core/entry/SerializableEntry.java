package endorh.simpleconfig.core.entry;

import endorh.simpleconfig.api.ConfigEntryHolder;
import endorh.simpleconfig.api.entry.ConfigEntrySerializer;
import endorh.simpleconfig.api.entry.ISerializableConfigEntry;
import endorh.simpleconfig.api.entry.ISerializableEntryBuilder;
import endorh.simpleconfig.api.ui.TextFormatter;
import endorh.simpleconfig.core.BackingField;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.function.Function;

/**
 * Simple serializable entry.
 * @param <V> Type of the value
 */
public class SerializableEntry<V> extends AbstractSerializableEntry<V> {
	public Function<V, String> serializer;
	public Function<String, Optional<V>> deserializer;
	public TextFormatter textFormatter = TextFormatter.DEFAULT;
	
	@Internal public SerializableEntry(
	  ConfigEntryHolder parent, String name, V value,
	  Function<V, String> serializer,
	  Function<String, Optional<V>> deserializer,
	  Class<?> typeClass
	) {
		super(parent, name, value, typeClass);
		this.serializer = serializer;
		this.deserializer = deserializer;
	}
	
	public static class Builder<V> extends AbstractSerializableEntry.Builder<
	  V, SerializableEntry<V>, ISerializableEntryBuilder<V>, Builder<V>
	> implements ISerializableEntryBuilder<V> {
		protected Function<V, String> serializer;
		protected Function<String, Optional<V>> deserializer;
		protected TextFormatter formatter = TextFormatter.DEFAULT;
		
		public Builder(V value, Function<V, String> serializer, Function<String, Optional<V>> deserializer) {
			super(value);
			this.serializer = serializer;
			this.deserializer = deserializer;
		}
		
		public Builder(V value, ConfigEntrySerializer<V> serializer) {
			this(value, serializer::serializeConfigEntry, serializer::deserializeConfigEntry);
			this.typeClass = serializer.getClass(value);
			//noinspection unchecked
			this.backingFieldBuilder =
			  BackingField.field(Function.identity(), (Class<V>) typeClass)
			    .withCommitter(Function.identity());
			this.formatter = serializer.getConfigTextFormatter();
		}
		
		@Override @Contract(pure=true) public @NotNull Builder<V> fieldClass(Class<?> fieldClass) {
			Builder<V> copy = copy();
			copy.typeClass = fieldClass;
			return copy;
		}
		
		@Override @Contract(pure=true) public @NotNull Builder<V> setTextFormatter(
		  TextFormatter formatter) {
			Builder<V> copy = copy();
			copy.formatter = formatter;
			return copy;
		}
		
		@Override
		protected SerializableEntry<V> buildEntry(ConfigEntryHolder parent, String name) {
			SerializableEntry<V> entry = new SerializableEntry<>(parent, name, value, serializer, deserializer, typeClass);
			entry.textFormatter = formatter;
			return entry;
		}
		
		@Override protected Builder<V> createCopy(V value) {
			return new Builder<>(value, serializer, deserializer);
		}
	}
	
	public static class SelfSerializableBuilder<V extends ISerializableConfigEntry<V>> extends Builder<V> {
		public SelfSerializableBuilder(V value) {
			super(value, value.getConfigSerializer());
		}
	}
	
	@Override protected TextFormatter getTextFormatter() {
		return textFormatter;
	}
	
	@Override
	protected String serialize(V value) {
		return serializer.apply(value);
	}
	
	@Override
	protected @Nullable V deserialize(String value) {
		return deserializer.apply(value).orElse(null);
	}
}
