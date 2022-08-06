package endorh.simpleconfig.core.commands;

import com.google.gson.JsonObject;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import endorh.simpleconfig.SimpleConfigMod.ServerConfig.permissions;
import endorh.simpleconfig.core.SimpleConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import net.minecraft.command.arguments.IArgumentSerializer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.config.ModConfig.Type;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public class SimpleConfigModIdArgumentType implements ArgumentType<String> {
	private static final DynamicCommandExceptionType UNKNOWN_CONFIG = new DynamicCommandExceptionType(
		 m -> new TranslationTextComponent("simpleconfig.command.error.no_such_config", m));
	
	public static SimpleConfigModIdArgumentType modId(Type type) {
		return new SimpleConfigModIdArgumentType(type);
	}
	
	private final Type type;
	
	private SimpleConfigModIdArgumentType(Type type) {
		this.type = type;
	}
	
	@Override public String parse(StringReader reader) throws CommandSyntaxException {
		String modId = reader.readUnquotedString();
		if (SimpleConfig.hasConfig(modId, type)) return modId;
		throw UNKNOWN_CONFIG.createWithContext(reader, modId);
	}
	
	@Override public <S> CompletableFuture<Suggestions> listSuggestions(
	  CommandContext<S> context, SuggestionsBuilder builder
	) {
		S src = context.getSource();
		if (src instanceof ClientSuggestionProvider)
			return listSuggestionsWithPermissions(context, builder);
		SimpleConfig.getAllConfigs().stream()
		  .filter(c -> c.getType() == type)
		  .map(SimpleConfig::getModId)
		  .forEach(builder::suggest);
		return builder.buildFuture();
	}
	
	@OnlyIn(Dist.CLIENT) private <S> CompletableFuture<Suggestions> listSuggestionsWithPermissions(
	  CommandContext<S> context, SuggestionsBuilder builder
	) {
		PlayerEntity player = Minecraft.getInstance().player;
		SimpleConfig.getAllConfigs().stream()
		  .filter(c -> c.getType() == type)
		  .map(SimpleConfig::getModId)
		  .filter(id -> player == null || permissions.permissionFor(player, id).getLeft().canView())
		  .forEach(builder::suggest);
		return builder.buildFuture();
	}
	
	public static class Serializer implements IArgumentSerializer<SimpleConfigModIdArgumentType> {
		@Override public void write(@NotNull SimpleConfigModIdArgumentType arg, @NotNull PacketBuffer buf) {
			buf.writeEnumValue(arg.type);
		}
		
		@Override public @NotNull SimpleConfigModIdArgumentType read(@NotNull PacketBuffer buf) {
			return new SimpleConfigModIdArgumentType(buf.readEnumValue(Type.class));
		}
		
		@Override public void write(@NotNull SimpleConfigModIdArgumentType arg, @NotNull JsonObject obj) {
			obj.addProperty("type", arg.type.extension());
		}
	}
}
