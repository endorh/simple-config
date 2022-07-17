package endorh.simpleconfig.core.commands;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import endorh.simpleconfig.core.SimpleConfig;
import net.minecraft.util.text.TranslationTextComponent;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;

public class SimpleConfigKeyArgumentType implements ArgumentType<String> {
	private static final SimpleCommandExceptionType UNKNOWN_KEY = new SimpleCommandExceptionType(
	  new TranslationTextComponent("simpleconfig.command.error.no_such_entry"));
	
	public static SimpleConfigKeyArgumentType entryPath(SimpleConfig config, boolean includeGroups) {
		return new SimpleConfigKeyArgumentType(config, includeGroups);
	}
	
	private final SimpleConfig config;
	private final boolean includeGroups;
	
	private SimpleConfigKeyArgumentType(SimpleConfig config, boolean includeGroups) {
		this.config = config;
		this.includeGroups = includeGroups;
	}
	
	@Override public String parse(StringReader reader) throws CommandSyntaxException {
		String path = reader.readUnquotedString();
		if (!config.hasEntry(path) && (!includeGroups || !config.hasChild(path)))
			throw UNKNOWN_KEY.createWithContext(reader);
		return path;
	}
	
	@Override public <S> CompletableFuture<Suggestions> listSuggestions(
	  CommandContext<S> context, SuggestionsBuilder builder
	) {
		Collection<String> paths = config.getPaths(includeGroups);
		for (String path: paths) builder.suggest(path);
		return builder.buildFuture();
	}
}
