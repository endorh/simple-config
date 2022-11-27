package endorh.simpleconfig.api.ui.format;

import endorh.simpleconfig.api.ui.TextFormatter;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import org.jetbrains.annotations.NotNull;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Color text formatter in {@code #AARRGGBB} or {@code #RRGGBB} format.
 */
public class ColorTextFormatter implements TextFormatter {
	private static final Pattern COLOR_PATTERN = Pattern.compile(
	  "^#(?<a>[\\da-fA-F]{2})?(?<r>[\\da-fA-F]{2})(?<g>[\\da-fA-F]{2})(?<b>[\\da-fA-F]{2})$");
	
	@Override public MutableComponent formatText(String text) {
		Matcher m = COLOR_PATTERN.matcher(text.toUpperCase());
		TextColor color = TextColor.parseColor(text.length() == 9? "#" + text.substring(3) : text);
		if (color == null || !m.matches()) return Component.literal(text)
		  .withStyle(ChatFormatting.RED).withStyle(ChatFormatting.UNDERLINE);
		String a = m.group("a");
		String r = m.group("r");
		String g = m.group("g");
		String b = m.group("b");
		MutableComponent res = Component.literal("#")
		  .withStyle(Style.EMPTY.withColor(color));
		if (a != null) res.append(
		  Component.literal(a).withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xA0A0A0))));
		res.append(Component.literal(r).withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xFFBDBD))));
		res.append(Component.literal(g).withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xBDFFBD))));
		res.append(Component.literal(b).withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xBDBDFF))));
		return res;
	}
	
	@Override public @NotNull String stripInsertText(@NotNull String text) {
		return TextFormatter.filterCharacters(
		  text.toUpperCase(),
		  c -> c == '#' || Character.isDigit(c) || c >= 'A' && c <= 'F');
	}
}
