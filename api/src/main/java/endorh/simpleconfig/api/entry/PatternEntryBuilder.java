package endorh.simpleconfig.api.entry;

import org.jetbrains.annotations.Contract;

import java.util.regex.Pattern;

public interface PatternEntryBuilder extends SerializableEntryBuilder<Pattern, PatternEntryBuilder> {
	@Contract(pure=true) PatternEntryBuilder flags(int flags);
}
