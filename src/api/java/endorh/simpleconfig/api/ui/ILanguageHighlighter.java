package endorh.simpleconfig.api.ui;

import net.minecraft.util.text.IFormattableTextComponent;
import org.jetbrains.annotations.Nullable;

public interface ILanguageHighlighter extends ITextFormatter {
	IFormattableTextComponent highlight(String text);
	
	@Override @Nullable String closingPair(char typedChar, String context, int caretPos);
	
	@Override boolean shouldSkipClosingPair(char typedChar, String context, int caretPos);
	
	String getLanguage();
}
