package endorh.simpleconfig.core.entry;

import endorh.simpleconfig.ui.api.ITextFormatter;
import endorh.simpleconfig.core.ISimpleConfigEntryHolder;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class PatternEntry extends AbstractSerializableEntry<Pattern, PatternEntry> {
	protected final int flags;
	
	@Internal public PatternEntry(ISimpleConfigEntryHolder parent, String name, Pattern value) {
		super(parent, name, value, Pattern.class);
		this.flags = value.flags();
	}
	
	public static class Builder extends AbstractSerializableEntry.Builder<Pattern, PatternEntry, Builder> {
		public Builder(Pattern value) {
			super(value, Pattern.class);
		}
		
		public Builder(String pattern) {
			this(Pattern.compile(pattern));
		}
		
		public Builder(String pattern, int flags) {
			this(Pattern.compile(pattern, flags));
		}
		
		public Builder flags(int flags) {
			Builder copy = copy();
			try {
				copy.value = Pattern.compile(value.pattern(), flags);
			} catch (PatternSyntaxException e) {
				throw new IllegalArgumentException(
				  "Cannot compile default pattern value with the new flags");
			}
			return copy;
		}
		
		@Override
		protected PatternEntry buildEntry(ISimpleConfigEntryHolder parent, String name) {
			return new PatternEntry(parent, name, value);
		}
		
		@Override protected Builder createCopy() {
			return new Builder(value);
		}
	}
	
	@Override
	protected String serialize(Pattern value) {
		return value.pattern();
	}
	
	@Override
	protected @Nullable Pattern deserialize(String value) {
		try {
			return Pattern.compile(value, flags);
		} catch (PatternSyntaxException e) {
			return null;
		}
	}
	
	@Override protected List<ITextComponent> addExtraTooltip(String value) {
		final List<ITextComponent> extra = super.addExtraTooltip(value);
		if (flags != 0)
			extra.add(0, new TranslationTextComponent(
			  "simpleconfig.config.help.pattern_flags", displayFlags(flags)
			).mergeStyle(TextFormatting.GRAY));
		return extra;
	}
	
	private static final Map<Integer, String> FLAG_NAMES = new HashMap<>();
	static {
		FLAG_NAMES.put(Pattern.UNIX_LINES, "d");
		FLAG_NAMES.put(Pattern.CASE_INSENSITIVE, "i");
		FLAG_NAMES.put(Pattern.COMMENTS, "x");
		FLAG_NAMES.put(Pattern.MULTILINE, "m");
		FLAG_NAMES.put(Pattern.LITERAL, "");
		FLAG_NAMES.put(Pattern.DOTALL, "s");
		FLAG_NAMES.put(Pattern.UNICODE_CASE, "u");
		FLAG_NAMES.put(Pattern.CANON_EQ, "");
		FLAG_NAMES.put(Pattern.UNICODE_CHARACTER_CLASS, "U");
	}
	public static String displayFlags(int flags) {
		if (flags == 0)
			return "";
		StringBuilder f = new StringBuilder("(?");
		for (int k : FLAG_NAMES.keySet()) {
			if ((k & flags) != 0)
				f.append(FLAG_NAMES.get(k));
		}
		if ((flags & Pattern.LITERAL) != 0)
			f.append("+LITERAL");
		if ((flags & Pattern.CANON_EQ) != 0)
			f.append("+CANON_EQ");
		return f + ")";
	}
	
	@Override
	protected Optional<ITextComponent> getErrorMessage(String value) {
		try {
			Pattern.compile(value, flags);
			return super.getErrorMessage(value);
		} catch (PatternSyntaxException e) {
			return Optional.of(new TranslationTextComponent(
			  "simpleconfig.config.error.invalid_pattern",
			  e.getMessage().trim().replace("\r\n", ": ")));
		}
	}
	
	@Override protected ITextFormatter getTextFormatter() {
		return ITextFormatter.forLanguageOrDefault("regex", ITextFormatter.DEFAULT);
	}
}
