package endorh.simpleconfig.api.entry;

import endorh.simpleconfig.api.ui.ITextFormatter;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public interface IConfigEntrySerializer<T> {
	String serializeConfigEntry(T value);
	Optional<T> deserializeConfigEntry(String value);
	
	default @NotNull Class<?> getClass(T value) {
		return value.getClass();
	}
	default @NotNull ITextFormatter getConfigTextFormatter() {
		return ITextFormatter.DEFAULT;
	}
}
