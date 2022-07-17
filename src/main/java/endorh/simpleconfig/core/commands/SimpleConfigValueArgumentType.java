package endorh.simpleconfig.core.commands;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import endorh.simpleconfig.core.AbstractConfigEntry;
import endorh.simpleconfig.core.SimpleConfig;
import net.minecraft.util.text.TranslationTextComponent;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.YAMLException;

import java.util.concurrent.CompletableFuture;

public class SimpleConfigValueArgumentType implements ArgumentType<String> {
	private static final DynamicCommandExceptionType INVALID_YAML = new DynamicCommandExceptionType(
	  m -> new TranslationTextComponent("simpleconfig.command.error.invalid_yaml", m));
	
	public static SimpleConfigValueArgumentType entryValue(SimpleConfig config) {
		return new SimpleConfigValueArgumentType(config);
	}
	
	private final SimpleConfig config;
	private SimpleConfigValueArgumentType(SimpleConfig config) {
		this.config = config;
	}
	
	@Override public String parse(StringReader reader) throws CommandSyntaxException {
		// TODO: Read a single scalar handling quotes, brackets and braces
		//       instead of consuming all input
		StringBuilder b = new StringBuilder();
		while (reader.canRead())
			b.append(reader.read());
		String value = b.toString();
		Yaml yaml = config.getModConfig().getConfigFormat().getYaml();
		try {
			yaml.compose(new java.io.StringReader(value));
		} catch (YAMLException e) {
			throw INVALID_YAML.createWithContext(reader, e.getLocalizedMessage());
		}
		return value;
	}
	
	@Override public <S> CompletableFuture<Suggestions> listSuggestions(
	  CommandContext<S> context, SuggestionsBuilder builder
	) {
		String key = context.getArgument("key", String.class);
		AbstractConfigEntry<Object, Object, Object, ?> entry = config.getEntry(key);
		String serialized = entry.getForCommand();
		if (serialized != null)
			builder.suggest(serialized, new TranslationTextComponent(
			  "simpleconfig.command.suggest.current"));
		String defSerialized = entry.forCommand(entry.defValue);
		if (defSerialized != null && !defSerialized.equals(serialized))
			builder.suggest(defSerialized, new TranslationTextComponent(
			  "simpleconfig.command.suggest.default"));
		return builder.buildFuture();
	}
}
