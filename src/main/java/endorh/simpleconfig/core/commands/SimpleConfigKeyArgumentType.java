package endorh.simpleconfig.core.commands;

import com.google.gson.JsonObject;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import endorh.simpleconfig.api.SimpleConfig;
import endorh.simpleconfig.api.SimpleConfig.EditType;
import endorh.simpleconfig.api.SimpleConfig.Type;
import endorh.simpleconfig.core.SimpleConfigImpl;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.network.FriendlyByteBuf;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;

import static endorh.simpleconfig.core.ByteBufUtils.readNullable;
import static endorh.simpleconfig.core.ByteBufUtils.writeNullable;

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
		// Some mods use spaces within paths
		return reader.readString();
	}
	
	@Override public <S> CompletableFuture<Suggestions> listSuggestions(
	  CommandContext<S> context, SuggestionsBuilder builder
	) {
		SimpleConfigImpl config = getConfig(context);
		if (config == null) return Suggestions.empty();
		Collection<String> paths = config.getPaths(includeGroups);
		for (String path: paths) builder.suggest(StringArgumentType.escapeIfRequired(path));
		return builder.buildFuture();
	}
	
	public static class Info implements ArgumentTypeInfo<SimpleConfigKeyArgumentType, Info.Template> {
		@Override public void serializeToNetwork(@NotNull Template t, @NotNull FriendlyByteBuf buf) {
			writeNullable(buf, FriendlyByteBuf::writeUtf, t.modId);
			writeNullable(buf, FriendlyByteBuf::writeEnum, t.type);
			buf.writeBoolean(t.includeGroups);
		}
		
		@Override public @NotNull Template deserializeFromNetwork(@NotNull FriendlyByteBuf buf) {
			String modId = readNullable(buf, FriendlyByteBuf::readUtf);
			EditType type = readNullable(buf, b -> b.readEnum(EditType.class));
			boolean includeGroups = buf.readBoolean();
			return new Template(modId, type, includeGroups);
			// return new Template(
			//   buf.readBoolean()? buf.readUtf() : null,
			//   buf.readBoolean()? buf.readEnum(EditType.class) : null,
			//   buf.readBoolean());
		}
		
		@Override public void serializeToJson(Template t, JsonObject obj) {
			obj.addProperty("modId", t.modId);
			obj.addProperty("type", t.type != null? t.type.getAlias() : null);
			obj.addProperty("includeGroups", t.includeGroups);
		}
		
		@Override public @NotNull Template unpack(@NotNull SimpleConfigKeyArgumentType arg) {
			return new Template(arg.modId, arg.type, arg.includeGroups);
		}
		
		public class Template implements ArgumentTypeInfo.Template<SimpleConfigKeyArgumentType> {
			private final String modId;
			private final EditType type;
			private final boolean includeGroups;
			
			public Template(String modId, EditType type, boolean includeGroups) {
				this.modId = modId;
				this.type = type;
				this.includeGroups = includeGroups;
			}
			
			@Override public @NotNull SimpleConfigKeyArgumentType instantiate(@NotNull CommandBuildContext ctx) {
				return new SimpleConfigKeyArgumentType(modId, type, includeGroups);
			}
			
			@Override public @NotNull ArgumentTypeInfo<SimpleConfigKeyArgumentType, ?> type() {
				return Info.this;
			}
		}
	}
}
