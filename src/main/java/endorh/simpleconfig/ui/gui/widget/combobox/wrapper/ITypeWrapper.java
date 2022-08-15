package endorh.simpleconfig.ui.gui.widget.combobox.wrapper;

import com.mojang.blaze3d.matrix.MatrixStack;
import endorh.simpleconfig.ui.api.ITextFormatter;
import endorh.simpleconfig.ui.gui.SimpleConfigIcons;
import endorh.simpleconfig.ui.gui.icon.Icon;
import endorh.simpleconfig.ui.gui.widget.combobox.IComboBoxModel;
import net.minecraft.util.text.ITextComponent;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.regex.Pattern;

public interface ITypeWrapper<T> {
	Icon ICON_ERROR = SimpleConfigIcons.ComboBox.ERROR;
	Icon ICON_UNKNOWN = SimpleConfigIcons.ComboBox.UNKNOWN;
	
	/**
	 * Whether this type has an icon to display in combo boxes<br>
	 * Subclasses that return yes should also override {@link ITypeWrapper#renderIcon}
	 */
	default boolean hasIcon() {
		return false;
	}
	
	/**
	 * Only queried if {@link ITypeWrapper#hasIcon} returns true
	 *
	 * @return The height reserved for the icon of this type
	 */
	default int getIconHeight() {
		return 20;
	}
	
	/**
	 * Only queried if {@link ITypeWrapper#hasIcon()} returns true.
	 *
	 * @return The width reserved for the icons of this type.
	 * Defaults to {@link ITypeWrapper#getIconHeight()}.
	 */
	default int getIconWidth() {
		return getIconHeight();
	}
	
	/**
	 * Render the icon for an element, by default calls {@link ITypeWrapper#getIcon}
	 * and renders it if present.
	 */
	default void renderIcon(
	  @Nullable T element, String text, @NotNull MatrixStack mStack, int x, int y, int w, int h,
	  int mouseX, int mouseY, float delta
	) {
		final Optional<Icon> opt = getIcon(element, text);
		opt.ifPresent(icon -> icon.renderCentered(mStack, x, y, w, h));
	}
	
	/**
	 * Get the icon of an element.<br>
	 * Implementations may alternatively override
	 * {@link ITypeWrapper#renderIcon} directly.
	 *
	 * @param element The element being rendered, possibly null.
	 * @param text    The text written by the user, possibly not matching
	 *                any valid element if element is null.
	 */
	default Optional<Icon> getIcon(@Nullable T element, String text) {
		return Optional.empty();
	}
	
	/**
	 * Parse an element from its string representation, if possible
	 *
	 * @return A pair containing the parsed element (or empty)
	 * and an optional parse error message
	 */
	Pair<Optional<T>, Optional<ITextComponent>> parseElement(@NotNull String text);
	
	/**
	 * Get the display name of the element.<br>
	 * It should have the same text as the lookup name,
	 * otherwise the lookup will use the string name.
	 */
	ITextComponent getDisplayName(@NotNull T element);
	
	@Internal Pattern STYLE_ESCAPE = Pattern.compile("§[\\da-f]");
	
	/**
	 * Get a string name of the element to be used
	 * for query lookup by the {@link IComboBoxModel}<br>
	 * Should have the same text as the namme returned by {@link ITypeWrapper#getDisplayName}
	 */
	default String getName(@NotNull T element) {
		return STYLE_ESCAPE.matcher(getDisplayName(element).getString()).replaceAll("");
	}
	
	/**
	 * Get an optional text formatter for this type.<br>
	 * The text formatter will be used to format the text when a value is
	 * not selected.
	 *
	 * @return A text formatter for this type or {@code null}.
	 */
	default @Nullable ITextFormatter getTextFormatter() {
		return null;
	}
}
