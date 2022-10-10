package endorh.simpleconfig.api.ui.format;

import endorh.simpleconfig.api.ui.ITextFormatter;
import net.minecraft.util.text.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class NumberTextFormatter implements ITextFormatter {
	private Style numberStyle = Style.EMPTY.setColor(Color.fromInt(0xA0BDFF));
	private Style punctuationStyle = Style.EMPTY.setColor(Color.fromInt(0xBDA0FF));
	private Style errorStyle = Style.EMPTY.setColor(Color.fromInt(0xFF8080));
	private boolean integer;
	
	public NumberTextFormatter(boolean integer) {
		this.integer = integer;
	}
	
	@Override public IFormattableTextComponent formatText(String text) {
		IFormattableTextComponent res = null;
		char[] chars = text.toCharArray();
		boolean seenSign = false;
		boolean seenDot = integer;
		boolean seenExp = integer;
		int i = 0;
		for (char c : chars) {
			if (Character.isWhitespace(c)) {
				res = append(res, new StringTextComponent(String.valueOf(c)));
			} else if ((c == '-' || c == '+') && !seenSign) {
				res =
				  append(res, new StringTextComponent(String.valueOf(c)).mergeStyle(punctuationStyle));
				seenSign = true;
			} else if (Character.isDigit(c)) {
				res = append(res, new StringTextComponent(String.valueOf(c))).mergeStyle(numberStyle);
				seenSign = true;
			} else if (c == '.' && !seenDot) {
				res = append(res, new StringTextComponent(".").mergeStyle(punctuationStyle));
				seenDot = true;
			} else if (!seenExp && c == 'e' || c == 'E') {
				res = append(res, new StringTextComponent(String.valueOf(c)).mergeStyle(punctuationStyle));
				seenExp = true;
				seenSign = false;
				seenDot = true;
			} else {
				res = append(res, new StringTextComponent(text.substring(i)).mergeStyle(errorStyle));
				break;
			}
			i++;
		}
		return res != null ? res : StringTextComponent.EMPTY.deepCopy();
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
	
	private IFormattableTextComponent append(
	  @Nullable IFormattableTextComponent tc, ITextComponent fragment
	) {
		if (tc == null) return fragment.deepCopy();
		return tc.append(fragment);
	}
	
	@Override public @NotNull String stripInsertText(@NotNull String text) {
		return ITextFormatter.filterCharacters(
		  text, c -> Character.isDigit(c) || c == '-' || c == '+' || c == '_'
		             || !integer && (c == '.' || c == 'e' || c == 'E'));
	}
}
