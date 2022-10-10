package endorh.simpleconfig.api.ui.format;

import endorh.simpleconfig.api.ui.ITextFormatter;
import net.minecraft.network.chat.MutableComponent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class CachedTextFormatter implements ITextFormatter {
	private final ITextFormatter delegate;
	private String lastText;
	private MutableComponent lastFormattedText;
	
	public CachedTextFormatter(ITextFormatter delegate) {
		this.delegate = delegate;
	}
	
	@Override public MutableComponent formatText(String text) {
		if (Objects.equals(lastText, text)) return lastFormattedText;
		lastText = text;
		lastFormattedText = delegate.formatText(text);
		return lastFormattedText;
	}
	
	@Override public @NotNull String stripInsertText(@NotNull String text) {
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
