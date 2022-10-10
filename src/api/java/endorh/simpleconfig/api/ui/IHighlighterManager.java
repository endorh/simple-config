package endorh.simpleconfig.api.ui;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface IHighlighterManager {
	@NotNull static IHighlighterManager getInstance() {
		return HighlighterManagerProxy.getHighlighterManager();
	}
	
	@Nullable ILanguageHighlighter getHighlighter(String language);
}
