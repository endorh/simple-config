package endorh.simpleconfig.core;

import net.minecraft.client.resources.I18n;
import net.minecraft.util.text.IFormattableTextComponent;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.LanguageMap;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.ApiStatus.Internal;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @deprecated Internal utils, see Endorh Util mod for an updated version of these methods
 */
@Internal @Deprecated public class SimpleConfigTextUtil {
	protected static final Pattern NEW_LINE = Pattern.compile("\\R");
	protected static final Pattern FS_PATTERN = Pattern.compile(
	  "%(?:(?<index>\\d+)\\$)?(?<flags>[-#+ 0,(<]*)?(?<width>\\d+)?(?<precision>\\.\\d+)?(?<t>[tT])?(?<conversion>[a-zA-Z%])");

	/**
	 * Separate a translation text component on each line break<br>
	 * Line breaks added by format arguments aren't considered<br>
	 * Only works on the client side<br>
	 * @deprecated Internal utils, see Endorh Util mod for an updated version of these methods
	 */
	@OnlyIn(Dist.CLIENT)
	@Internal @Deprecated public static List<ITextComponent> splitTtc(String key, Object... args) {
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
	
	// For some reason, using a lookahead for the last character class
	//   makes the pattern fail if at the start of the sample
	//   It's actually a bug from IntelliJ's "Check RegExp" intention
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
