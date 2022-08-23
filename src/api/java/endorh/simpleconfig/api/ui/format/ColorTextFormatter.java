package endorh.simpleconfig.api.ui.format;

import endorh.simpleconfig.api.ui.ITextFormatter;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.chat.TextComponent;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ColorTextFormatter implements ITextFormatter {
	private static final Pattern COLOR_PATTERN = Pattern.compile(
	  "^#(?<a>[\\da-fA-F]{2})?(?<r>[\\da-fA-F]{2})(?<g>[\\da-fA-F]{2})(?<b>[\\da-fA-F]{2})$");
	
	@Override public MutableComponent formatText(String text) {
		Matcher m = COLOR_PATTERN.matcher(text.toUpperCase());
		TextColor color = TextColor.parseColor(text.length() == 9? "#" + text.substring(3) : text);
		if (color == null || !m.matches()) return new TextComponent(text)
		  .withStyle(ChatFormatting.RED).withStyle(ChatFormatting.UNDERLINE);
		String a = m.group("a");
		String r = m.group("r");
		String g = m.group("g");
		String b = m.group("b");
		MutableComponent res = new TextComponent("#")
		  .withStyle(Style.EMPTY.withColor(color));
		if (a != null) res.append(
		  new TextComponent(a).withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xA0A0A0))));
		res.append(new TextComponent(r).withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xFFBDBD))));
		res.append(new TextComponent(g).withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xBDFFBD))));
		res.append(new TextComponent(b).withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xBDBDFF))));
		return res;
	}
	
	@Override public String stripInsertText(String text) {
		return ITextFormatter.filterCharacters(
		  text.toUpperCase(),
		  c -> c == '#' || Character.isDigit(c) || c >= 'A' && c <= 'F');
	}
}
