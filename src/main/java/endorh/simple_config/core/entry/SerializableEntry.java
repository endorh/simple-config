package endorh.simple_config.core.entry;

import endorh.simple_config.core.AbstractConfigEntry;
import endorh.simple_config.core.ISimpleConfigEntryHolder;
import org.jetbrains.annotations.ApiStatus.Internal;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.function.Function;

/**
 * Doesn't have a GUI<br>
 * To create your custom type of entry with GUI,
 * extend this class or {@link AbstractConfigEntry} directly
 *
 * @param <V> Type of the value
 */
public class SerializableEntry<V>
  extends AbstractSerializableEntry<V, SerializableEntry<V>> {
	public Function<V, String> serializer;
	public Function<String, Optional<V>> deserializer;
	
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
		
		public Builder(V value, Function<V, String> serializer, Function<String, Optional<V>> deserializer) {
			super(value);
			this.serializer = serializer;
			this.deserializer = deserializer;
		}
		
		public Builder(V value, IConfigEntrySerializer<V> serializer) {
			this(value, serializer::serializeConfigEntry, serializer::deserializeConfigEntry);
		}
		
		public Builder<V> fieldClass(Class<?> fieldClass) {
			typeClass = fieldClass;
			return this;
		}
		
		@Override
		protected SerializableEntry<V> buildEntry(ISimpleConfigEntryHolder parent, String name) {
			return new SerializableEntry<>(parent, name, value, serializer, deserializer, typeClass);
		}
	}
	
	public static class SelfSerializableBuilder<V extends ISerializableConfigEntry<V>> extends Builder<V> {
		public SelfSerializableBuilder(V value) {
			super(value, value.getConfigSerializer());
		}
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
