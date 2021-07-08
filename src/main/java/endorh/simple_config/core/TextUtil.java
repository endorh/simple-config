package endorh.simple_config.core;

import net.minecraft.client.resources.I18n;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.LanguageMap;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TextUtil {
	/**
	 * Separate a translation text component on each line break<br>
	 * Line breaks added by format arguments aren't considered<br>
	 * <b>This must only be called on the client side</b>
	 */
	@OnlyIn(Dist.CLIENT)
	public static List<ITextComponent> splitTtc(String key, Object... args) {
		if (I18n.hasKey(key)) {
			final String f = addExplicitFormatIndexes(LanguageMap.getInstance().func_230503_a_(key));
			final String[] lines = f.split("\\R");
			List<ITextComponent> components = new ArrayList<>();
			for (String line : lines)
				components.add(new StringTextComponent(String.format(line, args)));
			return components;
		} else {
			List<ITextComponent> components = new ArrayList<>();
			components.add(new StringTextComponent(key));
			return components;
		}
	}
	
	private static final Pattern fsIndexPattern = Pattern.compile(
	  "(?<=%)(?:(\\d+|<)\\$)?(?=[^%])");
	private static String addExplicitFormatIndexes(String fmt) {
		final Matcher m = fsIndexPattern.matcher(fmt);
		final StringBuffer sb = new StringBuffer();
		int last_gen = -1;
		int last = -1;
		while (m.find()) {
			final String g = m.group(1);
			String rep = g;
			if (g == null || g.isEmpty()) {
				last_gen++;
				last = last_gen;
				rep = (last_gen + 1) + "\\$";
			} else if (g.equals("<")) {
				if (last >= 0)
					rep = (last + 1) + "\\$";
			} else last = Integer.parseInt(g) - 1;
			m.appendReplacement(sb, rep);
		}
		m.appendTail(sb);
		return sb.toString();
	}
}
