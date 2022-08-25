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
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.ModList;
import org.jetbrains.annotations.NotNull;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class SimpleConfigModIdArgumentType implements ArgumentType<String> {
	private static final DynamicCommandExceptionType UNKNOWN_MOD = new DynamicCommandExceptionType(
		 m -> Component.translatable("simpleconfig.command.error.no_such_mod", m));
	
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
		Player player = Minecraft.getInstance().player;
		Set<Type> types = isRemote? SimpleConfig.Type.remoteTypes() : SimpleConfig.Type.localTypes();
		SimpleConfigImpl.getAllConfigs().stream()
		  .filter(c -> types.contains(c.getType()))
		  .map(SimpleConfigImpl::getModId)
		  .filter(id -> player == null || permissions.permissionFor(player, id).getLeft().canView())
		  .forEach(builder::suggest);
		return builder.buildFuture();
	}
	
	public static class Info implements ArgumentTypeInfo<SimpleConfigModIdArgumentType, Info.Template> {
		@Override public void serializeToNetwork(Template t, FriendlyByteBuf buf) {
			buf.writeBoolean(t.isRemote);
		}
		
		@Override public @NotNull Template deserializeFromNetwork(FriendlyByteBuf buf) {
			return new Template(buf.readBoolean());
		}
		
		@Override public void serializeToJson(@NotNull Template t, @NotNull JsonObject obj) {
			obj.addProperty("isRemote", t.isRemote);
		}
		
		@Override public @NotNull Template unpack(@NotNull SimpleConfigModIdArgumentType arg) {
			return new Template(arg.isRemote);
		}
		
		public class Template implements ArgumentTypeInfo.Template<SimpleConfigModIdArgumentType> {
			private final boolean isRemote;
			
			public Template(boolean isRemote) {
				this.isRemote = isRemote;
			}
			
			@Override public @NotNull SimpleConfigModIdArgumentType instantiate(@NotNull CommandBuildContext ctx) {
				return new SimpleConfigModIdArgumentType(isRemote);
			}
			
			@Override public @NotNull ArgumentTypeInfo<SimpleConfigModIdArgumentType, ?> type() {
				return Info.this;
			}
		}
	}
}
