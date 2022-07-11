package endorh.simpleconfig.ui.api;

import endorh.simpleconfig.ui.impl.format.*;
import endorh.simpleconfig.ui.impl.format.CharacterBasedTextFormatter.CharacterMapTextFormatter;
import endorh.simpleconfig.ui.impl.format.CharacterBasedTextFormatter.ICharacterFormatter;
import endorh.simpleconfig.highlight.HighlighterManager;
import endorh.simpleconfig.highlight.HighlighterManager.LanguageHighlighter;
import net.minecraft.util.text.IFormattableTextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.Style;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.function.Predicate;

public interface ITextFormatter {
	static ITextFormatter cached(ITextFormatter formatter) {
		return new CachedTextFormatter(formatter);
	}
	
	static ITextFormatter characterBased(Map<Character, Style> map) {
		return new CharacterMapTextFormatter(map);
	}
	
	static ITextFormatter characterBased(ICharacterFormatter formatter) {
		return new CharacterBasedTextFormatter(formatter);
	}
	
	static ITextFormatter forLanguage(String language) {
		LanguageHighlighter<?> highlighter = HighlighterManager.INSTANCE.getHighlighter(language);
		if (highlighter == null) throw new IllegalArgumentException(
		  "Missing highlighter for language: \"" + language + "\"");
		return highlighter;
	}
	
	static ITextFormatter forLanguageOrDefault(String language, ITextFormatter def) {
		LanguageHighlighter<?> highlighter = HighlighterManager.INSTANCE.getHighlighter(language);
		if (highlighter == null) return def;
		return highlighter;
	}
	
	ITextFormatter DEFAULT = plain(Style.EMPTY);
	
	static ITextFormatter plain(Style style) {
		return text -> new StringTextComponent(text).setStyle(style);
	}
	
	static ITextFormatter numeric(boolean integer) {
		return new NumberTextFormatter(integer);
	}
	
	static ITextFormatter forColor() {
		return new ColorTextFormatter();
	}
	
	static ITextFormatter forResourceLocation() {
		return new ResourceLocationTextFormatter();
	}
	
	static String filterCharacters(String text, Predicate<Character> filter) {
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < text.length(); i++) {
			char c = text.charAt(i);
			if (filter.test(c)) builder.append(c);
		}
		return builder.toString();
	}
	
	IFormattableTextComponent formatText(String text);
	default String stripInsertText(String text) {
		return text;
	}
	default @Nullable String closingPair(char typedChar, String context, int caretPos) {
		return null;
	}
	default boolean shouldSkipClosingPair(char typedChar, String context, int caretPos) {
		return false;
	}
}
