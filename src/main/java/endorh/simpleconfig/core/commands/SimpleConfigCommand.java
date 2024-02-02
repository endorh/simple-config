package endorh.simpleconfig.core.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.context.ParsedCommandNode;
import com.mojang.brigadier.context.StringRange;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import endorh.simpleconfig.SimpleConfigMod;
import endorh.simpleconfig.api.SimpleConfig.*;
import endorh.simpleconfig.config.ServerConfig.ConfigPermission;
import endorh.simpleconfig.config.ServerConfig.permissions;
import endorh.simpleconfig.core.AbstractConfigEntry;
import endorh.simpleconfig.core.AbstractSimpleConfigEntryHolder;
import endorh.simpleconfig.core.SimpleConfigGUIManagerImpl;
import endorh.simpleconfig.core.SimpleConfigImpl;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.*;
import net.minecraft.network.chat.HoverEvent.Action;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent.ClientTickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.forgespi.language.IModInfo;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static endorh.simpleconfig.core.commands.SimpleConfigKeyArgumentType.entryPath;
import static endorh.simpleconfig.core.commands.SimpleConfigValueArgumentType.entryValue;
import static java.lang.Math.min;
import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

@EventBusSubscriber(modid = SimpleConfigMod.MOD_ID)
public class SimpleConfigCommand {
	private static final SimpleConfigCommand INSTANCE = new SimpleConfigCommand();
	private static final DynamicCommandExceptionType UNSUPPORTED_CONFIG = new DynamicCommandExceptionType(
	  m -> Component.translatable("simpleconfig.command.error.unsupported_config", m));
	private static final DynamicCommandExceptionType NO_PERMISSION = new DynamicCommandExceptionType(
	  m -> Component.translatable("simpleconfig.command.error.no_permission", m));
	private static final Logger LOGGER = LogManager.getLogger();
	
	@OnlyIn(Dist.CLIENT) private CommandClientTickExecutor clientTickExecutor;
	
	// Styles
	private Style keyStyle = Style.EMPTY.withColor(TextColor.fromRgb(0xA080C0)).withItalic(true);
	private Style modNameStyle = Style.EMPTY.applyFormat(ChatFormatting.AQUA).withBold(false);
	private Style typeStyle = Style.EMPTY.applyFormat(ChatFormatting.LIGHT_PURPLE);
	private Style valueStyle = Style.EMPTY.applyFormat(ChatFormatting.DARK_AQUA);
	private Style undoStyle = Style.EMPTY.applyFormat(ChatFormatting.BLUE);
	private Style copyStyle = Style.EMPTY.withColor(TextColor.fromRgb(0x808080));
	
	public SimpleConfigCommand() {
		DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
		  clientTickExecutor = new CommandClientTickExecutor());
	}
	
	protected boolean isRemote() {
		return true;
	}
	
	@SubscribeEvent public static void onRegisterCommands(RegisterCommandsEvent event) {
		CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
		ModList.get().forEachModContainer((modId, container) -> Stream.of(Type.SERVER, Type.COMMON).forEach(type -> {
			SimpleConfigImpl config = SimpleConfigImpl.getConfigOrNull(modId, type);
			if (config != null) {
				EditType tt = type.asEditType(true);
				INSTANCE.register(dispatcher, modId, tt);
				LiteralArgumentBuilder<CommandSourceStack> root = config.getCommandRoot();
				if (root != null) INSTANCE.registerRoot(modId, tt, dispatcher, root);
			}
		}));
	}
	
	@OnlyIn(Dist.CLIENT)
	@SubscribeEvent public static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
		CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
		ModList.get().forEachModContainer((modId, container) -> Stream.of(Type.CLIENT, Type.COMMON).forEach(type -> {
 			SimpleConfigImpl config = SimpleConfigImpl.getConfigOrNull(modId, type);
			if (config != null) {
				EditType tt = type.asEditType(false);
				INSTANCE.registerClient(dispatcher, modId, tt);
				LiteralArgumentBuilder<CommandSourceStack> root = config.getCommandRoot();
				if (root != null) INSTANCE.registerClientRoot(modId, tt, dispatcher, root);
			}
		}));
	}
	
	protected void register(
	  CommandDispatcher<CommandSourceStack> dispatcher, String modId, EditType type
	) {
		// Register under global /config command
		// For the global command, the mod id is passed instead as parameter to the action
		boolean remote = true;
		LiteralArgumentBuilder<CommandSourceStack> command = literal("config")
		  .then(literal("get").then(buildGetCommand(literal(modId), modId, type, remote)))
		  .then(literal("set").then(buildSetCommand(literal(modId), modId, type, remote)))
		  .then(literal("reset").then(buildResetCommand(literal(modId), modId, type, remote)));
		dispatcher.register(command);
	}
	
	@OnlyIn(Dist.CLIENT) protected void registerClient(
	  CommandDispatcher<CommandSourceStack> dispatcher, String modId, EditType type
	) {
		// Register under global /config command
		// For the global command, the mod id is passed instead as parameter to the action
		boolean remote = false;
		LiteralArgumentBuilder<CommandSourceStack> command = literal("config")
		  .then(literal("edit").then(literal(modId).executes(c -> openConfigGUI(c, modId))))
		  .then(literal("get").then(buildGetCommand(literal(modId), modId, type, remote)))
		  .then(literal("set").then(buildSetCommand(literal(modId), modId, type, remote)))
		  .then(literal("reset").then(buildResetCommand(literal(modId), modId, type, remote)));
		dispatcher.register(command);
	}
	
	protected void registerRoot(
	  String modId, EditType type, CommandDispatcher<CommandSourceStack> dispatcher,
	  LiteralArgumentBuilder<CommandSourceStack> root
	) {
		LiteralArgumentBuilder<CommandSourceStack> command = root
		  .then(buildGetCommand(literal("get").requires(permission(modId, false)), modId, type, true))
		  .then(buildSetCommand(literal("set").requires(permission(modId, true)), modId, type, true))
		  .then(buildResetCommand(literal("reset").requires(permission(modId, true)), modId, type, true));
		dispatcher.register(command);
	}
	
	@OnlyIn(Dist.CLIENT) protected void registerClientRoot(
	  String modId, EditType type, CommandDispatcher<CommandSourceStack> dispatcher,
	  LiteralArgumentBuilder<CommandSourceStack> root
	) {
		LiteralArgumentBuilder<CommandSourceStack> command = root
		  .then(literal("edit").executes(c -> openConfigGUI(c, modId)))
		  .then(buildGetCommand(literal("get"), modId, type, false))
		  .then(buildSetCommand(literal("set"), modId, type, false))
		  .then(buildResetCommand(literal("reset"), modId, type, false));
		dispatcher.register(command);
	}
	
	private LiteralArgumentBuilder<CommandSourceStack> buildGetCommand(
	  LiteralArgumentBuilder<CommandSourceStack> builder, String modId, EditType type, boolean remote
	) {
		// Types must be literals in order to get client and server commands to merge suggestions
		Type tt = type.getType();
		LiteralArgumentBuilder<CommandSourceStack> b = literal(type.getAlias());
		if (remote) b.requires(permission(modId, false));
		b.then(argument("key", entryPath(modId, type, false)).executes(
		  remote? c -> getEntryValue(c, modId, tt)
		        : c -> getClientEntryValue(c, modId, tt)));
		builder.then(b);
		return builder;
	}
	
	private LiteralArgumentBuilder<CommandSourceStack> buildSetCommand(
	  LiteralArgumentBuilder<CommandSourceStack> builder, String modId, EditType type, boolean remote
	) {
		Type tt = type.getType();
		LiteralArgumentBuilder<CommandSourceStack> b = literal(type.getAlias());
		if (remote) b.requires(permission(modId, true));
		b.then(
		  argument("key", entryPath(modId, type, false)).then(
			 argument("value", entryValue(modId, type)).executes(
				remote? c -> setEntryValue(c, modId, tt)
				      : c -> setClientEntryValue(c, modId, tt))));
		builder.then(b);
		return builder;
	}
	
	private LiteralArgumentBuilder<CommandSourceStack> buildResetCommand(
	  LiteralArgumentBuilder<CommandSourceStack> builder, String modId, EditType type, boolean remote
	) {
		Type tt = type.getType();
		LiteralArgumentBuilder<CommandSourceStack> b = literal(type.getAlias());
		if (remote) b.requires(permission(modId, true));
		b.then(argument("key", entryPath(modId, type, true)).executes(
		  remote? c -> resetPath(c, modId, tt)
		        : c -> resetClientPath(c, modId, tt)));
		builder.then(b);
		return builder;
	}
	
	private String getModId(CommandContext<?> ctx) {
		return ctx.getArgument("modId", String.class);
	}
	
	private Type getType(CommandContext<?> ctx) {
		return ctx.getArgument("type", EditType.class).getType();
	}
	
	private SimpleConfigImpl requireConfig(
	  CommandContext<CommandSourceStack> ctx, String modId, Type type, boolean forWrite
	) throws CommandSyntaxException {
		CommandSourceStack src = ctx.getSource();
		if (!SimpleConfigImpl.hasConfig(modId, type)) {
			throw UNSUPPORTED_CONFIG.create(modId);
		} else if (
		  forWrite? !getConfigPermission(src, modId).canEdit()
		          : !getConfigPermission(src, modId).canView()
		) throw NO_PERMISSION.create(modId);
		return SimpleConfigImpl.getConfig(modId, type);
	}
	
	private SimpleConfigImpl requireClientConfig(
	  String modId, Type type
	) throws CommandSyntaxException {
		if (!SimpleConfigImpl.hasConfig(modId, type))
			throw UNSUPPORTED_CONFIG.create(modId);
		return SimpleConfigImpl.getConfig(modId, type);
	}
	
	private static ConfigPermission getConfigPermission(CommandSourceStack src, String modId) {
		ServerPlayer player = src.getPlayer();
		return player != null
		       ? permissions.permissionFor(player, modId).getLeft()
		       : permissions.datapack_permission;
	}
	
	// Commands -------------------------------------------------------
	
	protected int getEntryValue(
	  CommandContext<CommandSourceStack> ctx, @Nullable String modId, @Nullable Type type
	) throws CommandSyntaxException {
		if (modId == null) modId = getModId(ctx);
		if (type == null) type = getType(ctx);
		SimpleConfigImpl config = requireConfig(ctx, modId, type, false);
		CommandSourceStack src = ctx.getSource();
		String key = ctx.getArgument("key", String.class);
		try {
			AbstractConfigEntry<Object, Object, Object> entry = config.getEntry(key);
			BaseCommand base = getBase(ctx, modId, 3);
			String serialized = entry.getForCommand();
			if (serialized == null) {
				src.sendFailure(Component.translatable("simpleconfig.command.error.get.unexpected"));
				LOGGER.error("Couldn't serialize entry value for command: {}", entry.getGlobalPath());
			}
			src.sendSuccess(Component.translatable("simpleconfig.command.msg.get",
			                                       formatKey(modId, key, type, 50), formatValue(base, type, key, serialized, 60)), false);
			return 0;
		} catch (NoSuchConfigEntryError e) {
			src.sendFailure(Component.translatable("simpleconfig.command.error.no_such_entry", formatKey(modId, key, type, 60)));
		} catch (RuntimeException e) {
			src.sendFailure(Component.translatable("simpleconfig.command.error.get.unexpected", formatKey(modId, key, type, 40)));
			LOGGER.error("Unexpected error getting config entry", e);
		}
		return 1;
	}
	
	protected int getClientEntryValue(
	  CommandContext<CommandSourceStack> ctx, @Nullable String modId, @Nullable Type type
	) throws CommandSyntaxException {
		if (modId == null) modId = getModId(ctx);
		if (type == null) type = getType(ctx);
		SimpleConfigImpl config = requireClientConfig(modId, type);
		CommandSourceStack src = ctx.getSource();
		String key = ctx.getArgument("key", String.class);
		try {
			AbstractConfigEntry<Object, Object, Object> entry = config.getEntry(key);
			BaseCommand base = getBase(ctx, modId, 3);
			String serialized = entry.getForCommand();
			if (serialized == null) {
				src.sendFailure(Component.translatable("simpleconfig.command.error.get.unexpected"));
				LOGGER.error("Couldn't serialize entry value for command: {}", entry.getGlobalPath());
			}
			src.sendSuccess(Component.translatable("simpleconfig.command.msg.get",
			                                       formatKey(modId, key, type, 50), formatValue(base, type, key, serialized, 60)), false);
			return 0;
		} catch (NoSuchConfigEntryError e) {
			src.sendFailure(Component.translatable("simpleconfig.command.error.no_such_entry", formatKey(modId, key, type, 60)));
		} catch (RuntimeException e) {
			src.sendFailure(Component.translatable("simpleconfig.command.error.get.unexpected", formatKey(modId, key, type, 40)));
			LOGGER.error("Unexpected error getting config entry", e);
		}
		return 1;
	}
	
	protected int setEntryValue(
	  CommandContext<CommandSourceStack> ctx, @Nullable String modId, @Nullable Type type
	) throws CommandSyntaxException {
		if (modId == null) modId = getModId(ctx);
		if (type == null) type = getType(ctx);
		SimpleConfigImpl config = requireConfig(ctx, modId, type, true);
		CommandSourceStack src = ctx.getSource();
		String key = ctx.getArgument("key", String.class);
		String value = ctx.getArgument("value", String.class);
		Player player = src.getPlayer();
		if (!getConfigPermission(src, config.getModId()).canEdit()) {
			src.sendFailure(Component.translatable("simpleconfig.command.error.no_permission", config.getModName()));
			return 1;
		}
		AbstractConfigEntry<Object, Object, Object> entry;
		try {
			entry = config.getEntry(key);
		} catch (NoSuchConfigEntryError e) {
			src.sendFailure(Component.translatable("simpleconfig.command.error.no_such_entry", formatKey(modId, key, type, 45)));
			return 1;
		}
		try {
			String prev = entry.getForCommand();
			BaseCommand base = getBase(ctx, modId, 4);
			String undoCommand = base.resolve("set", type.asEditType(isRemote()).getAlias(), key, prev);
			
			Optional<Component> error = entry.getErrorFromCommand(value);
			if (error.isPresent()) {
				src.sendFailure(Component.translatable("simpleconfig.command.error.set.invalid_value", error.get()));
				return -1;
			}
			
			entry.setFromCommand(value);
			value = entry.getForCommand();
			
			int valueWidth = min(55, value.length());
			int prevWidth = min(35 - valueWidth, prev.length());
			MutableComponent undoLink = genUndoLink(undoCommand),
			  formatvalue = formatValue(base, type, key, value, valueWidth),
			  formatPrev = formatValue(base, type, key, prev, prevWidth);
			src.sendSuccess(Component.translatable(
			  "simpleconfig.command.msg.set",
			  formatKey(modId, key, type, 45), undoLink, formatPrev, formatvalue), false);
			broadcastToOtherOperators(player, Component.translatable(
			  "simpleconfig.command.msg.set.remote",
			  playerName(player), formatKey(modId, key, type, 40), undoLink, formatPrev, formatvalue));
			// The file watcher isn't always reliable
			config.update();
			return 0;
		} catch (InvalidConfigValueTypeException e) {
			src.sendFailure(Component.translatable("simpleconfig.command.error.set.invalid_type", entry.getConfigCommentTooltip()));
		} catch (InvalidConfigValueException e) {
			src.sendFailure(Component.translatable("simpleconfig.command.error.set.invalid_value", entry.getConfigCommentTooltip()));
		} catch (RuntimeException e) {
			src.sendFailure(Component.translatable("simpleconfig.command.error.set.unexpected", formatKey(modId, key, type, 20)));
			LOGGER.error("Unexpected error setting config entry", e);
		}
		return 1;
	}
	
	protected int setClientEntryValue(
	  CommandContext<CommandSourceStack> ctx, @Nullable String modId, @Nullable Type type
	) throws CommandSyntaxException {
		if (modId == null) modId = getModId(ctx);
		if (type == null) type = getType(ctx);
		SimpleConfigImpl config = requireClientConfig(modId, type);
		CommandSourceStack src = ctx.getSource();
		String key = ctx.getArgument("key", String.class);
		String value = ctx.getArgument("value", String.class);
		
		AbstractConfigEntry<Object, Object, Object> entry;
		try {
			entry = config.getEntry(key);
		} catch (NoSuchConfigEntryError e) {
			src.sendFailure(Component.translatable("simpleconfig.command.error.no_such_entry", formatKey(modId, key, type, 45)));
			return 1;
		}
		try {
			String prev = entry.getForCommand();
			BaseCommand base = getBase(ctx, modId, 4);
			String undoCommand =
			  base.resolve("set", type.asEditType(isRemote()).getAlias(), key, prev);
			
			Optional<Component> error = entry.getErrorFromCommand(value);
			if (error.isPresent()) {
				src.sendFailure(Component.translatable("simpleconfig.command.error.set.invalid_value", error.get()));
				return -1;
			}
			
			entry.setFromCommand(value);
			value = entry.getForCommand();
			
			int valueWidth = min(40, value.length());
			int prevWidth = min(60 - valueWidth, prev.length());
			
			MutableComponent undoLink = genUndoLink(undoCommand),
			  formatvalue = formatValue(base, type, key, value, valueWidth),
			  formatPrev = formatValue(base, type, key, prev, prevWidth);
			src.sendSuccess(Component.translatable("simpleconfig.command.msg.set",
			                                       formatKey(modId, key, type, 45), undoLink, formatPrev, formatvalue), false);
			// The file watcher isn't always reliable
			config.update();
			// config.save();
			return 0;
		} catch (InvalidConfigValueTypeException e) {
			src.sendFailure(Component.translatable("simpleconfig.command.error.set.invalid_type", entry.getConfigCommentTooltip()));
		} catch (InvalidConfigValueException e) {
			src.sendFailure(Component.translatable("simpleconfig.command.error.set.invalid_value", entry.getConfigCommentTooltip()));
		} catch (RuntimeException e) {
			src.sendFailure(Component.translatable("simpleconfig.command.error.set.unexpected", formatKey(modId, key, type, 20)));
			LOGGER.error("Unexpected error setting config entry", e);
		}
		return 1;
	}
	
	protected int resetPath(
	  CommandContext<CommandSourceStack> ctx, @Nullable String modId, @Nullable Type type
	) throws CommandSyntaxException {
		if (modId == null) modId = getModId(ctx);
		if (type == null) type = getType(ctx);
		SimpleConfigImpl config = requireConfig(ctx, modId, type, true);
		CommandSourceStack src = ctx.getSource();
		String key = ctx.getArgument("key", String.class);
		Player player = src.getPlayer();
		if (!getConfigPermission(src, modId).canEdit()) {
			src.sendFailure(Component.translatable("simpleconfig.command.error.no_permission", config.getModName()));
			return 1;
		}
		try {
			if (config.hasEntry(key)) {
				AbstractConfigEntry<Object, Object, Object> entry = config.getEntry(key);
				String prev = entry.getForCommand();
				BaseCommand base = getBase(ctx, modId, 3);
				String undoCommand = base.resolve("set", type.asEditType(isRemote()).getAlias(), key, prev);
				config.reset(key);
				String value = entry.getForCommand();
				int valueWidth = min(40, value.length());
				int prevWidth = min(60 - valueWidth, prev.length());
				MutableComponent undoLink = genUndoLink(undoCommand);
				MutableComponent formatPrev =
				  formatValue(base, type, key, prev, prevWidth);
				MutableComponent formatValue =
				  formatValue(base, type, key, value, valueWidth);
				src.sendSuccess(Component.translatable("simpleconfig.command.msg.reset",
				                                       formatKey(modId, key, type, 45), undoLink, formatPrev, formatValue), false);
				broadcastToOtherOperators(player, Component.translatable("simpleconfig.command.msg.reset.remote",
				  playerName(player), formatKey(modId, key, type, 40), undoLink, formatPrev, formatValue));
				// The file watcher isn't always reliable
				config.update();
			} else if (config.hasChild(key)) {
				AbstractSimpleConfigEntryHolder group = config.getChild(key);
				group.reset();
				MutableComponent count = Component.literal(String.valueOf(group.getPaths(false).size()))
				  .withStyle(ChatFormatting.DARK_AQUA);
				src.sendSuccess(Component.translatable("simpleconfig.command.msg.reset_group",
				                                       formatKey(modId, key, type, 45), count), false);
				broadcastToOtherOperators(player, Component.translatable("simpleconfig.command.msg.reset_group.remote",
				  playerName(player), formatKey(modId, key, type, 45), count));
				// The file watcher isn't always reliable
				config.update();
			} else throw new NoSuchConfigEntryError(key);
			return 0;
		} catch (NoSuchConfigEntryError e) {
			src.sendFailure(Component.translatable("simpleconfig.command.error.no_such_entry", formatKey(modId, key, type, 50)));
		} catch (RuntimeException e) {
			src.sendFailure(Component.translatable("simpleconfig.command.error.reset.unexpected", formatKey(modId, key, type, 20)));
			LOGGER.error("Unexpected error resetting config entry", e);
		}
		return 1;
	}
	
	protected int resetClientPath(
	  CommandContext<CommandSourceStack> ctx, @Nullable String modId, @Nullable Type type
	) throws CommandSyntaxException {
		if (modId == null) modId = getModId(ctx);
		if (type == null) type = getType(ctx);
		SimpleConfigImpl config = requireClientConfig(modId, type);
		CommandSourceStack src = ctx.getSource();
		String key = ctx.getArgument("key", String.class);
		try {
			if (config.hasEntry(key)) {
				AbstractConfigEntry<Object, Object, Object> entry = config.getEntry(key);
				String prev = entry.getForCommand();
				BaseCommand base = getBase(ctx, modId, 3);
				String undoCommand =
				  base.resolve("set", type.asEditType(isRemote()).getAlias(), key, prev);
				config.reset(key);
				String value = entry.getForCommand();
				int valueWidth = min(40, value.length());
				int prevWidth = min(60 - valueWidth, prev.length());
				MutableComponent undoLink = genUndoLink(undoCommand);
				MutableComponent formatPrev =
				  formatValue(base, type, key, prev, prevWidth);
				MutableComponent formatValue =
				  formatValue(base, type, key, value, valueWidth);
				src.sendSuccess(Component.translatable("simpleconfig.command.msg.reset",
				                                       formatKey(modId, key, type, 45), undoLink, formatPrev, formatValue), false);
				// The file watcher isn't always reliable
				config.update();
			} else if (config.hasChild(key)) {
				AbstractSimpleConfigEntryHolder group = config.getChild(key);
				group.reset();
				MutableComponent count = Component.literal(String.valueOf(group.getPaths(false).size()))
				  .withStyle(ChatFormatting.DARK_AQUA);
				src.sendSuccess(Component.translatable("simpleconfig.command.msg.reset_group",
				                                       formatKey(modId, key, type, 45), count), false);
				// The file watcher isn't always reliable
				config.update();
			} else throw new NoSuchConfigEntryError(key);
			return 0;
		} catch (NoSuchConfigEntryError e) {
			src.sendFailure(Component.translatable("simpleconfig.command.error.no_such_entry", formatKey(modId, key, type, 50)));
		} catch (RuntimeException e) {
			src.sendFailure(Component.translatable("simpleconfig.command.error.reset.unexpected", formatKey(modId, key, type, 20)));
			LOGGER.error("Unexpected error resetting config entry", e);
		}
		return 1;
	}
	
	@OnlyIn(Dist.CLIENT) protected int openConfigGUI(
	  CommandContext<CommandSourceStack> ctx, @Nullable String modId
	) throws CommandSyntaxException {
		if (modId == null) modId = getModId(ctx);
		if (!SimpleConfigGUIManagerImpl.INSTANCE.hasConfigGUI(modId)) throw UNSUPPORTED_CONFIG.create(modId);
		String id = modId;
		clientTickExecutor.run(() -> SimpleConfigGUIManagerImpl.INSTANCE.showConfigGUI(id));
		return 0;
	}
	
	// Feedback -------------------------------------------------------
	
	public static void broadcastToOtherOperators(@Nullable Player player, Component message) {
		if (player == null && !permissions.broadcast_datapack_config_changes) return;
		ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayers().stream()
		  .filter(p -> p.hasPermissions(2) && p != player)
		  .forEach(p -> p.sendSystemMessage(message));
	}
	
	public static MutableComponent playerName(@Nullable Player player) {
		if (player == null) return Component.literal("<datapack>");
		return Component.literal(player.getName().getString())
		  .withStyle(ChatFormatting.DARK_GREEN).withStyle(style -> style
			 .withClickEvent(new ClickEvent(
				ClickEvent.Action.SUGGEST_COMMAND, "/msg " + player.getScoreboardName() + " "))
			 .withHoverEvent(new HoverEvent(
				HoverEvent.Action.SHOW_TEXT, player.getName().copy()
			   .append("\n").append(Component.literal(player.getStringUUID()).withStyle(ChatFormatting.GRAY))))
			 .withInsertion(player.getScoreboardName()));
	}
	
	protected MutableComponent wrap(
	  String text, int width, Style style,
	  @Nullable MutableComponent tooltipTitle,
	  @Nullable MutableComponent tooltipSubtitle,
	  @Nullable ClickEvent clickEvent
	) {
		if (clickEvent == null) {
			clickEvent = new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, text);
			if (tooltipSubtitle == null) tooltipSubtitle =
			  Component.literal("(").append(Component.translatable("chat.copy"))
				 .append(")").withStyle(ChatFormatting.GRAY);
		}
		final ClickEvent ce = clickEvent;
		MutableComponent wrapped;
		
		int length = text.length();
		if (length > width) {
			int cut = width < 23? width - 10 : 10;
			int rightCut = cut <= 0? 3 : length - width + cut + 3;
			MutableComponent ellipsis = Component.literal("...")
			  .withStyle(style).withStyle(ChatFormatting.GRAY);
			wrapped =
			  width <= 3
			  ? ellipsis : (cut <= 0? ellipsis : Component.literal(text.substring(0, cut))
			    .withStyle(style).append(ellipsis)).append(
				 Component.literal(text.substring(rightCut)).withStyle(style));
		} else wrapped = Component.literal(text).withStyle(style);
		
		MutableComponent tooltip = Component.literal("");
		if (tooltipTitle != null) tooltip.append(tooltipTitle).append("\n");
		tooltip.append(Component.literal(text).withStyle(style));
		if (tooltipSubtitle != null) tooltip.append("\n").append(tooltipSubtitle);
		
		wrapped.withStyle(s -> s.withHoverEvent(
		  new HoverEvent(Action.SHOW_TEXT, tooltip)
		).withClickEvent(ce));
		
		return wrapped;
	}
	
	protected MutableComponent formatKey(String modId, String key, Type type, int width) {
		return wrap(key, width, keyStyle, Component.literal(getModNameOrId(modId))
		  .withStyle(modNameStyle).append(" ").append(
		  Component.literal(type.getAlias()).withStyle(typeStyle)
		), null, null).withStyle(s -> s.withInsertion(key));
	}
	
	private static String getModNameOrId(String modId) {
		final Optional<IModInfo> first = ModList.get().getMods().stream()
		  .filter(m -> modId.equals(m.getModId())).findFirst();
		if (first.isPresent())
			return first.get().getDisplayName();
		return modId;
	}
	
	protected MutableComponent createCopyButton(String text) {
		return Component.literal("⧉").withStyle(s -> s
		  .applyFormat(ChatFormatting.DARK_GRAY)
		  .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable("chat.copy")
		    .withStyle(copyStyle))).withClickEvent(
			 new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, text)));
	}
	
	protected MutableComponent formatValue(
	  BaseCommand baseCommand, Type type, String key, String value, int width
	) {
		return wrap(
		  value, width, valueStyle, Component.literal(key).withStyle(keyStyle),
		  Component.literal("(").append(
			 Component.translatable("simpleconfig.command.help.set")).append(")").withStyle(ChatFormatting.GRAY), new ClickEvent(
			 ClickEvent.Action.SUGGEST_COMMAND,
			 baseCommand.resolve("set", type.asEditType(isRemote()).getAlias(), key, value))
		).append(" ").append(createCopyButton(value)).withStyle(s -> s.withInsertion(value));
	}
	
	protected MutableComponent genUndoLink(String undoCommand) {
		return Component.literal("(").append(
		  Component.translatable("simpleconfig.command.action.undo").withStyle(
			 s -> s.applyTo(undoStyle)
				.withHoverEvent(new HoverEvent(
				  HoverEvent.Action.SHOW_TEXT, Component.translatable("simpleconfig.command.help.undo")
				  .withStyle(ChatFormatting.GRAY))
				).withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, undoCommand)))
		).append(")").withStyle(ChatFormatting.GRAY);
	}
	
	protected BaseCommand getBase(CommandContext<?> ctx, String modId, int args) {
		String command = ctx.getInput();
		List<? extends ParsedCommandNode<?>> nodes = ctx.getNodes();
		if (args >= nodes.size()) throw new IllegalArgumentException(
		  "Not enough arguments in command, expected " + (args + 1) + " but found " + nodes.size() +
		  "\n  Command: " + command);
		StringBuilder b = new StringBuilder(command);
		int i;
		for (i = 1; i < args; i++) b.delete(
		  nodes.get(nodes.size() - i - 1).getRange().getEnd(),
		  nodes.get(nodes.size() - i).getRange().getEnd());
		
		// Check if the last node to remove is the modId
		StringRange modIdRange = nodes.get(nodes.size() - i).getRange();
		int end = modIdRange.getEnd();
		String id = b.substring(modIdRange.getStart(), end);
		b.delete(nodes.get(nodes.size() - i - 1).getRange().getEnd(), end);
		boolean hasModId = modId.equals(id);
		
		// In that case, remove one more
		if (hasModId) b.delete(
		  nodes.get(nodes.size() - ++i - 1).getRange().getEnd(),
		  nodes.get(nodes.size() - i).getRange().getEnd());
		b.insert(0, '/');
		return new BaseCommand(b.toString(), hasModId? modId : null);
	}
	
	private Predicate<CommandSourceStack> permission(String modId, boolean forWrite) {
		return forWrite? s -> getConfigPermission(s, modId).canEdit()
		               : s -> getConfigPermission(s, modId).canView();
	}
	
	protected static class BaseCommand {
		private final String base;
		private final @Nullable String modId;
		
		public BaseCommand(String base, @Nullable String modId) {
			this.base = base;
			this.modId = modId;
		}
		
		public String resolve(String first, String... args) {
			StringBuilder b = new StringBuilder(base);
			b.append(' ').append(first);
			if (modId != null) b.append(' ').append(modId);
			for (String arg: args) b.append(' ').append(arg);
			return b.toString();
		}
	}
	
	// GUI
	
	@OnlyIn(Dist.CLIENT) public static class CommandClientTickExecutor {
		private final List<Runnable> tickActions = new ArrayList<>();
		
		public CommandClientTickExecutor() {
			MinecraftForge.EVENT_BUS.register(this);
		}
		
		@SubscribeEvent public void onTick(ClientTickEvent event) {
			synchronized (tickActions) {
				tickActions.forEach(Runnable::run);
				tickActions.clear();
			}
		}
		
		public void run(Runnable runnable) {
			synchronized (tickActions) {
				tickActions.add(runnable);
			}
		}
		
		public void clearPending() {
			synchronized (tickActions) {
				tickActions.clear();
			}
		}
	}
	
	// Styles ---------------------------------------------------------
	
	public Style getKeyStyle() {
		return keyStyle;
	}
	public void setKeyStyle(Style keyStyle) {
		this.keyStyle = keyStyle;
	}
	
	public Style getModNameStyle() {
		return modNameStyle;
	}
	public void setModNameStyle(Style modNameStyle) {
		this.modNameStyle = modNameStyle;
	}
	
	public Style getTypeStyle() {
		return typeStyle;
	}
	public void setTypeStyle(Style typeStyle) {
		this.typeStyle = typeStyle;
	}
	
	public Style getValueStyle() {
		return valueStyle;
	}
	public void setValueStyle(Style valueStyle) {
		this.valueStyle = valueStyle;
	}
	
	public Style getUndoStyle() {
		return undoStyle;
	}
	public void setUndoStyle(Style undoStyle) {
		this.undoStyle = undoStyle;
	}
	
	public Style getCopyStyle() {
		return copyStyle;
	}
	public void setCopyStyle(Style copyStyle) {
		this.copyStyle = copyStyle;
	}
}
