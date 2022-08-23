package endorh.simpleconfig.core.commands;

import com.google.gson.JsonObject;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import endorh.simpleconfig.api.SimpleConfig;
import endorh.simpleconfig.api.SimpleConfig.EditType;
import endorh.simpleconfig.config.ServerConfig.permissions;
import endorh.simpleconfig.core.SimpleConfigImpl;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import net.minecraft.commands.synchronization.ArgumentSerializer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

public class SimpleConfigTypeArgumentType implements ArgumentType<EditType> {
	private static final DynamicCommandExceptionType UNKNOWN_TYPE =
	  new DynamicCommandExceptionType(
		 m -> new TranslatableComponent("simpleconfig.command.error.no_such_type", m));
	
	public static SimpleConfigTypeArgumentType type(String modId, boolean isRemote) {
		return new SimpleConfigTypeArgumentType(modId, isRemote);
	}
	
	private final @Nullable String modId;
	private final boolean isRemote;
	
	private SimpleConfigTypeArgumentType(@Nullable String modId, boolean isRemote) {
		this.modId = modId;
		this.isRemote = isRemote;
	}
	
	@Override public EditType parse(StringReader reader) throws CommandSyntaxException {
		String alias = reader.readUnquotedString();
		EditType type = SimpleConfig.EditType.fromAlias(alias);
		if (type == null || type.isRemote() != isRemote)
			throw UNKNOWN_TYPE.createWithContext(reader, alias);
		return type;
	}
	
	@Override public <S> CompletableFuture<Suggestions> listSuggestions(
	  CommandContext<S> context, SuggestionsBuilder builder
	) {
		S src = context.getSource();
		String modId = this.modId != null? this.modId : context.getArgument("modId", String.class);
		if (src instanceof ClientSuggestionProvider)
			if (denyPermission(modId)) return Suggestions.empty();
		SimpleConfigImpl.getAllConfigs().stream()
		  .filter(c -> c.getModId().equals(modId))
		  .map(c -> c.getType().asEditType(isRemote))
		  .filter(t -> t.isRemote() == isRemote)
		  .map(SimpleConfig.EditType::getAlias)
		  .forEach(builder::suggest);
		return builder.buildFuture();
	}
	
	@OnlyIn(Dist.CLIENT) private <S> boolean denyPermission(
	  String modId
	) {
		Player player = Minecraft.getInstance().player;
		return player == null || !permissions.permissionFor(player, modId).getLeft().canView();
	}
	
	public static class Serializer implements ArgumentSerializer<SimpleConfigTypeArgumentType> {
		@Override
		public void serializeToNetwork(@NotNull SimpleConfigTypeArgumentType arg, @NotNull FriendlyByteBuf buf) {
			buf.writeBoolean(arg.modId != null);
			if (arg.modId != null) buf.writeUtf(arg.modId);
			buf.writeBoolean(arg.isRemote);
		}
		
		@Override public @NotNull SimpleConfigTypeArgumentType deserializeFromNetwork(@NotNull FriendlyByteBuf buf) {
			return new SimpleConfigTypeArgumentType(
			  buf.readBoolean()? buf.readUtf(32767) : null,
			  buf.readBoolean());
		}
		
		@Override
		public void serializeToJson(@NotNull SimpleConfigTypeArgumentType arg, @NotNull JsonObject obj) {
			obj.addProperty("modId", arg.modId);
			obj.addProperty("isRemote", arg.isRemote);
		}
	}
}
