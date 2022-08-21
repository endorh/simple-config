package endorh.simpleconfig.ui.gui.widget.combobox;

import endorh.simpleconfig.ui.gui.widget.combobox.wrapper.ITypeWrapper;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;
import java.util.Optional;

public interface IComboBoxModel<T> {
	Pair<List<T>, List<ITextComponent>> pickAndDecorateSuggestions(
	  ITypeWrapper<T> typeWrapper, String query, List<T> suggestions);
	
	Optional<List<T>> updateSuggestions(ITypeWrapper<T> typeWrapper, String query);
	
	default Optional<ITextComponent> getPlaceHolder(ITypeWrapper<T> typeWrapper, String query) {
		return Optional.of(new TranslationTextComponent(
		  "simpleconfig.ui.no_suggestions").withStyle(TextFormatting.GRAY));
	}
}
