package endorh.simpleconfig.api.entry;

import endorh.simpleconfig.api.ui.ITextFormatter;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public interface IConfigEntrySerializer<T> {
	/**
	 * Serialize the value to a string.
	 */
	String serializeConfigEntry(T value);
	
	/**
	 * Deserialize the value from a string.
	 * @return The deserialized value, or {@link Optional#empty()} if the string is invalid.
	 */
	Optional<T> deserializeConfigEntry(String value);
	
	/**
	 * Get the class of the serialized value.<br>
	 * Used for type checking.
	 */
	default @NotNull Class<?> getClass(T value) {
		return value.getClass();
	}
	
	/**
	 * Get the {@link ITextFormatter} for this entry.<br>
	 * It will be used to provide syntax highlighting and editing
	 * features in the GUI.
	 */
	default @NotNull ITextFormatter getConfigTextFormatter() {
		return ITextFormatter.DEFAULT;
	}
}
