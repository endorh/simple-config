package endorh.simpleconfig.api.ui;

import endorh.simpleconfig.api.ui.format.*;
import endorh.simpleconfig.api.ui.format.CharacterBasedTextFormatter.CharacterMapTextFormatter;
import endorh.simpleconfig.api.ui.format.CharacterBasedTextFormatter.ICharacterFormatter;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.function.Predicate;

/**
 * Text formatter, used to format user-input depending on the context.
 */
public interface ITextFormatter {
	/**
	 * Cache the another formatter.<br>
	 * Only remembers the last value.<br>
	 * Useful for rendering loops where the same text may be formatted multiple times.
	 */
	static @NotNull CachedTextFormatter cached(ITextFormatter formatter) {
		return new CachedTextFormatter(formatter);
	}
	
	/**
	 * Character-based formatter.<br>
	 * Uses a map from characters to styles.<br>
	 * Characters not in the map won't be styled, and can be styled by merging
	 * tho style of the whole component.
	 */
	static @NotNull CharacterMapTextFormatter characterBased(Map<Character, Style> map) {
		return new CharacterMapTextFormatter(map);
	}
	
	/**
	 * Character-based formatter using an {@link ICharacterFormatter}.
	 */
	static @NotNull CharacterBasedTextFormatter characterBased(ICharacterFormatter formatter) {
		return new CharacterBasedTextFormatter(formatter);
	}
	
	/**
	 * Text formatter for a registered language.<br>
	 * The only built-in languages are {@code "regex"} and {@code "snbt"}
	 */
	static @NotNull ILanguageHighlighter forLanguage(String language) {
		ILanguageHighlighter highlighter = IHighlighterManager.getInstance().getHighlighter(language);
		if (highlighter == null) throw new IllegalArgumentException(
		  "Missing highlighter for language: \"" + language + "\"");
		return highlighter;
	}
	
	/**
	 * Text formatter for a registered language, or a fallback text formatter.<br>
	 */
	static @NotNull ITextFormatter forLanguageOrDefault(String language, ITextFormatter def) {
		ILanguageHighlighter highlighter = IHighlighterManager.getInstance().getHighlighter(language);
		if (highlighter == null) return def;
		return highlighter;
	}
	
	/**
	 * Default text formatter, which doesn't apply any style.
	 */
	ITextFormatter DEFAULT = plain(Style.EMPTY);
	
	/**
	 * Plain text formatter, which applies the given style to the whole text.
	 */
	static @NotNull ITextFormatter plain(Style style) {
		return text -> Component.literal(text).setStyle(style);
	}
	
	/**
	 * Numeric text formatter, which applies a blue color to numbers,
	 * and highlights as errors non-numbers.<br>
	 * @param integer whether non-integer numbers should be highlighted as errors
	 */
	static @NotNull NumberTextFormatter numeric(boolean integer) {
		return new NumberTextFormatter(integer);
	}
	
	/**
	 * Color text formatter in {@code #AARRGGBB} or {@code #RRGGBB} formats.<br>
	 * Highlights each component with an identifying tint, and also highlights errors.
	 */
	static @NotNull ColorTextFormatter forColor() {
		return new ColorTextFormatter();
	}
	
	/**
	 * Text formatter for resource locations.<br>
	 * Highlights the namespace and path separately, and also errors.
	 */
	static @NotNull ResourceLocationTextFormatter forResourceLocation() {
		return new ResourceLocationTextFormatter();
	}
	
	/**
	 * Filter a string by removing the characters that don't match a given predicate.
	 */
	static @NotNull String filterCharacters(String text, Predicate<Character> filter) {
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < text.length(); i++) {
			char c = text.charAt(i);
			if (filter.test(c)) builder.append(c);
		}
		return builder.toString();
	}
	
	/**
	 * Format text according to the rules of this formatter.
	 */
	MutableComponent formatText(String text);
	
	/**
	 * Filter user input inserted in a text field using this formatter.<br>
	 */
	default @NotNull String stripInsertText(@NotNull String text) {
		return text;
	}
	
	/**
	 * Return the closing pair of a character inputted in a text field using this formatter.<br>
	 * For example, insert {@code ")"} after {@code "("} is typed.
	 */
	default @Nullable String closingPair(char typedChar, String context, int caretPos) {
		return null;
	}
	
	/**
	 * Return whether a character inputted in a text field using this formatter
	 * should be skipped as the closing pair of a previous opening character.<br>
	 * For example, skip a {@code ")"} character if typed before one present in
	 * the string if there's an unpaired {@code "("} before it.
	 */
	default boolean shouldSkipClosingPair(char typedChar, String context, int caretPos) {
		return false;
	}
}
