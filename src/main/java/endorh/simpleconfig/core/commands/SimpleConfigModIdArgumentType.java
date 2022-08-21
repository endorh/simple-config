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
import endorh.simpleconfig.api.SimpleConfig.Type;
import endorh.simpleconfig.config.ServerConfig.permissions;
import endorh.simpleconfig.core.SimpleConfigImpl;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import net.minecraft.command.arguments.IArgumentSerializer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.ModList;
import org.jetbrains.annotations.NotNull;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class SimpleConfigModIdArgumentType implements ArgumentType<String> {
	private static final DynamicCommandExceptionType UNKNOWN_MOD = new DynamicCommandExceptionType(
		 m -> new TranslationTextComponent("simpleconfig.command.error.no_such_mod", m));
	
	public static SimpleConfigModIdArgumentType modId(boolean isRemote) {
		return new SimpleConfigModIdArgumentType(isRemote);
	}
	
	boolean isRemote;
	
	private SimpleConfigModIdArgumentType(boolean isRemote) {
		this.isRemote = isRemote;
	}
	
	@Override public String parse(StringReader reader) throws CommandSyntaxException {
		String modId = reader.readUnquotedString();
		if (ModList.get().getMods().stream().noneMatch(m -> m.getModId().equals(modId)))
			throw UNKNOWN_MOD.createWithContext(reader, modId);
		return modId;
	}
	
	@Override public <S> CompletableFuture<Suggestions> listSuggestions(
	  CommandContext<S> context, SuggestionsBuilder builder
	) {
		S src = context.getSource();
		if (src instanceof ClientSuggestionProvider)
			return listSuggestionsWithPermissions(builder);
		Set<Type> types = isRemote? SimpleConfig.Type.remoteTypes() : SimpleConfig.Type.localTypes();
		SimpleConfigImpl.getAllConfigs().stream()
		  .filter(c -> types.contains(c.getType()))
		  .map(SimpleConfigImpl::getModId)
		  .forEach(builder::suggest);
		return builder.buildFuture();
	}
	
	@OnlyIn(Dist.CLIENT) private CompletableFuture<Suggestions> listSuggestionsWithPermissions(
	  SuggestionsBuilder builder
	) {
		PlayerEntity player = Minecraft.getInstance().player;
		Set<Type> types = isRemote? SimpleConfig.Type.remoteTypes() : SimpleConfig.Type.localTypes();
		SimpleConfigImpl.getAllConfigs().stream()
		  .filter(c -> types.contains(c.getType()))
		  .map(SimpleConfigImpl::getModId)
		  .filter(id -> player == null || permissions.permissionFor(player, id).getLeft().canView())
		  .forEach(builder::suggest);
		return builder.buildFuture();
	}
	
	public static class Serializer implements IArgumentSerializer<SimpleConfigModIdArgumentType> {
		@Override public void serializeToNetwork(@NotNull SimpleConfigModIdArgumentType arg, @NotNull PacketBuffer buf) {
			buf.writeBoolean(arg.isRemote);
		}
		
		@Override public @NotNull SimpleConfigModIdArgumentType deserializeFromNetwork(@NotNull PacketBuffer buf) {
			return new SimpleConfigModIdArgumentType(buf.readBoolean());
		}
		
		@Override public void serializeToJson(@NotNull SimpleConfigModIdArgumentType arg, @NotNull JsonObject obj) {
			obj.addProperty("isRemote", arg.isRemote);
		}
	}
}
