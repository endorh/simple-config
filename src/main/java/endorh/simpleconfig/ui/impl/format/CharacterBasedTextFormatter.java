package endorh.simpleconfig.ui.impl.format;

import endorh.simpleconfig.ui.api.ITextFormatter;
import net.minecraft.util.text.IFormattableTextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.Style;

import java.util.Map;

public class CharacterBasedTextFormatter implements ITextFormatter {
	private final ICharacterFormatter characterFormatter;
	
	public CharacterBasedTextFormatter(ICharacterFormatter formatter) {
		this.characterFormatter = formatter;
	}
	
	protected Style getCharacterStyle(String text, int pos, char chr, Style last) {
		return characterFormatter.getCharacterStyle(text, pos, chr, last);
	}
	
	@Override public IFormattableTextComponent formatText(String text) {
		IFormattableTextComponent res = null;
		Style last = Style.EMPTY;
		StringBuilder builder = new StringBuilder();
		int i = 0;
		for (char c : text.toCharArray()) {
			Style style = getCharacterStyle(text, i, c, last);
			if (style != last) {
				IFormattableTextComponent added =
				  new StringTextComponent(builder.toString()).withStyle(last);
				last = style;
				builder = new StringBuilder().append(c);
				if (res == null) res = added;
				else res.append(added);
			} else builder.append(c);
			i++;
		}
		if (builder.length() > 0) {
			IFormattableTextComponent added =
			  new StringTextComponent(builder.toString()).withStyle(last);
			if (res == null) res = added;
			else res.append(added);
		}
		return res;
	}
	
	@FunctionalInterface public interface ICharacterFormatter {
		ICharacterFormatter DEFAULT = plain(Style.EMPTY);
		static ICharacterFormatter plain(Style style) {
			return (text, pos, chr, last) -> style;
		}
		Style getCharacterStyle(String text, int pos, char chr, Style last);
	}
	
	public static class CharacterMapTextFormatter extends CharacterBasedTextFormatter {
		private final Map<Character, Style> charMap;
		
		public CharacterMapTextFormatter(Map<Character, Style> charMap) {
			super((text, pos, chr, last) -> charMap.getOrDefault(chr, last));
			this.charMap = charMap;
		}
		
		@Override protected Style getCharacterStyle(String text, int pos, char chr, Style last) {
			return charMap.getOrDefault(chr, Style.EMPTY);
		}
	}
}
