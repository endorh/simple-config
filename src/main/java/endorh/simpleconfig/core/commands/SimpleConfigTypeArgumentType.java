package endorh.simpleconfig.core.commands;

import com.google.gson.JsonObject;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import endorh.simpleconfig.config.ServerConfig.permissions;
import endorh.simpleconfig.core.SimpleConfig;
import endorh.simpleconfig.core.SimpleConfig.EditType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import net.minecraft.command.arguments.IArgumentSerializer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

public class SimpleConfigTypeArgumentType implements ArgumentType<EditType> {
	private static final DynamicCommandExceptionType UNKNOWN_TYPE =
	  new DynamicCommandExceptionType(
		 m -> new TranslationTextComponent("simpleconfig.command.error.no_such_type", m));
	
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
		EditType type = EditType.fromAlias(alias);
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
		SimpleConfig.getAllConfigs().stream()
		  .filter(c -> c.getModId().equals(modId))
		  .map(c -> c.getType().asEditType(isRemote))
		  .filter(t -> t.isRemote() == isRemote)
		  .map(EditType::getAlias)
		  .forEach(builder::suggest);
		return builder.buildFuture();
	}
	
	@OnlyIn(Dist.CLIENT) private <S> boolean denyPermission(
	  String modId
	) {
		PlayerEntity player = Minecraft.getInstance().player;
		return player == null || !permissions.permissionFor(player, modId).getLeft().canView();
	}
	
	public static class Serializer implements IArgumentSerializer<SimpleConfigTypeArgumentType> {
		@Override
		public void write(@NotNull SimpleConfigTypeArgumentType arg, @NotNull PacketBuffer buf) {
			buf.writeBoolean(arg.modId != null);
			if (arg.modId != null) buf.writeString(arg.modId);
			buf.writeBoolean(arg.isRemote);
		}
		
		@Override public @NotNull SimpleConfigTypeArgumentType read(@NotNull PacketBuffer buf) {
			return new SimpleConfigTypeArgumentType(
			  buf.readBoolean()? buf.readString(32767) : null,
			  buf.readBoolean());
		}
		
		@Override
		public void write(@NotNull SimpleConfigTypeArgumentType arg, @NotNull JsonObject obj) {
			obj.addProperty("modId", arg.modId);
			obj.addProperty("isRemote", arg.isRemote);
		}
	}
}
