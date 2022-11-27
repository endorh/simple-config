package endorh.simpleconfig.ui.gui.widget.combobox;

import endorh.simpleconfig.ui.gui.widget.combobox.wrapper.TypeWrapper;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;
import java.util.Optional;

public interface IComboBoxModel<T> {
	Pair<List<T>, List<Component>> pickAndDecorateSuggestions(
	  TypeWrapper<T> typeWrapper, String query, List<T> suggestions);
	
	Optional<List<T>> updateSuggestions(TypeWrapper<T> typeWrapper, String query);
	
	default Optional<Component> getPlaceHolder(TypeWrapper<T> typeWrapper, String query) {
		return Optional.of(Component.translatable("simpleconfig.ui.no_suggestions").withStyle(ChatFormatting.GRAY));
	}
}
