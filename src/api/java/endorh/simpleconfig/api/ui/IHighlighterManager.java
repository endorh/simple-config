package endorh.simpleconfig.api.ui;

import org.jetbrains.annotations.Nullable;

public interface IHighlighterManager {
	static IHighlighterManager getInstance() {
		return HighlighterManagerProxy.getHighlighterManager();
	}
	
	@Nullable ILanguageHighlighter getHighlighter(String language);
}
