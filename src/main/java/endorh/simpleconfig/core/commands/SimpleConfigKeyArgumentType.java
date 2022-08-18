package endorh.simpleconfig.core.commands;

import com.google.gson.JsonObject;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import endorh.simpleconfig.api.SimpleConfig;
import endorh.simpleconfig.api.SimpleConfig.EditType;
import endorh.simpleconfig.api.SimpleConfig.Type;
import endorh.simpleconfig.core.SimpleConfigImpl;
import net.minecraft.command.arguments.IArgumentSerializer;
import net.minecraft.network.PacketBuffer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;

public class SimpleConfigKeyArgumentType implements ArgumentType<String> {
	public static SimpleConfigKeyArgumentType entryPath(
	  @Nullable String modId, @Nullable EditType type, boolean includeGroups
	) {
		return new SimpleConfigKeyArgumentType(modId, type, includeGroups);
	}
	
	private final @Nullable String modId;
	private final @Nullable SimpleConfig.EditType type;
	private final boolean includeGroups;
	
	private SimpleConfigKeyArgumentType(@Nullable String modId, @Nullable EditType type, boolean includeGroups) {
		this.modId = modId;
		this.type = type;
		this.includeGroups = includeGroups;
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
		return reader.readUnquotedString();
	}
	
	@Override public <S> CompletableFuture<Suggestions> listSuggestions(
	  CommandContext<S> context, SuggestionsBuilder builder
	) {
		SimpleConfigImpl config = getConfig(context);
		if (config == null) return Suggestions.empty();
		Collection<String> paths = config.getPaths(includeGroups);
		for (String path: paths) builder.suggest(path);
		return builder.buildFuture();
	}
	
	public static class Serializer implements IArgumentSerializer<SimpleConfigKeyArgumentType> {
		@Override public void write(@NotNull SimpleConfigKeyArgumentType arg, @NotNull PacketBuffer buf) {
			buf.writeBoolean(arg.modId != null);
			if (arg.modId != null) buf.writeString(arg.modId);
			buf.writeBoolean(arg.type != null);
			if (arg.type != null) buf.writeEnumValue(arg.type);
			buf.writeBoolean(arg.includeGroups);
		}
		
		@Override public @NotNull SimpleConfigKeyArgumentType read(@NotNull PacketBuffer buf) {
			return new SimpleConfigKeyArgumentType(
			  buf.readBoolean()? buf.readString(32767) : null,
			  buf.readBoolean()? buf.readEnumValue(EditType.class) : null,
			  buf.readBoolean());
		}
		
		@Override public void write(@NotNull SimpleConfigKeyArgumentType arg, @NotNull JsonObject obj) {
			obj.addProperty("modId", arg.modId);
			obj.addProperty("type", arg.type != null? arg.type.getAlias() : null);
			obj.addProperty("includeGroups", arg.includeGroups);
		}
	}
}
