package endorh.simpleconfig.api.entry;

import endorh.simpleconfig.api.AtomicEntryBuilder;
import endorh.simpleconfig.api.ConfigEntryBuilder;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public interface EnumEntryBuilder<E extends Enum<E>>
  extends ConfigEntryBuilder<@NotNull E, E, E, EnumEntryBuilder<E>>, AtomicEntryBuilder {
	@Contract(pure=true) @NotNull EnumEntryBuilder<E> useComboBox();
	
	@Contract(pure=true) @NotNull EnumEntryBuilder<E> useComboBox(Boolean useComboBox);
}
