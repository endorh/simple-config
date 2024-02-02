package endorh.simpleconfig.ui.gui.widget.combobox;

import endorh.simpleconfig.ui.gui.widget.combobox.wrapper.TypeWrapper;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;
import java.util.Optional;

/**
 * Model used by a {@link ComboBoxWidget} to provide, filter and decorate
 * suggestions for a given user query.<br>
 * Can also provide a placeholder to show when no suggestions are available.
 *
 * @param <T> The type of combo box values.
 */
public interface IComboBoxModel<T> {
	/**
	 * Filter suggestions matching the current query and decorate them
	 * with highlighting and other formatting.
	 */
	Pair<List<T>, List<ITextComponent>> pickAndDecorateSuggestions(
	  TypeWrapper<T> typeWrapper, String query, List<T> suggestions);

	/**
	 * Return a list of suggestions if they've changed from the last call.<br>
	 * <br>
	 * You can also filter suggestions to match the current query, but the final
	 * filtering step will be performed by {@link #pickAndDecorateSuggestions}.
	 */
	Optional<List<T>> updateSuggestions(TypeWrapper<T> typeWrapper, String query);

	/**
	 * Get the placeholder to show when no suggestions are available, if any.<br>
	 * <br>
	 * By default, this shows a grayed-out "No suggestions" message.
	 */
	default Optional<ITextComponent> getPlaceHolder(TypeWrapper<T> typeWrapper, String query) {
		return Optional.of(new TranslationTextComponent(
		  "simpleconfig.ui.no_suggestions").mergeStyle(TextFormatting.GRAY));
	}
}
