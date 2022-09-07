package endorh.simpleconfig.core.commands;

import com.google.gson.JsonObject;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import endorh.simpleconfig.api.SimpleConfig.EditType;
import endorh.simpleconfig.api.SimpleConfig.Type;
import endorh.simpleconfig.core.AbstractConfigEntry;
import endorh.simpleconfig.core.SimpleConfigImpl;
import endorh.simpleconfig.yaml.SimpleConfigCommentedYamlFormat;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.YAMLException;

import java.util.concurrent.CompletableFuture;

public class SimpleConfigValueArgumentType implements ArgumentType<String> {
	private static final DynamicCommandExceptionType INVALID_YAML = new DynamicCommandExceptionType(
	  m -> Component.translatable("simpleconfig.command.error.invalid_yaml", m));
	
	public static SimpleConfigValueArgumentType entryValue(
	  @Nullable String modId, @Nullable EditType type
	) {
		return new SimpleConfigValueArgumentType(modId, type);
	}
	
	private final @Nullable String modId;
	private final @Nullable EditType type;
	
	private SimpleConfigValueArgumentType(@Nullable String modId, @Nullable EditType type) {
		this.modId = modId;
		this.type = type;
	}
	
	public @Nullable SimpleConfigImpl getConfig(CommandContext<?> ctx) {
		String modId = this.modId;
		EditType type = this.type;
		if (modId == null) modId = ctx.getArgument("modId", String.class);
		if (type == null) type = ctx.getArgument("type", EditType.class);
		Type configType = type.getType();
		if (SimpleConfigImpl.hasConfig(modId, configType))
			return SimpleConfigImpl.getConfig(modId, configType);
		return null;
	}
	
	@Override public String parse(StringReader reader) throws CommandSyntaxException {
		// TODO: Read a single scalar handling quotes, brackets and braces
		//       instead of consuming all input
		StringBuilder b = new StringBuilder();
		while (reader.canRead())
			b.append(reader.read());
		String value = b.toString();
		Yaml yaml = SimpleConfigCommentedYamlFormat.getDefaultYaml();
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
		SimpleConfigImpl config = getConfig(context);
		if (config == null) return Suggestions.empty();
		AbstractConfigEntry<?, ?, ?> entry = config.getEntry(key);
		return entry.addCommandSuggestions(builder)? builder.buildFuture() : Suggestions.empty();
	}
	
	public static class Info implements ArgumentTypeInfo<SimpleConfigValueArgumentType, Info.Template> {
		@Override public void serializeToNetwork(Template t, FriendlyByteBuf buf) {
			buf.writeBoolean(t.modId != null);
			if (t.modId != null) buf.writeUtf(t.modId);
			buf.writeBoolean(t.type != null);
			if (t.type != null) buf.writeEnum(t.type);
		}
		
		@Override public @NotNull Template deserializeFromNetwork(FriendlyByteBuf buf) {
			return new Template(
			  buf.readBoolean()? buf.readUtf() : null,
			  buf.readBoolean()? buf.readEnum(EditType.class) : null);
		}
		
		@Override public void serializeToJson(@NotNull Template t, @NotNull JsonObject obj) {
			obj.addProperty("modId", t.modId);
			obj.addProperty("type", t.type != null? t.type.getAlias() : null);
		}
		
		@Override public @NotNull Template unpack(@NotNull SimpleConfigValueArgumentType arg) {
			return new Template(arg.modId, arg.type);
		}
		
		public class Template implements ArgumentTypeInfo.Template<SimpleConfigValueArgumentType> {
			final String modId;
			final EditType type;
			
			public Template(String modId, EditType type) {
				this.modId = modId;
				this.type = type;
			}
			
			@Override public @NotNull SimpleConfigValueArgumentType instantiate(
			  @NotNull CommandBuildContext ctx) {
				return new SimpleConfigValueArgumentType(modId, type);
			}
			
			@Override public @NotNull ArgumentTypeInfo<SimpleConfigValueArgumentType, ?> type() {
				return Info.this;
			}
		}
	}
}
