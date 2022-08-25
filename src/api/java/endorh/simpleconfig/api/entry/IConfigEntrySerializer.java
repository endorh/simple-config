package endorh.simpleconfig.api.entry;

import endorh.simpleconfig.api.ui.ITextFormatter;

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
