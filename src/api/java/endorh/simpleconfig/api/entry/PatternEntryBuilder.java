package endorh.simpleconfig.api.entry;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.regex.Pattern;

public interface PatternEntryBuilder
  extends SerializableEntryBuilder<@NotNull Pattern, PatternEntryBuilder> {
	@Contract(pure=true) @NotNull PatternEntryBuilder flags(int flags);
}
