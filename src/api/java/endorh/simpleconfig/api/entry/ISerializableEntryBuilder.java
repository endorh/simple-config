package endorh.simpleconfig.api.entry;

import endorh.simpleconfig.api.ui.TextFormatter;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public interface ISerializableEntryBuilder<V>
  extends SerializableEntryBuilder<@NotNull V, ISerializableEntryBuilder<V>> {
	/**
	 * Set the class of the backing field assigned to this entry.<br>
	 * Used for type checking.
	 */
	@Contract(pure=true) @NotNull ISerializableEntryBuilder<V> fieldClass(Class<?> fieldClass);
	
	/**
	 * Set the text formatter used by this entry.<br>
	 * It will provide syntax highlighting and editing features in the GUI.
	 */
	@Contract(pure=true) @NotNull ISerializableEntryBuilder<V> setTextFormatter(
	  TextFormatter formatter);
}
