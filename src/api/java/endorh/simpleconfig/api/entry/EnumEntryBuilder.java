package endorh.simpleconfig.api.entry;

import endorh.simpleconfig.api.AtomicEntryBuilder;
import endorh.simpleconfig.api.ConfigEntryBuilder;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
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
	@Contract(pure=true) @NotNull EnumEntryBuilder<E> useComboBox(@Nullable Boolean useComboBox);
	
	/**
	 * Restricts the possible values of this entry to the given values.<br>
	 * Restrictions are overwritten if previously set, not combined.<br>
	 * The default value is always considered allowed.
	 *
	 * @see #restrict(Collection)
	 * @see #clearRestrictions()
	 */
	@Contract(pure=true) @NotNull EnumEntryBuilder<E> restrict(E... values);
	
	/**
	 * Restricts the possible values of this entry to the given values.<br>
	 * The default value is always considered allowed.<br>
	 * Restrictions are overwritten if previously set, not combined.<br>
	 * <br>
	 * Passing a null value removes any restriction previously set.
	 *
	 * @see #restrict(E...)
	 * @see #clearRestrictions()
	 */
	@Contract(pure=true) @NotNull EnumEntryBuilder<E> restrict(@Nullable Collection<E> values);
	
	/**
	 * Clears all restrictions on the possible values of this entry.<br>
	 * Equivalent to calling {@link #restrict(Collection)} with a null collection.
	 *
	 * @see #restrict(E...)
	 * @see #restrict(Collection)
	 */
	@Contract(pure=true) @NotNull EnumEntryBuilder<E> clearRestrictions();
	
	/**
	 * Implement this interface in your enum classes to provide your own
	 * translations/tooltips for values.<br>
	 *
	 * Keep in mind that you probably don't need to do this.
	 * Enum values get automatically mapped translation keys:
	 * <ul>
	 *    <li>{@code <modid>.config.enum.<EnumClassName>.<ENUM_VALUE_NAME>}</li>
	 *    <li>{@code <modid>.config.enum.<EnumClassName>.<ENUM_VALUE_NAME>:help} (for the
	 *    tooltip (optional))</li>
	 * </ul>
	 * where {@code <modid>} is your mod ID, {@code <EnumClassName>} is the name of the enum
	 * class and {@code <ENUM_VALUE_NAME>} is the name of the enum value, all in their
	 * original capitalization.
	 */
	interface TranslatedEnum {
		Component getDisplayName();
		default List<Component> getHelpTooltip() {
			return Collections.emptyList();
		}
	}
}
