package endorh.simpleconfig.api.entry;

import endorh.simpleconfig.api.ui.ITextFormatter;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public interface ISerializableEntryBuilder<V>
  extends SerializableEntryBuilder<@NotNull V, ISerializableEntryBuilder<V>> {
	@Contract(pure=true) @NotNull ISerializableEntryBuilder<V> fieldClass(Class<?> fieldClass);
	
	@Contract(pure=true) @NotNull ISerializableEntryBuilder<V> setTextFormatter(ITextFormatter formatter);
}
