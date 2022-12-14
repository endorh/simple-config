package endorh.simpleconfig.ui.gui.widget.combobox.wrapper;

import endorh.simpleconfig.api.ui.TextFormatter;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class StringTypeWrapper implements TypeWrapper<String> {
	@Override
	public Pair<Optional<String>, Optional<Component>> parseElement(@NotNull String text) {
		return Pair.of(Optional.of(text), Optional.empty());
	}
	
	@Override public Component getDisplayName(@NotNull String element) {
		return Component.literal(element);
	}
	
	@Override public @Nullable TextFormatter getTextFormatter() {
		return TextFormatter.plain(Style.EMPTY.withColor(TextColor.fromRgb(0xE0E0E0)));
	}
}
