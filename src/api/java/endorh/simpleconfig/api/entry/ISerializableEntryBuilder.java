package endorh.simpleconfig.api.entry;

import endorh.simpleconfig.api.ui.ITextFormatter;
import org.jetbrains.annotations.Contract;

public interface ISerializableEntryBuilder<V>
  extends SerializableEntryBuilder<V, ISerializableEntryBuilder<V>> {
	@Contract(pure=true) ISerializableEntryBuilder<V> fieldClass(Class<?> fieldClass);
	
	@Contract(pure=true) ISerializableEntryBuilder<V> setTextFormatter(ITextFormatter formatter);
}
