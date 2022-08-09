package endorh.simpleconfig.core;

import net.minecraft.client.resources.I18n;
import net.minecraft.util.text.*;
import net.minecraft.util.text.ITextProperties.IStyledTextAcceptor;
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
 * <b>Internal utils</b>, see Endorh Util mod for an updated version of these methods.
 */
@Internal public class SimpleConfigTextUtil {
	protected static final Pattern NEW_LINE = Pattern.compile("\\R");
	protected static final Pattern FS_PATTERN = Pattern.compile(
	  "%(?:(?<index>\\d+)\\$)?(?<flags>[-#+ 0,(<]*)?(?<width>\\d+)?(?<precision>\\.\\d+)?(?<t>[tT])?(?<conversion>[a-zA-Z%])");
	protected static final Pattern FORMATTING_CODE_PATTERN = Pattern.compile("§[\\da-fklmnor]");
	
	public static String stripFormattingCodes(String text) {
		return FORMATTING_CODE_PATTERN.matcher(text).replaceAll("");
	}
	
	public static String toTitleCase(String s) {
		return s.substring(0, 1).toUpperCase() + s.substring(1);
	}
	
	/**
	 * Build a translated paragraph.<br>
	 * Accepts a list of translation keys, with optional objects following each of them,
	 * used as arguments.<br>
	 * To use strings as arguments, wrap them in {@link StringTextComponent}s.<br>
	 * @param key First key, must be a translation key.
	 */
	public static List<ITextComponent> paragraph(String key, Object... lines) {
		List<ITextComponent> result = new ArrayList<>();
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
    * Extract formatted subText from {@link ITextComponent}.<br>
    * <b>Internal utils</b>, see Endorh Util mod for an updated version of these methods.
    *
    * @param component {@link ITextComponent} to extract from
    * @param start     Inclusive index to start from
    * @return Formatted component corresponding to the text that would be
    * returned by a call to substring on its contents.
    * @throws StringIndexOutOfBoundsException if start is out of bounds
    */
	@OnlyIn(Dist.CLIENT)
	public static IFormattableTextComponent subText(ITextComponent component, int start) {
		int length = component.getString().length();
		if (start < 0 || start > length) throw new StringIndexOutOfBoundsException(start);
		SubTextVisitor visitor = new SubTextVisitor(start, Integer.MAX_VALUE);
		component.getComponentWithStyle(visitor, Style.EMPTY);
		return visitor.getResult();
	}
	
	/**
	 * Extract formatted subText from {@link ITextComponent}.<br>
	 * <b>Internal utils</b>, see Endorh Util mod for an updated version of these methods.
	 * @param component {@link ITextComponent} to extract from
	 * @param start Inclusive index to start from
	 * @param end Exclusive index to end at
	 * @throws StringIndexOutOfBoundsException if start or end are out of bounds
	 * @return Formatted component corresponding to the text that would be
	 *         returned by a call to substring on its contents.
	 */
	@OnlyIn(Dist.CLIENT)
	public static IFormattableTextComponent subText(ITextComponent component, int start, int end) {
		int length = component.getString().length();
		if (start < 0 || start > length) throw new StringIndexOutOfBoundsException(start);
		if (end < 0 || end > length) throw new StringIndexOutOfBoundsException(end);
		SubTextVisitor visitor = new SubTextVisitor(start, end);
		component.getComponentWithStyle(visitor, Style.EMPTY);
		return visitor.getResult();
	}
	
	@OnlyIn(Dist.CLIENT)
	private static class SubTextVisitor implements IStyledTextAcceptor<Boolean> {
		private final int start;
		private final int end;
		private IFormattableTextComponent result = null;
		private int length = 0;
		
		private SubTextVisitor(int start, int end) {
			this.start = start;
			this.end = end;
		}
		
		private void appendFragment(String fragment, Style style) {
			if (result == null) {
				result = new StringTextComponent(fragment).mergeStyle(style);
			} else result.append(new StringTextComponent(fragment).mergeStyle(style));
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
		
		public IFormattableTextComponent getResult() {
			return result != null? result : StringTextComponent.EMPTY.deepCopy();
		}
	}
	
	/**
	 * Apply a style to a specific range of a component.<br>
	 * <b>Internal utils</b>, see Endorh Util mod for an updated version of these methods.
	 *
	 * @param component Component to style
	 * @param style Style to apply
	 * @param start Inclusive index of styled range
	 * @param end Exclusive index of styled range
	 * @return A new component with the style applied to the specified range
	 * @throws StringIndexOutOfBoundsException if start or end are out of bounds
	 */
	@OnlyIn(Dist.CLIENT) public static IFormattableTextComponent applyStyle(
	  ITextComponent component, TextFormatting style, int start, int end
	) {
		return applyStyle(component, Style.EMPTY.applyFormatting(style), start, end);
	}
	
	/**
	 * Apply a style to a specific range of a component.<br>
	 * <b>Internal utils</b>, see Endorh Util mod for an updated version of these methods.
	 *
	 * @param component Component to style
	 * @param style Style to apply
	 * @param start Inclusive index of styled range
	 * @param end Exclusive index of styled range
	 * @return A new component with the style applied to the specified range
	 * @throws StringIndexOutOfBoundsException if start or end are out of bounds
	 */
	@OnlyIn(Dist.CLIENT) public static IFormattableTextComponent applyStyle(
	  ITextComponent component, Style style, int start, int end
	) {
		int length = component.getString().length();
		checkBounds(start, length);
		checkBounds(end, length);
		if (start == end) return component.deepCopy();
		ApplyStyleVisitor visitor = new ApplyStyleVisitor(style, start, end);
		component.getComponentWithStyle(visitor, Style.EMPTY);
		return visitor.getResult();
	}
	
	private static void checkBounds(int index, int length) {
		if (index < 0 || index > length) throw new StringIndexOutOfBoundsException(index);
	}
	
	@OnlyIn(Dist.CLIENT)
	private static final class ApplyStyleVisitor implements IStyledTextAcceptor<Boolean> {
		private final Style style;
		private final int start;
		private final int end;
		private IFormattableTextComponent result = null;
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
					appendFragment(text.substring(relStart, relEnd), this.style.mergeStyle(style));
				if (relEnd < l)
					appendFragment(text.substring(relEnd), style);
			}
			length += l;
			return Optional.empty();
		}
		
		public IFormattableTextComponent getResult() {
			return result != null? result : StringTextComponent.EMPTY.copyRaw();
		}
		
		private void appendFragment(String fragment, Style style) {
			if (result == null) {
				result = new StringTextComponent(fragment).setStyle(style);
			} else result.append(new StringTextComponent(fragment).setStyle(style));
		}
	}
	
	/**
	 * Separate a translation text component on each line break<br>
	 * Line breaks added by format arguments aren't considered<br>
	 * Only works on the client side<br>
	 * <b>Internal utils</b>, see Endorh Util mod for an updated version of these methods
	 */
	@OnlyIn(Dist.CLIENT)
	@Internal public static List<ITextComponent> splitTtc(String key, Object... args) {
		if (I18n.hasKey(key)) {
			// We add the explicit indexes, so relative/implicit indexes
			//   preserve meaning after splitting
			final String f = addExplicitFormatIndexes(LanguageMap.getInstance().func_230503_a_(key));
			final String[] lines = NEW_LINE.split(f);
			final List<ITextComponent> components = new ArrayList<>();
			for (String line : lines) {
				final Matcher m = FS_PATTERN.matcher(line);
				final IFormattableTextComponent built = new StringTextComponent("");
				int cursor = 0;
				while (m.find()) {
					if (m.group("conversion").equals("%")) {
						built.appendString("%");
						continue;
					}
					final int s = m.start();
					if (s > cursor)
						built.appendString(line.substring(cursor, s));
					// Since we've called addExplicitFormatIndexes,
					//   the following line must not fail
					final int i = Integer.parseInt(m.group("index")) - 1;
					if (i < args.length) {
						// Format options are ignored when the argument is an ITextComponent
						if (args[i] instanceof ITextComponent)
							built.append((ITextComponent) args[i]);
						else built.appendString(String.format(m.group(), args));
					} // else ignore error
					cursor = m.end();
				}
				if (line.length() > cursor)
					built.appendString(line.substring(cursor));
				components.add(built);
			}
			return components;
		} else {
			List<ITextComponent> components = new ArrayList<>();
			components.add(new StringTextComponent(key));
			return components;
		}
	}
	
	protected static final Pattern FS_INDEX_PATTERN = Pattern.compile(
	  "(?<pre>(?<!%)(?:%%)*+%)(?:(?<d>\\d+)\\$)?(?<flags>[-#+ 0,(<]*)(?<pos>[a-zA-Z])");
	protected static String addExplicitFormatIndexes(String fmt) {
		final Matcher m = FS_INDEX_PATTERN.matcher(fmt);
		final StringBuffer sb = new StringBuffer();
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
}
