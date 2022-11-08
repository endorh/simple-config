package endorh.simpleconfig.api.ui;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface IHighlighterManager {
	@NotNull static IHighlighterManager getInstance() {
		return HighlighterManagerProxy.getHighlighterManager();
	}
	
	/**
	 * Get the highlighter for a language.<br>
	 * The only built-in languages are {@code "regex"} and {@code "snbt"}.
	 */
	@Nullable ILanguageHighlighter getHighlighter(String language);
}
