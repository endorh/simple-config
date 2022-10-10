package endorh.simpleconfig.api.ui;

import net.minecraft.util.text.IFormattableTextComponent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface ILanguageHighlighter extends ITextFormatter {
	@NotNull IFormattableTextComponent highlight(@NotNull String text);
	
	@Override @Nullable String closingPair(char typedChar, String context, int caretPos);
	
	@Override boolean shouldSkipClosingPair(char typedChar, String context, int caretPos);
	
	@NotNull String getLanguage();
}
