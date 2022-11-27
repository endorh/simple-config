package endorh.simpleconfig.api.ui.format;

import endorh.simpleconfig.api.ui.TextFormatter;
import net.minecraft.network.chat.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Number text formatter which uses a different style for the numeric characters,
 * punctuation, and errors.<br>
 * It also prevents the user from inputting any non-numeric characters,
 * or decimal points if the number should be an integer.
 */
public class NumberTextFormatter implements TextFormatter {
	private Style numberStyle = Style.EMPTY.withColor(TextColor.fromRgb(0xA0BDFF));
	private Style punctuationStyle = Style.EMPTY.withColor(TextColor.fromRgb(0xBDA0FF));
	private Style errorStyle = Style.EMPTY.withColor(TextColor.fromRgb(0xFF8080));
	private boolean integer;
	
	public NumberTextFormatter(boolean integer) {
		this.integer = integer;
	}
	
	@Override public MutableComponent formatText(String text) {
		MutableComponent res = null;
		char[] chars = text.toCharArray();
		boolean seenSign = false;
		boolean seenDot = integer;
		boolean seenExp = integer;
		int i = 0;
		for (char c : chars) {
			if (Character.isWhitespace(c)) {
				res = append(res, new TextComponent(String.valueOf(c)));
			} else if ((c == '-' || c == '+') && !seenSign) {
				res = append(res, new TextComponent(String.valueOf(c)).withStyle(punctuationStyle));
				seenSign = true;
			} else if (Character.isDigit(c)) {
				res = append(res, new TextComponent(String.valueOf(c))).withStyle(numberStyle);
				seenSign = true;
			} else if (c == '.' && !seenDot) {
				res = append(res, new TextComponent(".").withStyle(punctuationStyle));
				seenDot = true;
			} else if (!seenExp && c == 'e' || c == 'E') {
				res = append(res, new TextComponent(String.valueOf(c)).withStyle(punctuationStyle));
				seenExp = true;
				seenSign = false;
				seenDot = true;
			} else {
				res = append(res, new TextComponent(text.substring(i)).withStyle(errorStyle));
				break;
			}
			i++;
		}
		return res != null ? res : TextComponent.EMPTY.copy();
	}
	
	public boolean isInteger() {
		return integer;
	}
	
	public void setInteger(boolean integer) {
		this.integer = integer;
	}
	
	public Style getNumberStyle() {
		return numberStyle;
	}
	
	public void setNumberStyle(Style numberStyle) {
		this.numberStyle = numberStyle;
	}
	
	public Style getPunctuationStyle() {
		return punctuationStyle;
	}
	
	public void setPunctuationStyle(Style punctuationStyle) {
		this.punctuationStyle = punctuationStyle;
	}
	
	public Style getErrorStyle() {
		return errorStyle;
	}
	
	public void setErrorStyle(Style errorStyle) {
		this.errorStyle = errorStyle;
	}
	
	private MutableComponent append(
	  @Nullable MutableComponent tc, Component fragment
	) {
		if (tc == null) return fragment.copy();
		return tc.append(fragment);
	}
	
	@Override public @NotNull String stripInsertText(@NotNull String text) {
		return TextFormatter.filterCharacters(
		  text, c -> Character.isDigit(c) || c == '-' || c == '+' || c == '_'
		             || !integer && (c == '.' || c == 'e' || c == 'E'));
	}
}
