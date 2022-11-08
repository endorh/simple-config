package endorh.simpleconfig.api.entry;

import endorh.simpleconfig.api.AtomicEntryBuilder;
import endorh.simpleconfig.api.ConfigEntryBuilder;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public interface EnumEntryBuilder<E extends Enum<E>>
  extends ConfigEntryBuilder<@NotNull E, E, E, EnumEntryBuilder<E>>, AtomicEntryBuilder {
	/**
	 * Configure this entry to always use a combo box instead of an enum button.<br>
	 * Enum entries with a number of options greater than a user-defined
	 * threshold will automatically use a combo box.
	 */
	@Contract(pure=true) @NotNull EnumEntryBuilder<E> useComboBox();
	
	/**
	 * Configure this entry to always use a combo box instead of an enum button.<br>
	 * Enum entries with a number of options greater than a user-defined
	 * threshold will automatically use a combo box.
	 */
	@Contract(pure=true) @NotNull EnumEntryBuilder<E> useComboBox(Boolean useComboBox);
}
