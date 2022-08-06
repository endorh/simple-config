package endorh.simpleconfig.core.commands;

import com.google.gson.JsonObject;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import endorh.simpleconfig.core.SimpleConfig;
import net.minecraft.command.arguments.IArgumentSerializer;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.fml.config.ModConfig.Type;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;

public class SimpleConfigKeyArgumentType implements ArgumentType<String> {
	private static final DynamicCommandExceptionType UNKNOWN_CONFIG = new DynamicCommandExceptionType(
		 m -> new TranslationTextComponent("simpleconfig.command.error.no_such_config", m));
	private static final SimpleCommandExceptionType UNKNOWN_KEY = new SimpleCommandExceptionType(
	  new TranslationTextComponent("simpleconfig.command.error.no_such_entry"));
	
	public static SimpleConfigKeyArgumentType entryPath(String modId, Type type, boolean includeGroups) {
		return new SimpleConfigKeyArgumentType(modId, type, includeGroups);
	}
	
	private final String modId;
	private final Type type;
	private final boolean includeGroups;
	
	private SimpleConfigKeyArgumentType(String modId, Type type, boolean includedGroups) {
		this.modId = modId;
		this.type = type;
		this.includeGroups = includedGroups;
	}
	
	public @Nullable SimpleConfig getConfig(CommandContext<?> ctx) {
		String modId = this.modId;
		if (modId == null) modId = ctx.getArgument("modId", String.class);
		if (SimpleConfig.hasConfig(modId, type))
			return SimpleConfig.getConfig(modId, type);
		return null;
	}
	
	@Override public String parse(StringReader reader) throws CommandSyntaxException {
		return reader.readUnquotedString();
		// SimpleConfig config = getConfig();
		// if (config == null) throw UNKNOWN_CONFIG.create(modId + " [" + type.extension() + "]");
		// if (!config.hasEntry(path) && (!includeGroups || !config.hasChild(path)))
		// 	throw UNKNOWN_KEY.createWithContext(reader);
		// return path;
	}
	
	@Override public <S> CompletableFuture<Suggestions> listSuggestions(
	  CommandContext<S> context, SuggestionsBuilder builder
	) {
		SimpleConfig config = getConfig(context);
		if (config == null) return Suggestions.empty();
		Collection<String> paths = config.getPaths(includeGroups);
		for (String path: paths) builder.suggest(path);
		return builder.buildFuture();
	}
	
	public static class Serializer implements IArgumentSerializer<SimpleConfigKeyArgumentType> {
		@Override public void write(@NotNull SimpleConfigKeyArgumentType arg, @NotNull PacketBuffer buf) {
			buf.writeBoolean(arg.modId != null);
			if (arg.modId != null) buf.writeString(arg.modId);
			buf.writeEnumValue(arg.type);
			buf.writeBoolean(arg.includeGroups);
		}
		
		@Override public @NotNull SimpleConfigKeyArgumentType read(@NotNull PacketBuffer buf) {
			return new SimpleConfigKeyArgumentType(
			  buf.readBoolean()? buf.readString(32767) : null,
			  buf.readEnumValue(Type.class),
			  buf.readBoolean());
		}
		
		@Override public void write(@NotNull SimpleConfigKeyArgumentType arg, @NotNull JsonObject obj) {
			obj.addProperty("modId", arg.modId);
			obj.addProperty("type", arg.type.extension());
			obj.addProperty("includeGroups", arg.includeGroups);
		}
	}
}
