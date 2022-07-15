package endorh.simpleconfig.core.entry;

import endorh.simpleconfig.core.BackingField;
import endorh.simpleconfig.core.ISimpleConfigEntryHolder;
import endorh.simpleconfig.ui.api.ITextFormatter;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.function.Function;

/**
 * Simple serializable entry.
 * @param <V> Type of the value
 */
public class SerializableEntry<V> extends AbstractSerializableEntry<V, SerializableEntry<V>> {
	public Function<V, String> serializer;
	public Function<String, Optional<V>> deserializer;
	public ITextFormatter textFormatter = ITextFormatter.DEFAULT;
	
	@Internal public SerializableEntry(
	  ISimpleConfigEntryHolder parent, String name, V value,
	  Function<V, String> serializer,
	  Function<String, Optional<V>> deserializer,
	  Class<?> typeClass
	) {
		super(parent, name, value, typeClass);
		this.serializer = serializer;
		this.deserializer = deserializer;
	}
	
	public static class Builder<V> extends AbstractSerializableEntry.Builder<V, SerializableEntry<V>, Builder<V>> {
		protected Function<V, String> serializer;
		protected Function<String, Optional<V>> deserializer;
		protected ITextFormatter formatter = ITextFormatter.DEFAULT;
		
		public Builder(V value, Function<V, String> serializer, Function<String, Optional<V>> deserializer) {
			super(value);
			this.serializer = serializer;
			this.deserializer = deserializer;
		}
		
		public Builder(V value, IConfigEntrySerializer<V> serializer) {
			this(value, serializer::serializeConfigEntry, serializer::deserializeConfigEntry);
			this.typeClass = serializer.getClass(value);
			//noinspection unchecked
			this.backingFieldBuilder =
			  typeClass != null
			  ? BackingField.<V, V>field(Function.identity(), (Class<V>) typeClass)
			    .withCommitter(Function.identity()) : null;
			this.formatter = serializer.getConfigTextFormatter();
		}
		
		public Builder<V> fieldClass(Class<?> fieldClass) {
			Builder<V> copy = copy();
			copy.typeClass = fieldClass;
			return copy;
		}
		
		public Builder<V> setTextFormatter(ITextFormatter formatter) {
			Builder<V> copy = copy();
			copy.formatter = formatter;
			return copy;
		}
		
		@Override
		protected SerializableEntry<V> buildEntry(ISimpleConfigEntryHolder parent, String name) {
			return new SerializableEntry<>(parent, name, value, serializer, deserializer, typeClass);
		}
		
		@Override protected Builder<V> createCopy() {
			return new Builder<>(value, serializer, deserializer);
		}
	}
	
	public static class SelfSerializableBuilder<V extends ISerializableConfigEntry<V>> extends Builder<V> {
		public SelfSerializableBuilder(V value) {
			super(value, value.getConfigSerializer());
		}
	}
	
	@Override protected ITextFormatter getTextFormatter() {
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
