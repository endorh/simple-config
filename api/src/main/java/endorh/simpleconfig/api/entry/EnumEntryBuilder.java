package endorh.simpleconfig.api.entry;

import endorh.simpleconfig.api.ConfigEntryBuilder;
import endorh.simpleconfig.api.KeyEntryBuilder;
import org.jetbrains.annotations.Contract;

public interface EnumEntryBuilder<E extends Enum<E>>
  extends ConfigEntryBuilder<E, E, E, EnumEntryBuilder<E>>, KeyEntryBuilder<E> {
	@Contract(pure=true) EnumEntryBuilder<E> useComboBox();
	
	@Contract(pure=true) EnumEntryBuilder<E> useComboBox(Boolean useComboBox);
}
