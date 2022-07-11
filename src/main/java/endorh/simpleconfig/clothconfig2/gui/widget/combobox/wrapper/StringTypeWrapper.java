package endorh.simpleconfig.clothconfig2.gui.widget.combobox.wrapper;

import endorh.simpleconfig.clothconfig2.api.ITextFormatter;
import net.minecraft.util.text.Color;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.Style;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class StringTypeWrapper implements ITypeWrapper<String> {
	@Override
	public Pair<Optional<String>, Optional<ITextComponent>> parseElement(@NotNull String text) {
		return Pair.of(Optional.of(text), Optional.empty());
	}
	
	@Override public ITextComponent getDisplayName(@NotNull String element) {
		return new StringTextComponent(element);
	}
	
	@Override public @Nullable ITextFormatter getTextFormatter() {
		return ITextFormatter.plain(Style.EMPTY.withColor(Color.fromRgb(0xE0E0E0)));
	}
}
