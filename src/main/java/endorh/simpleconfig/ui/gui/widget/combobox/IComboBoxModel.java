package endorh.simpleconfig.ui.gui.widget.combobox;

import endorh.simpleconfig.ui.gui.widget.combobox.wrapper.ITypeWrapper;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;
import java.util.Optional;

public interface IComboBoxModel<T> {
	Pair<List<T>, List<Component>> pickAndDecorateSuggestions(
	  ITypeWrapper<T> typeWrapper, String query, List<T> suggestions);
	
	Optional<List<T>> updateSuggestions(ITypeWrapper<T> typeWrapper, String query);
	
	default Optional<Component> getPlaceHolder(ITypeWrapper<T> typeWrapper, String query) {
		return Optional.of(new TranslatableComponent(
		  "simpleconfig.ui.no_suggestions").withStyle(ChatFormatting.GRAY));
	}
}
