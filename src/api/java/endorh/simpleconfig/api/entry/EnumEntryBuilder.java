package endorh.simpleconfig.api.entry;

import endorh.simpleconfig.api.AtomicEntryBuilder;
import endorh.simpleconfig.api.ConfigEntryBuilder;
import net.minecraft.util.text.ITextComponent;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

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
	
	/**
	 * Implement this interface in your enum classes to provide your own
	 * translations/tooltips for values.<br>
	 *
	 * Keep in mind that you probably don't need to do this.
	 * Enum values get automatically mapped translation keys:
	 * <ul>
	 *    <li>{@code <modid>.config.enum.<enum_class_name>.<enum_value_name>}</li>
	 *    <li>{@code <modid>.config.enum.<enum_class_name>.<enum_value_name>:help} (for the
	 *    tooltip (optional))</li>
	 * </ul>
	 * where {@code <modid>} is your mod ID, {@code <enum_class_name>} is the name of the enum
	 * class in snake_case and {@code <enum_value_name>} is the name of the enum value in
	 * snake_case.
	 */
	interface TranslatedEnum {
		ITextComponent getDisplayName();
		default List<ITextComponent> getHelpTooltip() {
			return Collections.emptyList();
		}
	}
}
