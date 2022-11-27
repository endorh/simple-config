package endorh.simpleconfig.api.ui;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface HighlighterManager {
	@NotNull static HighlighterManager getInstance() {
		return HighlighterManagerProxy.getHighlighterManager();
	}
	
	/**
	 * Get the highlighter for a language.<br>
	 * The only built-in languages are {@code "regex"} and {@code "snbt"}.
	 */
	@Nullable LanguageHighlighter getHighlighter(String language);
}
