package endorh.simple_config.clothconfig2.impl;

import org.jetbrains.annotations.ApiStatus.Internal;

import java.util.List;
import java.util.regex.Pattern;

public interface ISeekableComponent {
	List<ISeekableComponent> search(Pattern query);
	boolean isFocusedMatch();
	@Internal void setFocusedMatch(boolean isFocusedMatch);
}
