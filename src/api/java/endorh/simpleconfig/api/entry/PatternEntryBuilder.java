package endorh.simpleconfig.api.entry;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.regex.Pattern;

public interface PatternEntryBuilder
  extends SerializableEntryBuilder<@NotNull Pattern, PatternEntryBuilder> {
	/**
	 * Set the flags used to compile user input into a pattern.<br>
	 * Users will still be able to modify these flags by using the
	 * {@code (?+-flags)} syntax in their patterns.<br>
	 */
	@Contract(pure=true) @NotNull PatternEntryBuilder flags(int flags);
}
