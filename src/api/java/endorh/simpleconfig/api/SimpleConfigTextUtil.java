package endorh.simpleconfig.api;

import com.google.common.collect.Lists;
import net.minecraft.ChatFormatting;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText.StyledContentConsumer;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.Math.max;
import static java.lang.Math.min;

/**
 * <b>Internal utils</b>, see LazuLib mod for an updated version of these methods.
 */
@Internal public class SimpleConfigTextUtil {
	protected static final Pattern NEW_LINE = Pattern.compile("\\R");
	protected static final Pattern FS_PATTERN = Pattern.compile(
	  "%(?:(?<index>\\d+)\\$)?(?<flags>[-#+ 0,(<]*)?(?<width>\\d+)?(?<precision>\\.\\d+)?(?<t>[tT])?(?<conversion>[a-zA-Z%])");
	protected static final Pattern FORMATTING_CODE_PATTERN = Pattern.compile("§[\\da-fklmnor]");
	
	/**
	 * Strip formatting codes (§[0-9a-fklmnor]) from a string
	 */
	public static @NotNull String stripFormattingCodes(@NotNull String text) {
		return FORMATTING_CODE_PATTERN.matcher(text).replaceAll("");
	}
	
	/**
	 * Make the first letter of a string uppercase.
	 */
	public static @NotNull String toTitleCase(@NotNull String s) {
		return s.substring(0, 1).toUpperCase() + s.substring(1);
	}
	
	/**
	 * Build a translated paragraph.<br>
	 * Accepts a list of translation keys, with optional objects following each of them,
	 * used as arguments.<br>
	 * To use strings as arguments, wrap them in {@link Component}s.<br>
	 * @param key First key, must be a translation key.
	 */
	public static @NotNull List<Component> paragraph(@NotNull String key, Object... lines) {
		List<Component> result = new ArrayList<>();
		List<Object> args = new ArrayList<>();
		for (Object line: lines) {
			if (line instanceof String) {
				result.addAll(splitTtc(key, args.toArray()));
				key = (String) line;
				args.clear();
			} else args.add(line);
		}
		result.addAll(splitTtc(key, args.toArray()));
		return result;
	}
	
	/**
    * Extract formatted subText from {@link Component}.<br>
    * <b>Internal utils</b>, see the LazuLib mod for an updated version of these methods.
    *
    * @param component {@link Component} to extract from
    * @param start     Inclusive index to start from
    * @return Formatted component corresponding to the text that would be
    * returned by a call to substring on its contents.
    * @throws StringIndexOutOfBoundsException if start is out of bounds
    */
	@OnlyIn(Dist.CLIENT)
	public static @NotNull MutableComponent subText(@NotNull Component component, int start) {
		int length = component.getString().length();
		if (start < 0 || start > length) throw new StringIndexOutOfBoundsException(start);
		SubTextVisitor visitor = new SubTextVisitor(start, Integer.MAX_VALUE);
		component.visit(visitor, Style.EMPTY);
		return visitor.getResult();
	}
	
	/**
	 * Extract formatted subText from {@link Component}.<br>
	 * <b>Internal utils</b>, see the LazuLib mod for an updated version of these methods.
	 * @param component {@link Component} to extract from
	 * @param start Inclusive index to start from
	 * @param end Exclusive index to end at
	 * @throws StringIndexOutOfBoundsException if start or end are out of bounds
	 * @return Formatted component corresponding to the text that would be
	 *         returned by a call to substring on its contents.
	 */
	@OnlyIn(Dist.CLIENT)
	public static @NotNull MutableComponent subText(@NotNull Component component, int start, int end) {
		int length = component.getString().length();
		if (start < 0 || start > length) throw new StringIndexOutOfBoundsException(start);
		if (end < 0 || end > length) throw new StringIndexOutOfBoundsException(end);
		SubTextVisitor visitor = new SubTextVisitor(start, end);
		component.visit(visitor, Style.EMPTY);
		return visitor.getResult();
	}
	
	@OnlyIn(Dist.CLIENT)
	private static class SubTextVisitor implements StyledContentConsumer<Boolean> {
		private final int start;
		private final int end;
		private MutableComponent result = null;
		private int length = 0;
		
		private SubTextVisitor(int start, int end) {
			this.start = start;
			this.end = end;
		}
		
		private void appendFragment(String fragment, Style style) {
			if (result == null) {
				result = Component.literal(fragment).withStyle(style);
			} else result.append(Component.literal(fragment).withStyle(style));
		}
		
		@Override public @NotNull Optional<Boolean> accept(
		  @NotNull Style style, @NotNull String contents
		) {
			int l = contents.length();
			if (length + l > end) {
				appendFragment(contents.substring(max(0, start - length), end - length), style);
				return Optional.of(true);
			} else if (length + l >= start) {
				appendFragment(contents.substring(max(0, start - length)), style);
			}
			length += l;
			return Optional.empty();
		}
		
		public MutableComponent getResult() {
			return result != null? result : Component.empty();
		}
	}
	
	/**
	 * Apply a style to a specific range of a component.<br>
	 * <b>Internal utils</b>, see the LazuLib mod for an updated version of these methods.
	 *
	 * @param component Component to style
	 * @param style Style to apply
	 * @param start Inclusive index of styled range
	 * @param end Exclusive index of styled range
	 * @return A new component with the style applied to the specified range
	 * @throws StringIndexOutOfBoundsException if start or end are out of bounds
	 */
	@OnlyIn(Dist.CLIENT) public static @NotNull MutableComponent applyStyle(
	  @NotNull Component component, ChatFormatting style, int start, int end
	) {
		return applyStyle(component, Style.EMPTY.applyFormat(style), start, end);
	}
	
	/**
	 * Apply a style to a specific range of a component.<br>
	 * <b>Internal utils</b>, see the LazuLib mod for an updated version of these methods.
	 *
	 * @param component Component to style
	 * @param style Style to apply
	 * @param start Inclusive index of styled range
	 * @param end Exclusive index of styled range
	 * @return A new component with the style applied to the specified range
	 * @throws StringIndexOutOfBoundsException if start or end are out of bounds
	 */
	@OnlyIn(Dist.CLIENT) public static @NotNull MutableComponent applyStyle(
	  @NotNull Component component, Style style, int start, int end
	) {
		int length = component.getString().length();
		checkBounds(start, length);
		checkBounds(end, length);
		if (start == end) return component.copy();
		ApplyStyleVisitor visitor = new ApplyStyleVisitor(style, start, end);
		component.visit(visitor, Style.EMPTY);
		return visitor.getResult();
	}
	
	private static void checkBounds(int index, int length) {
		if (index < 0 || index > length) throw new StringIndexOutOfBoundsException(index);
	}
	
	@OnlyIn(Dist.CLIENT)
	private static final class ApplyStyleVisitor implements StyledContentConsumer<Boolean> {
		private final Style style;
		private final int start;
		private final int end;
		private MutableComponent result = null;
		private int length = 0;
		
		private ApplyStyleVisitor(Style style, int start, int end) {
			this.style = style;
			this.start = start;
			this.end = end;
		}
		
		@Override
		public @NotNull Optional<Boolean> accept(@NotNull Style style, @NotNull String text) {
			int l = text.length();
			if (l + length <= start || length >= end) {
				appendFragment(text, style);
			} else {
				int relStart = max(0, start - length);
				int relEnd = min(l, end - length);
				if (relStart > 0)
					appendFragment(text.substring(0, relStart), style);
				if (relEnd > relStart)
					appendFragment(text.substring(relStart, relEnd), this.style.applyTo(style));
				if (relEnd < l)
					appendFragment(text.substring(relEnd), style);
			}
			length += l;
			return Optional.empty();
		}
		
		public MutableComponent getResult() {
			return result != null? result : Component.empty();
		}
		
		private void appendFragment(String fragment, Style style) {
			if (result == null) {
				result = Component.literal(fragment).setStyle(style);
			} else result.append(Component.literal(fragment).setStyle(style));
		}
	}
	
	/**
	 * Separate a translation text component on each line break<br>
	 * Line breaks added by format arguments aren't considered<br>
	 * Only works on the client side<br>
	 * <b>Internal utils</b>, see the LazuLib mod for an updated version of these methods
	 */
	@OnlyIn(Dist.CLIENT)
	@Internal public static @NotNull List<Component> splitTtc(@NotNull String key, Object... args) {
		List<Component> ls = optSplitTtc(key, args);
		if (ls.isEmpty()) ls.add(Component.literal(key));
		return ls;
	}
	
	/**
	 * Separate a translation text component on each line break<br>
	 * Line breaks added by format arguments aren't considered<br>
	 * Only works on the client side<br>
	 * <b>Internal utils</b>, see the LazuLib mod for an updated version of these methods
	 */
	@OnlyIn(Dist.CLIENT)
	@Internal public static @NotNull List<Component> optSplitTtc(
	  @NotNull String key, Object... args
	) {
		if (I18n.exists(key)) {
			// We add the explicit indexes, so relative/implicit indexes
			//   preserve meaning after splitting
			final String f = addExplicitFormatIndexes(Language.getInstance().getOrDefault(key));
			final String[] lines = NEW_LINE.split(f);
			final List<Component> components = new ArrayList<>();
			for (String line : lines) {
				final Matcher m = FS_PATTERN.matcher(line);
				final MutableComponent built = Component.literal("");
				int cursor = 0;
				while (m.find()) {
					if ("%".equals(m.group("conversion"))) {
						built.append("%");
						continue;
					}
					final int s = m.start();
					if (s > cursor)
						built.append(line.substring(cursor, s));
					// Since we've called addExplicitFormatIndexes,
					//   the following line must not fail
					final int i = Integer.parseInt(m.group("index")) - 1;
					if (i < args.length) {
						// Format options are ignored when the argument is a Component
						if (args[i] instanceof Component)
							built.append((Component) args[i]);
						else built.append(String.format(m.group(), args));
					} // else ignore error
					cursor = m.end();
				}
				if (line.length() > cursor)
					built.append(line.substring(cursor));
				components.add(built);
			}
			return components;
		} else return Lists.newArrayList();
	}
	
	protected static final Pattern FS_INDEX_PATTERN = Pattern.compile(
	  "(?<pre>(?<!%)(?:%%)*+%)(?:(?<d>\\d+)\\$)?(?<flags>[-#+ 0,(<]*)(?<pos>[a-zA-Z])");
	protected static String addExplicitFormatIndexes(String fmt) {
		final Matcher m = FS_INDEX_PATTERN.matcher(fmt);
		final StringBuilder sb = new StringBuilder();
		int last_gen = -1; // Counter for generated implicit indexes
		int last = -1; // Last found index, for relative indexing
		while (m.find()) {
			final String g = m.group("d");
			final String f = m.group("flags");
			String rep = g + f;
			if (f.contains("<")) { // Relative index (last index)
				if (last >= 0)
					rep = (last + 1) + "\\$" + f.replace("<", "");
			} else if (g == null || g.isEmpty()) { // Implicit index
				last_gen++;
				last = last_gen;
				rep = (last_gen + 1) + "\\$" + f;
			} else { // Explicit index
				last = Integer.parseInt(g) - 1;
				rep += "\\$";
			}
			m.appendReplacement(sb, "${pre}" + rep + "${pos}");
		}
		m.appendTail(sb);
		return sb.toString();
	}
	
	/**
	 * Convert a {@link FormattedCharSequence} into a {@link Component}
	 */
	@Internal public static Component asComponent(FormattedCharSequence sequence) {
		return new FormattedCharSequenceConverter(sequence).convert();
	}
	
	private static class FormattedCharSequenceConverter {
		private Style style = Style.EMPTY;
		private MutableComponent component = null;
		private StringBuilder builder = new StringBuilder();
		private final FormattedCharSequence sequence;
		
		public FormattedCharSequenceConverter(FormattedCharSequence sequence) {
			this.sequence = sequence;
		}
		
		public MutableComponent convert() {
			sequence.accept(this::append);
			if (builder.length() > 0) flush();
			return component != null? component : Component.empty();
		}
		
		private boolean append(int width, Style style, int ch) {
			if (this.style != style) {
				if (builder.length() > 0) {
					flush();
					builder = new StringBuilder();
				}
				this.style = style;
			}
			builder.appendCodePoint(ch);
			return true;
		}
		
		private void flush() {
			if (component == null) {
				component = Component.literal(builder.toString()).setStyle(style);
			} else component.append(Component.literal(builder.toString()).setStyle(style));
		}
	}
}
