package endorh.simpleconfig.core.entry;

import endorh.simpleconfig.ui.api.ITextFormatter;

import java.util.Optional;

public interface IConfigEntrySerializer<T> {
	String serializeConfigEntry(T value);
	
	Optional<T> deserializeConfigEntry(String value);
	
	default Class<?> getClass(T value) {
		return value.getClass();
	}
	
	default ITextFormatter getConfigTextFormatter() {
		return ITextFormatter.DEFAULT;
	}
}
