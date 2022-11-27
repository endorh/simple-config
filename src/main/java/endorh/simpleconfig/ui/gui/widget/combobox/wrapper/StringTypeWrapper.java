package endorh.simpleconfig.ui.gui.widget.combobox.wrapper;

import endorh.simpleconfig.api.ui.TextFormatter;
import net.minecraft.util.text.Color;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.Style;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class StringTypeWrapper implements TypeWrapper<String> {
	@Override
	public Pair<Optional<String>, Optional<ITextComponent>> parseElement(@NotNull String text) {
		return Pair.of(Optional.of(text), Optional.empty());
	}
	
	@Override public ITextComponent getDisplayName(@NotNull String element) {
		return new StringTextComponent(element);
	}
	
	@Override public @Nullable TextFormatter getTextFormatter() {
		return TextFormatter.plain(Style.EMPTY.setColor(Color.fromInt(0xE0E0E0)));
	}
}
