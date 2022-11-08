package endorh.simpleconfig.api.ui.format;

import endorh.simpleconfig.api.ui.ITextFormatter;
import net.minecraft.util.text.*;
import org.jetbrains.annotations.NotNull;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Color text formatter in {@code #AARRGGBB} or {@code #RRGGBB} format.
 */
public class ColorTextFormatter implements ITextFormatter {
	private static final Pattern COLOR_PATTERN = Pattern.compile(
	  "^#(?<a>[\\da-fA-F]{2})?(?<r>[\\da-fA-F]{2})(?<g>[\\da-fA-F]{2})(?<b>[\\da-fA-F]{2})$");
	
	@Override public IFormattableTextComponent formatText(String text) {
		Matcher m = COLOR_PATTERN.matcher(text.toUpperCase());
		Color color = Color.fromHex(text.length() == 9? "#" + text.substring(3) : text);
		if (color == null || !m.matches()) return new StringTextComponent(text)
		  .mergeStyle(TextFormatting.RED).mergeStyle(TextFormatting.UNDERLINE);
		String a = m.group("a");
		String r = m.group("r");
		String g = m.group("g");
		String b = m.group("b");
		IFormattableTextComponent res = new StringTextComponent("#")
		  .mergeStyle(Style.EMPTY.setColor(color));
		if (a != null) res.append(
		  new StringTextComponent(a).mergeStyle(Style.EMPTY.setColor(Color.fromInt(0xA0A0A0))));
		res.append(new StringTextComponent(r).mergeStyle(Style.EMPTY.setColor(Color.fromInt(0xFFBDBD))));
		res.append(new StringTextComponent(g).mergeStyle(Style.EMPTY.setColor(Color.fromInt(0xBDFFBD))));
		res.append(new StringTextComponent(b).mergeStyle(Style.EMPTY.setColor(Color.fromInt(0xBDBDFF))));
		return res;
	}
	
	@Override public @NotNull String stripInsertText(@NotNull String text) {
		return ITextFormatter.filterCharacters(
		  text.toUpperCase(), c -> c == '#' || Character.isDigit(c)
		                           || c >= 'A' && c <= 'F');
	}
}
