package endorh.simpleconfig.api.ui;

import endorh.simpleconfig.api.ui.format.*;
import endorh.simpleconfig.api.ui.format.CharacterBasedTextFormatter.CharacterMapTextFormatter;
import endorh.simpleconfig.api.ui.format.CharacterBasedTextFormatter.ICharacterFormatter;
import net.minecraft.util.text.IFormattableTextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.Style;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.function.Predicate;

public interface ITextFormatter {
	static @NotNull CachedTextFormatter cached(ITextFormatter formatter) {
		return new CachedTextFormatter(formatter);
	}
	
	static @NotNull CharacterMapTextFormatter characterBased(Map<Character, Style> map) {
		return new CharacterMapTextFormatter(map);
	}
	
	static @NotNull CharacterBasedTextFormatter characterBased(ICharacterFormatter formatter) {
		return new CharacterBasedTextFormatter(formatter);
	}
	
	static @NotNull ILanguageHighlighter forLanguage(String language) {
		ILanguageHighlighter highlighter = IHighlighterManager.getInstance().getHighlighter(language);
		if (highlighter == null) throw new IllegalArgumentException(
		  "Missing highlighter for language: \"" + language + "\"");
		return highlighter;
	}
	
	static @NotNull ITextFormatter forLanguageOrDefault(String language, ITextFormatter def) {
		ILanguageHighlighter highlighter = IHighlighterManager.getInstance().getHighlighter(language);
		if (highlighter == null) return def;
		return highlighter;
	}
	
	ITextFormatter DEFAULT = plain(Style.EMPTY);
	
	static @NotNull ITextFormatter plain(Style style) {
		return text -> new StringTextComponent(text).setStyle(style);
	}
	
	static @NotNull NumberTextFormatter numeric(boolean integer) {
		return new NumberTextFormatter(integer);
	}
	
	static @NotNull ColorTextFormatter forColor() {
		return new ColorTextFormatter();
	}
	
	static @NotNull ResourceLocationTextFormatter forResourceLocation() {
		return new ResourceLocationTextFormatter();
	}
	
	static @NotNull String filterCharacters(String text, Predicate<Character> filter) {
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < text.length(); i++) {
			char c = text.charAt(i);
			if (filter.test(c)) builder.append(c);
		}
		return builder.toString();
	}
	
	IFormattableTextComponent formatText(String text);
	default @NotNull String stripInsertText(@NotNull String text) {
		return text;
	}
	default @Nullable String closingPair(char typedChar, String context, int caretPos) {
		return null;
	}
	default boolean shouldSkipClosingPair(char typedChar, String context, int caretPos) {
		return false;
	}
}
