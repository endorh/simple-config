package endorh.simpleconfig.api.ui.format;

import endorh.simpleconfig.api.ui.ITextFormatter;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextComponent;

import java.util.Map;

public class CharacterBasedTextFormatter implements ITextFormatter {
	private final ICharacterFormatter characterFormatter;
	
	public CharacterBasedTextFormatter(ICharacterFormatter formatter) {
		this.characterFormatter = formatter;
	}
	
	protected Style getCharacterStyle(String text, int pos, char chr, Style last) {
		return characterFormatter.getCharacterStyle(text, pos, chr, last);
	}
	
	@Override public MutableComponent formatText(String text) {
		MutableComponent res = null;
		Style last = Style.EMPTY;
		StringBuilder builder = new StringBuilder();
		int i = 0;
		for (char c : text.toCharArray()) {
			Style style = getCharacterStyle(text, i, c, last);
			if (style != last) {
				MutableComponent added =
				  new TextComponent(builder.toString()).withStyle(last);
				last = style;
				builder = new StringBuilder().append(c);
				if (res == null) res = added;
				else res.append(added);
			} else builder.append(c);
			i++;
		}
		if (builder.length() > 0) {
			MutableComponent added =
			  new TextComponent(builder.toString()).withStyle(last);
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
