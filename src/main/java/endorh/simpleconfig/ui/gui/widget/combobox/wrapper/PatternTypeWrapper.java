package endorh.simpleconfig.ui.gui.widget.combobox.wrapper;

import endorh.simpleconfig.ui.api.ITextFormatter;
import net.minecraft.util.text.Color;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.Style;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class PatternTypeWrapper implements ITypeWrapper<Pattern> {
	
	protected int flags;
	
	public PatternTypeWrapper() {
		this(0);
	}
	
	public PatternTypeWrapper(int flags) {
		this.flags = flags;
	}
	
	@Override public Pair<Optional<Pattern>, Optional<ITextComponent>> parseElement(
	  @NotNull String text
	) {
		try {
			return Pair.of(Optional.of(Pattern.compile(text, flags)), Optional.empty());
		} catch (PatternSyntaxException e) {
			return Pair.of(
			  Optional.empty(), Optional.of(new StringTextComponent(e.getLocalizedMessage())));
		}
	}
	
	@Override public ITextComponent getDisplayName(@NotNull Pattern element) {
		return new StringTextComponent(element.pattern());
	}
	
	@Override public @Nullable ITextFormatter getTextFormatter() {
		return ITextFormatter.forLanguageOrDefault("regex", ITextFormatter.plain(
		  Style.EMPTY.withColor(Color.fromRgb(0xFFA080))));
	}
}