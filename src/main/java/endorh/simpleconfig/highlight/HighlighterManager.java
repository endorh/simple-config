package endorh.simpleconfig.highlight;

import com.google.gson.*;
import endorh.simpleconfig.api.ui.IHighlighterManager;
import endorh.simpleconfig.api.ui.ILanguageHighlighter;
import endorh.simpleconfig.highlight.HighlighterManager.HighlightRule.RuleDeserializer;
import endorh.simpleconfig.highlight.HighlighterManager.LanguageHighlightingRules.HighlighterDeserializer;
import endorh.simpleconfig.highlight.HighlighterManager.LanguageHighlightingRules.StyleDeserializer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.storage.loot.Deserializers;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.ParseTreeListener;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Type;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class HighlighterManager extends SimpleJsonResourceReloadListener implements IHighlighterManager {
	private static final Gson GSON = Deserializers.createConditionSerializer()
	  .registerTypeAdapter(HighlightRule.class, new RuleDeserializer())
	  .registerTypeAdapter(LanguageHighlightingRules.class, new HighlighterDeserializer())
	  .registerTypeAdapter(Style.class, new StyleDeserializer())
	  .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
	  .create();
	public static final HighlighterManager INSTANCE = new HighlighterManager();
	
	private final Map<String, LanguageHighlighter<?>> highlighters = new HashMap<>();
	private final Map<String, LanguageHighlightingRules> rules = new HashMap<>();
	
	public HighlighterManager() {
		super(GSON, "simpleconfig-highlight");
	}
	
	@Override protected void apply(
	  @NotNull Map<ResourceLocation, JsonElement> map, @NotNull ResourceManager manager,
	  @NotNull ProfilerFiller profiler
	) {
		map.entrySet().stream()
		  .map(e -> Pair.of(e.getKey(), GSON.fromJson(e.getValue(), LanguageHighlightingRules.class)))
		  .forEach(e -> {
			  String path = e.getKey().getPath();
			  LanguageHighlightingRules rules = e.getValue();
			  highlighters.get(path).setRules(rules);
			  this.rules.put(path, rules);
		  });
	}
	
	public void registerHighlighter(LanguageHighlighter<?> parser) {
		highlighters.put(parser.getLanguage(), parser);
	}
	
	@Override public @Nullable ILanguageHighlighter getHighlighter(String language) {
		LanguageHighlighter<?> parser = highlighters.get(language);
		if (parser != null) parser.setRules(
		  rules.getOrDefault(language, LanguageHighlightingRules.EMPTY));
		return parser;
	}
	
	public static class LanguageHighlighter<P extends Parser> implements ILanguageHighlighter {
		private final String language;
		private final Function<CharStream, Lexer> lexerFactory;
		private final Function<CommonTokenStream, P> parserFactory;
		private final Function<P, ParserRuleContext> rootParser;
		private LanguageHighlightingRules highlighter = LanguageHighlightingRules.EMPTY;
		
		public LanguageHighlighter(
		  String language, Function<CharStream, Lexer> lexerFactory,
		  Function<CommonTokenStream, P> parserFactory,
		  Function<P, ParserRuleContext> rootParser
		) {
			this.language = language;
			this.lexerFactory = lexerFactory;
			this.parserFactory = parserFactory;
			this.rootParser = rootParser;
		}
		
		private static class HighlightListener implements ParseTreeListener {
			private final Parser parser;
			private final CommonTokenStream tokens;
			private final LanguageHighlightingRules highlighter;
			private Style defaultStyle = Style.EMPTY;
			private Style errorStyle = Style.EMPTY.withColor(ChatFormatting.RED).withUnderlined(true);
			private MutableComponent result = null;
			private int lastIndex = 0;
			private final Stack<Style> styleStack = new Stack<>();
			
			public HighlightListener(Parser parser, CommonTokenStream tokens, LanguageHighlightingRules highlighter) {
				this.parser = parser;
				this.tokens = tokens;
				this.highlighter = highlighter;
				Map<String, Style> styles = highlighter.getStyles();
				defaultStyle = styles.getOrDefault("default", defaultStyle);
				errorStyle = styles.getOrDefault("error", errorStyle);
			}
			
			public HighlightListener withDefaultStyle(Style style) {
				this.defaultStyle = style;
				return this;
			}
			
			public HighlightListener withErrorStyle(Style style) {
				this.errorStyle = style;
				return this;
			}
			
			public MutableComponent getResult() {
				return result != null? result : TextComponent.EMPTY.copy();
			}
			
			protected void appendFragment(String fragment, Style style) {
				if (style == null) style = defaultStyle;
				if (result != null) {
					result.append(new TextComponent(fragment).setStyle(style));
				} else result = new TextComponent(fragment).setStyle(style);
			}
			
			public void visitHiddenTokensUpTo(int index) {
				if (index - lastIndex > 0) {
					for (int i = lastIndex; i < index; i++) {
						Token token = tokens.get(i);
						if (token.getChannel() != Token.DEFAULT_CHANNEL)
							visitHiddenToken(token);
					}
				}
				lastIndex = index;
			}
			
			public void visitHiddenToken(Token token) {
				if (token.getType() == Token.EOF) return;
				appendFragment(token.getText(), styleStack.isEmpty() ? defaultStyle : styleStack.peek());
			}
			
			@Override public void visitTerminal(TerminalNode node) {
				visitHiddenTokensUpTo(node.getSymbol().getTokenIndex());
				if (node.getSymbol().getType() == Token.EOF) return;
				Style style = styleStack.isEmpty()? defaultStyle : styleStack.peek();
				String tokenName = parser.getVocabulary().getSymbolicName(node.getSymbol().getType());
				String ruleName = highlighter.getRuleForToken(tokenName);
				if (ruleName != null)
					style = highlighter.getStyles().getOrDefault(ruleName, style).applyTo(style);
				appendFragment(node.getText(), style);
			}
			
			@Override public void visitErrorNode(ErrorNode node) {
				visitHiddenTokensUpTo(node.getSymbol().getTokenIndex());
				appendFragment(node.getText(), errorStyle);
			}
			
			@Override public void enterEveryRule(ParserRuleContext ctx) {
				String grammarRule = parser.getRuleNames()[ctx.getRuleIndex()];
				String highlightingRule = highlighter.getRuleForRule(grammarRule);
				if (highlightingRule != null) {
					Style style = highlighter.getStyles().getOrDefault(highlightingRule, defaultStyle);
					Style last = styleStack.isEmpty()? Style.EMPTY : styleStack.peek();
					styleStack.push(style.applyTo(last));
				}
			}
			
			@Override public void exitEveryRule(ParserRuleContext ctx) {
				String ruleName = parser.getRuleNames()[ctx.getRuleIndex()];
				if (highlighter.getRuleForRule(ruleName) != null) styleStack.pop();
			}
		}
		
		@Override public MutableComponent highlight(String text) {
			if (text.isEmpty()) return TextComponent.EMPTY.copy();
			Lexer lexer = lexerFactory.apply(CharStreams.fromString(text));
			CommonTokenStream tokens = new CommonTokenStream(lexer);
			P parser = parserFactory.apply(tokens);
			parser.removeErrorListeners();
			HighlightListener highlighter = new HighlightListener(parser, tokens, this.highlighter);
			parser.addParseListener(highlighter);
			rootParser.apply(parser);
			return highlighter.getResult();
		}
		
		@Override public MutableComponent formatText(String text) {
			return highlight(text);
		}
		
		@Override public @Nullable String closingPair(char typedChar, String context, int caretPos) {
			Map<String, String> tokenPairs = highlighter.getTokenPairs();
			String upTo = context.substring(0, caretPos) + typedChar;
			if (!tokenPairs.isEmpty()) {
				Lexer lexer = lexerFactory.apply(CharStreams.fromString(upTo));
				CommonTokenStream tokens = new CommonTokenStream(lexer);
				tokens.fill();
				if (tokens.size() > 0) {
					int i = tokens.size() - 1;
					Token last = null;
					while (i >= 0) {
						last = tokens.get(i--);
						if (last.getType() != Token.EOF) break;
					}
					if (last != null) {
						String tokenType = lexer.getVocabulary().getSymbolicName(last.getType());
						String closingPair = tokenPairs.get(tokenType);
						if (closingPair != null) return closingPair;
					}
				}
			}
			Map<String, String> charPairs = highlighter.getCharPairs();
			if (!charPairs.isEmpty()) {
				return charPairs.entrySet().stream()
				  .filter(e -> upTo.endsWith(e.getKey()))
				  .map(Entry::getValue)
				  .findFirst().orElse(null);
			}
			return null;
		}
		
		@Override public boolean shouldSkipClosingPair(char typedChar, String context, int caretPos) {
			return highlighter.getClosingPairs().contains(String.valueOf(typedChar));
		}
		
		@Override public String getLanguage() {
			return language;
		}
		
		public Function<CharStream, Lexer> getLexerFactory() {
			return lexerFactory;
		}
		public Function<CommonTokenStream, P> getParserFactory() {
			return parserFactory;
		}
		public Function<P, ParserRuleContext> getRootParser() {
			return rootParser;
		}
		public LanguageHighlightingRules getHighlighter() {
			return highlighter;
		}
		
		public void setRules(LanguageHighlightingRules highlighter) {
			this.highlighter = highlighter;
		}
	}
	
	public static class LanguageHighlightingRules {
		public static final LanguageHighlightingRules EMPTY = new LanguageHighlightingRules(
		  Collections.emptyMap(), Collections.emptyMap(),
		  Collections.emptyMap(), Collections.emptyMap());
		
		private final Map<String, HighlightRule> rules;
		private final Map<String, Style> styles;
		private final Map<String, String> tokenPairs;
		private final Map<String, String> charPairs;
		private final Set<String> closingPairs;
		private final Map<String, String> rulesByRule = new HashMap<>();
		private final Map<String, String> rulesByToken = new HashMap<>();
		
		public LanguageHighlightingRules(
		  Map<String, HighlightRule> rules, Map<String, Style> styles,
		  Map<String, String> tokenPairs, Map<String, String> charPairs
		) {
			this.rules = rules;
			this.styles = styles;
			this.tokenPairs = tokenPairs;
			this.charPairs = charPairs;
			closingPairs = Stream.concat(tokenPairs.values().stream(), charPairs.values().stream())
			  .collect(Collectors.toSet());
		}
		
		public String getRuleForRule(String rule) {
			return rulesByRule.computeIfAbsent(
			  rule, r -> rules.entrySet().stream()
				 .filter(e -> e.getValue().getRules().contains(r))
				 .map(Entry::getKey)
				 .findFirst().orElse(null));
		}
		
		public String getRuleForToken(String token) {
			return rulesByToken.computeIfAbsent(
			  token, t -> rules.entrySet().stream()
				 .filter(e -> e.getValue().getTokens().contains(t))
				 .map(Entry::getKey)
				 .findFirst().orElse(null));
		}
		
		public Map<String, HighlightRule> getRules() {
			return rules;
		}
		
		public Style getStyle(String ruleName) {
			return styles.get(ruleName);
		}
		
		public Map<String, Style> getStyles() {
			return styles;
		}
		
		public Map<String, String> getTokenPairs() {
			return tokenPairs;
		}
		
		public Map<String, String> getCharPairs() {
			return charPairs;
		}
		
		public Set<String> getClosingPairs() {
			return closingPairs;
		}
		
		public static class HighlighterDeserializer implements JsonDeserializer<LanguageHighlightingRules> {
			@Override public LanguageHighlightingRules deserialize(
			  JsonElement json, Type typeOfT, JsonDeserializationContext context
			) throws JsonParseException {
				if (!json.isJsonObject())
					throw new JsonParseException("Highlighter must be a JSON object");
				JsonObject obj = json.getAsJsonObject();
				JsonObject rulesObj = GsonHelper.getAsJsonObject(obj, "rules");
				Map<String, HighlightRule> rules = new LinkedHashMap<>();
				rulesObj.entrySet().forEach(
				  e -> rules.put(e.getKey(), context.deserialize(e.getValue(), HighlightRule.class)));
				JsonObject stylesObj = GsonHelper.getAsJsonObject(obj, "styles");
				Map<String, Object> styleDefs = new LinkedHashMap<>();
				stylesObj.entrySet().forEach(e -> styleDefs.put(
				  e.getKey(), e.getValue().isJsonPrimitive()
				              ? e.getValue().getAsString()
				              : context.deserialize(e.getValue(), Style.class)));
				Map<String, Style> styles = new LinkedHashMap<>();
				styleDefs.forEach((key, value) -> {
					if (value instanceof Style) {
						styles.put(key, (Style) value);
					} else {
						int guard = 1024; // If you need more than 1024 deep rules, contact me
						while (value instanceof String && guard-- > 0)
							value = styleDefs.get((String) value);
						if (guard <= 0)
							throw new JsonParseException("Circular style reference");
						if (value instanceof Style) {
							styles.put(key, (Style) value);
						} else throw new JsonParseException("Invalid style definition");
					}
				});
				Map<String, String> tokenPairs = new LinkedHashMap<>();
				if (obj.has("token_pairs")) {
					JsonObject tokenPairsObj = GsonHelper.getAsJsonObject(obj, "token_pairs");
					tokenPairsObj.entrySet().forEach(
					  e -> tokenPairs.put(e.getKey(), GsonHelper.getAsString(tokenPairsObj, e.getKey())));
				}
				Map<String, String> charPairs = new LinkedHashMap<>();
				if (obj.has("char_pairs")) {
					JsonObject charPairsObj = GsonHelper.getAsJsonObject(obj, "char_pairs");
					charPairsObj.entrySet().forEach(
					  e -> charPairs.put(e.getKey(), GsonHelper.getAsString(charPairsObj, e.getKey())));
				}
				return new LanguageHighlightingRules(rules, styles, tokenPairs, charPairs);
			}
		}
		
		public static class StyleDeserializer implements JsonDeserializer<Style> {
			private static final Pattern COLOR_PATTERN = Pattern.compile("#(?:[\\da-fA-F]{3}){1,2}");
			@Override public Style deserialize(
			  JsonElement json, Type typeOfT, JsonDeserializationContext context
			) throws JsonParseException {
				if (!json.isJsonObject())
					throw new JsonParseException("Style must be a JSON object");
				JsonObject obj = json.getAsJsonObject();
				String color = GsonHelper.getAsString(obj, "color", "#F0F0F0");
				if (!COLOR_PATTERN.matcher(color).matches())
					throw new JsonParseException("Invalid hex color: " + color);
				if (color.length() == 4)
					color = color.substring(1, 1) + color.substring(1, 1)
					        + color.substring(2, 2) + color.substring(2, 2)
					        + color.substring(3, 3) + color.substring(3, 3);
				boolean bold = GsonHelper.getAsBoolean(obj, "bold", false);
				boolean italic = GsonHelper.getAsBoolean(obj, "italic", false);
				boolean underlined = GsonHelper.getAsBoolean(obj, "underlined", false);
				boolean strikethrough = GsonHelper.getAsBoolean(obj, "strikethrough", false);
				return Style.EMPTY.withColor(
				  TextColor.parseColor(color)
				).withBold(bold)
				  .withItalic(italic)
				  .withUnderlined(underlined)
				  .setStrikethrough(strikethrough);
			}
		}
	}
	
	public static class HighlightRule {
		private final List<String> tokens;
		private final List<String> rules;
		
		public HighlightRule(List<String> tokens, List<String> rules) {
			this.tokens = tokens;
			this.rules = rules;
		}
		
		public List<String> getTokens() {
			return tokens;
		}
		
		public List<String> getRules() {
			return rules;
		}
		
		public static class RuleDeserializer implements JsonDeserializer<HighlightRule> {
			@Override public HighlightRule deserialize(
			  JsonElement json, Type typeOfT, JsonDeserializationContext context
			) throws JsonParseException {
				if (!json.isJsonObject())
					throw new JsonParseException("Highlight rule must be a JSON object");
				JsonObject obj = json.getAsJsonObject();
				JsonArray arr = GsonHelper.getAsJsonArray(obj, "tokens", null);
				List<String> tokens = new ArrayList<>();
				if (arr != null) {
					for (JsonElement element : arr) {
						String str = element.getAsJsonPrimitive().getAsString();
						tokens.add(str);
					}
				}
				arr = GsonHelper.getAsJsonArray(obj, "rules", null);
				List<String> rules = new ArrayList<>();
				if (arr != null) {
					for (JsonElement element : arr) {
						String str = element.getAsJsonPrimitive().getAsString();
						rules.add(str);
					}
				}
				return new HighlightRule(tokens, rules);
			}
		}
	
	}
}
