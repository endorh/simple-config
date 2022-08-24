package endorh.simpleconfig.ui.api.format;

import endorh.simpleconfig.ui.api.ITextFormatter;
import net.minecraft.util.text.IFormattableTextComponent;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class CachedTextFormatter implements ITextFormatter {
	private final ITextFormatter delegate;
	private String lastText;
	private IFormattableTextComponent lastFormattedText;
	
	public CachedTextFormatter(ITextFormatter delegate) {
		this.delegate = delegate;
	}
	
	@Override public IFormattableTextComponent formatText(String text) {
		if (Objects.equals(lastText, text)) return lastFormattedText;
		lastText = text;
		lastFormattedText = delegate.formatText(text);
		return lastFormattedText;
	}
	
	@Override public String stripInsertText(String text) {
		return delegate.stripInsertText(text);
	}
	
	@Override public @Nullable String closingPair(char typedChar, String context, int caretPos) {
		return delegate.closingPair(typedChar, context, caretPos);
	}
	
	@Override public boolean shouldSkipClosingPair(char typedChar, String context, int caretPos) {
		return delegate.shouldSkipClosingPair(typedChar, context, caretPos);
	}
	
	public ITextFormatter getDelegate() {
		return delegate;
	}
}
