package endorh.simpleconfig.api.entry;

import endorh.simpleconfig.api.ConfigEntryBuilder;
import endorh.simpleconfig.api.KeyEntryBuilder;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public interface EnumEntryBuilder<E extends Enum<E>>
  extends ConfigEntryBuilder<E, E, E, EnumEntryBuilder<E>>, KeyEntryBuilder<E> {
	@Contract(pure=true) @NotNull EnumEntryBuilder<E> useComboBox();
	
	@Contract(pure=true) @NotNull EnumEntryBuilder<E> useComboBox(Boolean useComboBox);
}
