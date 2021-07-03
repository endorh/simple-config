package endorh.simple_config.core.entry;

import endorh.simple_config.core.AbstractConfigEntry;

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
	
	public SerializableEntry(
	  V value, IConfigEntrySerializer<V> serializer
	) {
		this(value, serializer::serializeConfigEntry, serializer::deserializeConfigEntry);
	}
	
	public SerializableEntry(
	  V value,
	  Function<V, String> serializer,
	  Function<String, Optional<V>> deserializer
	) { this(value, serializer, deserializer, value.getClass()); }
	
	public SerializableEntry(
	  V value,
	  Function<V, String> serializer,
	  Function<String, Optional<V>> deserializer,
	  Class<?> typeClass
	) {
		super(value, typeClass);
		this.serializer = serializer;
		this.deserializer = deserializer;
	}
	
	@Override
	protected String serialize(V value) {
		return serializer.apply(value);
	}
	
	@Override
	protected @Nullable V deserialize(String value) {
		return deserializer.apply(value).orElse(null);
	}
	
	public static class SerializableConfigEntry<T extends ISerializableConfigEntry<T>> extends SerializableEntry<T> {
		public SerializableConfigEntry(T value) {
			super(value, null, null, value.getConfigEntryTypeClass());
			final IConfigEntrySerializer<T> serializer = value.getConfigSerializer();
			this.serializer = serializer::serializeConfigEntry;
			this.deserializer = serializer::deserializeConfigEntry;
		}
	}
}
