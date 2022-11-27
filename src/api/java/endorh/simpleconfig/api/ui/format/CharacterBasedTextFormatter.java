package endorh.simpleconfig.api.ui.format;

import endorh.simpleconfig.api.ui.TextFormatter;
import net.minecraft.util.text.IFormattableTextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.Style;

import java.util.Map;

public class CharacterBasedTextFormatter implements TextFormatter {
	private final CharacterFormatter characterFormatter;
	
	public CharacterBasedTextFormatter(CharacterFormatter formatter) {
		this.characterFormatter = formatter;
	}
	
	protected Style getCharacterStyle(String text, int pos, char chr, Style last) {
		return characterFormatter.getCharacterStyle(text, pos, chr, last);
	}
	
	/**
	 * Format a text using the formatting rules.<br>
	 * Merges adjacent characters with the same style under the same
	 * component.
	 */
	@Override public IFormattableTextComponent formatText(String text) {
		IFormattableTextComponent res = null;
		Style last = Style.EMPTY;
		StringBuilder builder = new StringBuilder();
		int i = 0;
		for (char c : text.toCharArray()) {
			Style style = getCharacterStyle(text, i, c, last);
			if (style != last) {
				IFormattableTextComponent added =
				  new StringTextComponent(builder.toString()).mergeStyle(last);
				last = style;
				builder = new StringBuilder().append(c);
				if (res == null) res = added;
				else res.append(added);
			} else builder.append(c);
			i++;
		}
		if (builder.length() > 0) {
			IFormattableTextComponent added =
			  new StringTextComponent(builder.toString()).mergeStyle(last);
			if (res == null) res = added;
			else res.append(added);
		}
		return res;
	}
	
	/**
	 * Character-based formatter.<br>
	 * For each character computes a style.
	 */
	@FunctionalInterface public interface CharacterFormatter {
		CharacterFormatter DEFAULT = plain(Style.EMPTY);
		
		/**
		 * Create a character-based format that assigns the same style to all characters.
		 */
		static CharacterFormatter plain(Style style) {
			return (text, pos, chr, last) -> style;
		}
		Style getCharacterStyle(String text, int pos, char chr, Style last);
	}
	
	/**
	 * Character-based formatter implemented with a map of styles.
	 */
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
