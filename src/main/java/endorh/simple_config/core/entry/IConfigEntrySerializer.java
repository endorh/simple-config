package endorh.simple_config.core.entry;

import java.util.Optional;

public interface IConfigEntrySerializer<T> {
	String serializeConfigEntry(T value);
	
	Optional<T> deserializeConfigEntry(String value);
}
