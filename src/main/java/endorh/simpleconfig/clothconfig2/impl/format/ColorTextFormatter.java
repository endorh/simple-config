package endorh.simpleconfig.clothconfig2.impl.format;

import endorh.simpleconfig.clothconfig2.api.ITextFormatter;
import net.minecraft.util.text.*;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ColorTextFormatter implements ITextFormatter {
	private static final Pattern COLOR_PATTERN = Pattern.compile(
	  "^#(?<a>[\\da-fA-F]{2})?(?<r>[\\da-fA-F]{2})(?<g>[\\da-fA-F]{2})(?<b>[\\da-fA-F]{2})$");
	
	@Override public IFormattableTextComponent formatText(String text) {
		Matcher m = COLOR_PATTERN.matcher(text.toUpperCase());
		Color color = Color.parseColor(text.length() == 9? "#" + text.substring(3) : text);
		if (color == null || !m.matches()) return new StringTextComponent(text)
		  .withStyle(TextFormatting.RED).withStyle(TextFormatting.UNDERLINE);
		String a = m.group("a");
		String r = m.group("r");
		String g = m.group("g");
		String b = m.group("b");
		IFormattableTextComponent res = new StringTextComponent("#")
		  .withStyle(Style.EMPTY.withColor(color));
		if (a != null) res.append(
		  new StringTextComponent(a).withStyle(Style.EMPTY.withColor(Color.fromRgb(0xA0A0A0))));
		res.append(new StringTextComponent(r).withStyle(Style.EMPTY.withColor(Color.fromRgb(0xFFBDBD))));
		res.append(new StringTextComponent(g).withStyle(Style.EMPTY.withColor(Color.fromRgb(0xBDFFBD))));
		res.append(new StringTextComponent(b).withStyle(Style.EMPTY.withColor(Color.fromRgb(0xBDBDFF))));
		return res;
	}
	
	@Override public String stripInsertText(String text) {
		return ITextFormatter.filterCharacters(
		  text.toUpperCase(), c -> c == '#' || Character.isDigit(c)
		                           || c >= 'A' && c <= 'F');
	}
}
