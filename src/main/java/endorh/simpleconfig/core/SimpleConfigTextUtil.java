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

/**
 * <b>Internal utils</b>, see Endorh Util mod for an updated version of these methods.
 */
@Internal public class SimpleConfigTextUtil {
	protected static final Pattern NEW_LINE = Pattern.compile("\\R");
	protected static final Pattern FS_PATTERN = Pattern.compile(
	  "%(?:(?<index>\\d+)\\$)?(?<flags>[-#+ 0,(<]*)?(?<width>\\d+)?(?<precision>\\.\\d+)?(?<t>[tT])?(?<conversion>[a-zA-Z%])");
	
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
	public static IFormattableTextComponent subText(ITextComponent component, int start) {
		int length = component.getString().length();
		if (start < 0 || start > length) throw new StringIndexOutOfBoundsException(start);
		SubTextVisitor visitor = new SubTextVisitor(start, Integer.MAX_VALUE);
		component.visit(visitor, Style.EMPTY);
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
	public static IFormattableTextComponent subText(ITextComponent component, int start, int end) {
		int length = component.getString().length();
		if (start < 0 || start > length) throw new StringIndexOutOfBoundsException(start);
		if (end < 0 || end > length) throw new StringIndexOutOfBoundsException(end);
		SubTextVisitor visitor = new SubTextVisitor(start, end);
		component.visit(visitor, Style.EMPTY);
		return visitor.getResult();
	}
	
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
				result = new StringTextComponent(fragment).withStyle(style);
			} else result.append(new StringTextComponent(fragment).withStyle(style));
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
			return result != null? result : StringTextComponent.EMPTY.copy();
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
		if (I18n.exists(key)) {
			// We add the explicit indexes, so relative/implicit indexes
			//   preserve meaning after splitting
			final String f = addExplicitFormatIndexes(LanguageMap.getInstance().getOrDefault(key));
			final String[] lines = NEW_LINE.split(f);
			final List<ITextComponent> components = new ArrayList<>();
			for (String line : lines) {
				final Matcher m = FS_PATTERN.matcher(line);
				final IFormattableTextComponent built = new StringTextComponent("");
				int cursor = 0;
				while (m.find()) {
					if (m.group("conversion").equals("%")) {
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
						// Format options are ignored when the argument is an ITextComponent
						if (args[i] instanceof ITextComponent)
							built.append((ITextComponent) args[i]);
						else built.append(String.format(m.group(), args));
					} // else ignore error
					cursor = m.end();
				}
				if (line.length() > cursor)
					built.append(line.substring(cursor));
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
