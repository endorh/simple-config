package endorh.simpleconfig.api.ui;

import net.minecraft.util.text.IFormattableTextComponent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Text formatter for a language.<br>
 */
public interface LanguageHighlighter extends TextFormatter {
	@NotNull IFormattableTextComponent highlight(@NotNull String text);
	
	@Override @Nullable String closingPair(char typedChar, String context, int caretPos);
	
	@Override boolean shouldSkipClosingPair(char typedChar, String context, int caretPos);
	
	@NotNull String getLanguage();
}
