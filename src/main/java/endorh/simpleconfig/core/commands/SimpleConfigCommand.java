package endorh.simpleconfig.core.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
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
import endorh.simpleconfig.core.SimpleConfigImpl;
import endorh.simpleconfig.core.SimpleConfigModConfig;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Util;
import net.minecraft.util.text.*;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraft.util.text.event.HoverEvent;
import net.minecraft.util.text.event.HoverEvent.Action;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.loading.moddiscovery.ModInfo;
import net.minecraftforge.fml.server.ServerLifecycleHooks;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static endorh.simpleconfig.core.SimpleConfigWrapper.getConfigs;
import static endorh.simpleconfig.core.commands.SimpleConfigKeyArgumentType.entryPath;
import static endorh.simpleconfig.core.commands.SimpleConfigModIdArgumentType.modId;
import static endorh.simpleconfig.core.commands.SimpleConfigTypeArgumentType.type;
import static endorh.simpleconfig.core.commands.SimpleConfigValueArgumentType.entryValue;
import static java.lang.Math.min;

// Client commands aren't implemented until 1.18
@EventBusSubscriber(modid = SimpleConfigMod.MOD_ID)
public class SimpleConfigCommand {
	private static final SimpleConfigCommand INSTANCE = new SimpleConfigCommand();
	private static final DynamicCommandExceptionType UNSUPPORTED_CONFIG = new DynamicCommandExceptionType(
	  m -> new TranslationTextComponent("simpleconfig.command.error.unsupported_config", m));
	private static final DynamicCommandExceptionType NO_PERMISSION = new DynamicCommandExceptionType(
	  m -> new TranslationTextComponent("simpleconfig.command.error.no_permission", m));
	
	// Styles
	private Style keyStyle = Style.EMPTY.withColor(Color.fromRgb(0xA080C0)).withItalic(true);
	private Style modNameStyle = Style.EMPTY.applyFormat(TextFormatting.AQUA).withBold(false);
	private Style typeStyle = Style.EMPTY.applyFormat(TextFormatting.LIGHT_PURPLE);
	private Style valueStyle = Style.EMPTY.applyFormat(TextFormatting.DARK_AQUA);
	private Style undoStyle = Style.EMPTY.applyFormat(TextFormatting.BLUE);
	private Style copyStyle = Style.EMPTY.withColor(Color.fromRgb(0x808080));
	
	protected boolean isRemote() {
		return true;
	}
	
	@SubscribeEvent public static void onRegisterCommands(RegisterCommandsEvent event) {
		CommandDispatcher<CommandSource> dispatcher = event.getDispatcher();
		INSTANCE.register(dispatcher);
		ModList.get().forEachModContainer((modId, container) -> {
			EnumMap<ModConfig.Type, ModConfig> configs = getConfigs(container);
			Stream.of(ModConfig.Type.SERVER, ModConfig.Type.COMMON).forEach(type -> {
				ModConfig config = configs.get(type);
				LiteralArgumentBuilder<CommandSource> root =
				  config instanceof SimpleConfigModConfig
				  ? ((SimpleConfigModConfig) config).getSimpleConfig().getCommandRoot() : null;
				if (root != null) INSTANCE.registerRoot(modId, dispatcher, root);
			});
		});
	}
	
	protected void register(CommandDispatcher<CommandSource> dispatcher) {
		// Register under global /config command
		// For the global command, the mod id is passed instead as "parameter" to the action
		LiteralArgumentBuilder<CommandSource> command =
		  Commands.literal("config").then(
		    Commands.literal("get").then(
			   Commands.argument("modId", modId(isRemote())).then(getGetBuilder(null)))
		  ).then(
		    Commands.literal("set").then(
			   Commands.argument("modId", modId(isRemote())).then(getSetBuilder(null)))
		  ).then(
		    Commands.literal("reset").then(
			   Commands.argument("modId", modId(isRemote())).then(getResetBuilder(null))));
		dispatcher.register(command);
	}
	
	protected void registerRoot(
	  String modId, CommandDispatcher<CommandSource> dispatcher, LiteralArgumentBuilder<CommandSource> root
	) {
		// The open command is client only, so it's still unimplemented
		LiteralArgumentBuilder<CommandSource> command = root
		  .then(Commands.literal("get").requires(permission(modId, false)).then(getGetBuilder(modId))
		  ).then(Commands.literal("set").requires(permission(modId, true)).then(getSetBuilder(modId))
		  ).then(
			 Commands.literal("reset").requires(permission(modId, true)).then(getResetBuilder(modId)));
		dispatcher.register(command);
	}
	
	private RequiredArgumentBuilder<CommandSource, EditType> getGetBuilder(@Nullable String modId) {
		return Commands.argument("type", type(modId, isRemote())).then(
		  Commands.argument("key", entryPath(modId, null, false))
			 .executes(s -> getEntryValue(s, modId)));
	}
	
	private RequiredArgumentBuilder<CommandSource, EditType> getSetBuilder(@Nullable String modId) {
		return Commands.argument("type", type(modId, isRemote())).then(
		  Commands.argument("key", entryPath(modId, null, false)).then(
			 Commands.argument("value", entryValue(modId, null))
				.executes(s -> setEntryValue(s, modId))));
	}
	
	private RequiredArgumentBuilder<CommandSource, EditType> getResetBuilder(@Nullable String modId) {
		return Commands.argument("type", type(modId, isRemote())).then(
		  Commands.argument("key", entryPath(modId, null, true))
			 .executes(s -> resetPath(s, modId)));
	}
	
	private String getModId(CommandContext<CommandSource> ctx) {
		return ctx.getArgument("modId", String.class);
	}
	
	private Type getType(CommandContext<CommandSource> ctx) {
		return ctx.getArgument("type", EditType.class).getType();
	}
	
	private SimpleConfigImpl requireConfig(
	  CommandContext<CommandSource> ctx, String modId, Type type, boolean forWrite
	) throws CommandSyntaxException {
		PlayerEntity player = ctx.getSource().getPlayerOrException();
		if (!SimpleConfigImpl.hasConfig(modId, type)) {
			throw UNSUPPORTED_CONFIG.create(modId);
		} else if (
		  forWrite? !permissions.permissionFor(player, modId).getLeft().canEdit()
		          : !permissions.permissionFor(player, modId).getLeft().canView()
		) throw NO_PERMISSION.create(modId);
		return SimpleConfigImpl.getConfig(modId, type);
	}
	
	// Commands -------------------------------------------------------
	
	@OnlyIn(Dist.CLIENT) protected int openGUI(CommandContext<CommandSource> ctx) {
		// Must wait until next tick, otherwise the GUI isn't properly initialized
		// ...
		// SimpleConfigGUIManager.showConfigGUI(config.getModId());
		return 0;
	}
	
	protected int getEntryValue(CommandContext<CommandSource> ctx, @Nullable String modId)
	  throws CommandSyntaxException {
		if (modId == null) modId = getModId(ctx);
		Type type = getType(ctx);
		SimpleConfigImpl config = requireConfig(ctx, modId, type, false);
		CommandSource src = ctx.getSource();
		String key = ctx.getArgument("key", String.class);
		try {
			AbstractConfigEntry<Object, Object, Object> entry = config.getEntry(key);
			BaseCommand base = getBase(ctx, modId, 3);
			String serialized = entry.getForCommand();
			if (serialized == null) src.sendFailure(new TranslationTextComponent(
			  "simpleconfig.command.error.get.unexpected"));
			src.sendSuccess(new TranslationTextComponent(
			  "simpleconfig.command.msg.get",
			  formatKey(modId, key, type, 50), formatValue(base, type, key, serialized, 60)), false);
			return 0;
		} catch (NoSuchConfigEntryError e) {
			src.sendFailure(new TranslationTextComponent(
			  "simpleconfig.command.error.no_such_entry", formatKey(modId, key, type, 60)));
		} catch (RuntimeException e) {
			src.sendFailure(new TranslationTextComponent(
			  "simpleconfig.command.error.get.unexpected", formatKey(modId, key, type, 40)));
		}
		return 1;
	}
	
	protected int setEntryValue(
	  CommandContext<CommandSource> ctx, @Nullable String modId
	) throws CommandSyntaxException {
		if (modId == null) modId = getModId(ctx);
		Type type = getType(ctx);
		SimpleConfigImpl config = requireConfig(ctx, modId, type, true);
		CommandSource src = ctx.getSource();
		String key = ctx.getArgument("key", String.class);
		String value = ctx.getArgument("value", String.class);
		PlayerEntity player = src.getPlayerOrException();
		if (!permissions.permissionFor(player, config.getModId()).getLeft().canEdit()) {
			src.sendFailure(new TranslationTextComponent(
			  "simpleconfig.command.error.no_permission", config.getModName()));
			return 1;
		}
		AbstractConfigEntry<Object, Object, Object> entry;
		try {
			entry = config.getEntry(key);
		} catch (NoSuchConfigEntryError e) {
			src.sendFailure(new TranslationTextComponent(
			  "simpleconfig.command.error.no_such_entry", formatKey(modId, key, type, 45)));
			return 1;
		}
		try {
			String prev = entry.getForCommand();
			BaseCommand base = getBase(ctx, modId, 4);
			String undoCommand = base.resolve("set", type.asEditType(isRemote()).getAlias(), key, prev);
			
			Optional<ITextComponent> error = entry.getErrorFromCommand(value);
			if (error.isPresent()) {
				src.sendFailure(new TranslationTextComponent(
				  "simpleconfig.command.error.set.invalid_value", error.get()));
				return -1;
			}
			
			entry.setFromCommand(value);
			value = entry.getForCommand();
			
			int valueWidth = min(40, value.length());
			int prevWidth = min(60 - valueWidth, prev.length());
			IFormattableTextComponent undoLink = genUndoLink(undoCommand),
			  formatvalue = formatValue(base, type, key, value, valueWidth),
			  formatPrev = formatValue(base, type, key, prev, prevWidth);
			src.sendSuccess(new TranslationTextComponent(
			  "simpleconfig.command.msg.set",
			  formatKey(modId, key, type, 45), undoLink, formatPrev, formatvalue), false);
			broadcastToOtherOperators(player, new TranslationTextComponent(
			  "simpleconfig.command.msg.set.remote",
			  playerName(player), formatKey(modId, key, type, 40), undoLink, formatPrev, formatvalue));
			// The file watcher isn't always reliable
			config.sync();
			return 0;
		} catch (InvalidConfigValueTypeException e) {
			src.sendFailure(new TranslationTextComponent(
			  "simpleconfig.command.error.set.invalid_type", entry.getConfigCommentTooltip()));
		} catch (InvalidConfigValueException e) {
			src.sendFailure(new TranslationTextComponent(
			  "simpleconfig.command.error.set.invalid_value", entry.getConfigCommentTooltip()));
		} catch (RuntimeException e) {
			src.sendFailure(new TranslationTextComponent(
			  "simpleconfig.command.error.set.unexpected", formatKey(modId, key, type, 20)));
		}
		return 1;
	}
	
	protected int resetPath(
	  CommandContext<CommandSource> ctx, @Nullable String modId
	) throws CommandSyntaxException {
		if (modId == null) modId = getModId(ctx);
		Type type = getType(ctx);
		SimpleConfigImpl config = requireConfig(ctx, modId, type, true);
		CommandSource src = ctx.getSource();
		String key = ctx.getArgument("key", String.class);
		PlayerEntity player = src.getPlayerOrException();
		if (!permissions.permissionFor(player, config.getModId()).getLeft().canEdit()) {
			src.sendFailure(new TranslationTextComponent(
			  "simpleconfig.command.error.no_permission", config.getModName()));
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
				IFormattableTextComponent undoLink = genUndoLink(undoCommand);
				IFormattableTextComponent formatPrev =
				  formatValue(base, type, key, prev, prevWidth);
				IFormattableTextComponent formatValue =
				  formatValue(base, type, key, value, valueWidth);
				src.sendSuccess(new TranslationTextComponent(
				  "simpleconfig.command.msg.reset",
				  formatKey(modId, key, type, 45), undoLink, formatPrev, formatValue), false);
				broadcastToOtherOperators(player, new TranslationTextComponent(
				  "simpleconfig.command.msg.reset.remote",
				  playerName(player), formatKey(modId, key, type, 40), undoLink, formatPrev, formatValue));
				// The file watcher isn't always reliable
				config.sync();
			} else if (config.hasChild(key)) {
				AbstractSimpleConfigEntryHolder group = config.getChild(key);
				group.reset();
				IFormattableTextComponent count = new StringTextComponent(
				  String.valueOf(group.getPaths(false).size())
				).withStyle(TextFormatting.DARK_AQUA);
				src.sendSuccess(new TranslationTextComponent(
				  "simpleconfig.command.msg.reset_group",
				  formatKey(modId, key, type, 45), count), false);
				broadcastToOtherOperators(player, new TranslationTextComponent(
				  "simpleconfig.command.msg.reset_group.remote",
				  playerName(player), formatKey(modId, key, type, 45), count));
				// The file watcher isn't always reliable
				config.sync();
			} else throw new NoSuchConfigEntryError(key);
			return 0;
		} catch (NoSuchConfigEntryError e) {
			src.sendFailure(new TranslationTextComponent(
			  "simpleconfig.command.error.no_such_entry", formatKey(modId, key, type, 50)));
		} catch (RuntimeException e) {
			src.sendFailure(new TranslationTextComponent(
			  "simpleconfig.command.error.reset.unexpected", formatKey(modId, key, type, 20)));
		}
		return 1;
	}
	
	// Feedback -------------------------------------------------------
	
	public static void broadcastToOtherOperators(PlayerEntity player, ITextComponent message) {
		ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayers().stream()
		  .filter(p -> p.hasPermissions(2) && p != player)
		  .forEach(p -> p.sendMessage(message, Util.NIL_UUID));
	}
	
	public static IFormattableTextComponent playerName(PlayerEntity player) {
		return new StringTextComponent(player.getName().getString())
		  .withStyle(TextFormatting.DARK_GREEN).withStyle(style -> style
			 .withClickEvent(new ClickEvent(
				ClickEvent.Action.SUGGEST_COMMAND, "/msg " + player.getScoreboardName() + " "))
			 .withHoverEvent(new HoverEvent(
				HoverEvent.Action.SHOW_TEXT, player.getName().copy()
			   .append("\n").append(new StringTextComponent(
				  player.getUUID().toString()).withStyle(TextFormatting.GRAY))))
			 .withInsertion(player.getScoreboardName()));
	}
	
	protected IFormattableTextComponent wrap(
	  String text, int width, Style style,
	  @Nullable IFormattableTextComponent tooltipTitle,
	  @Nullable IFormattableTextComponent tooltipSubtitle,
	  @Nullable ClickEvent clickEvent
	) {
		if (clickEvent == null) {
			clickEvent = new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, text);
			if (tooltipSubtitle == null) tooltipSubtitle =
			  new StringTextComponent("(").append(new TranslationTextComponent("chat.copy"))
				 .append(")").withStyle(TextFormatting.GRAY);
		}
		final ClickEvent ce = clickEvent;
		IFormattableTextComponent wrapped;
		
		int length = text.length();
		if (length > width) {
			int cut = width < 23? width - 10 : 10;
			int rightCut = cut <= 0? 3 : length - width + cut + 3;
			IFormattableTextComponent ellipsis = new StringTextComponent("...")
			  .withStyle(style).withStyle(TextFormatting.GRAY);
			wrapped =
			  width <= 3
			  ? ellipsis : (cut <= 0? ellipsis : new StringTextComponent(
				 text.substring(0, cut)
			  ).withStyle(style).append(ellipsis)).append(
				 new StringTextComponent(text.substring(rightCut)).withStyle(style));
		} else wrapped = new StringTextComponent(text).withStyle(style);
		
		IFormattableTextComponent tooltip = new StringTextComponent("");
		if (tooltipTitle != null) tooltip.append(tooltipTitle).append("\n");
		tooltip.append(new StringTextComponent(text).withStyle(style));
		if (tooltipSubtitle != null) tooltip.append("\n").append(tooltipSubtitle);
		
		wrapped.withStyle(s -> s.withHoverEvent(
		  new HoverEvent(Action.SHOW_TEXT, tooltip)
		).withClickEvent(ce));
		
		return wrapped;
	}
	
	protected IFormattableTextComponent formatKey(String modId, String key, Type type, int width) {
		return wrap(key, width, keyStyle, new StringTextComponent(
		  getModNameOrId(modId)
		).withStyle(modNameStyle).append(" ").append(
		  new StringTextComponent(type.getAlias()).withStyle(typeStyle)
		), null, null).withStyle(s -> s.withInsertion(key));
	}
	
	private static String getModNameOrId(String modId) {
		final Optional<ModInfo> first = ModList.get().getMods().stream()
		  .filter(m -> modId.equals(m.getModId())).findFirst();
		if (first.isPresent())
			return first.get().getDisplayName();
		return modId;
	}
	
	protected IFormattableTextComponent createCopyButton(String text) {
		return new StringTextComponent("⧉").withStyle(s -> s
		  .applyFormat(TextFormatting.DARK_GRAY)
		  .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TranslationTextComponent(
			 "chat.copy"
		  ).withStyle(copyStyle))).withClickEvent(
			 new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, text)));
	}
	
	protected IFormattableTextComponent formatValue(
	  BaseCommand baseCommand, Type type, String key, String value, int width
	) {
		return wrap(
		  value, width, valueStyle, new StringTextComponent(key).withStyle(keyStyle),
		  new StringTextComponent("(").append(new TranslationTextComponent(
			 "simpleconfig.command.help.set"
		  )).append(")").withStyle(TextFormatting.GRAY), new ClickEvent(
			 ClickEvent.Action.SUGGEST_COMMAND,
			 baseCommand.resolve("set", type.asEditType(isRemote()).getAlias(), key, value))
		).append(" ").append(createCopyButton(value)).withStyle(s -> s.withInsertion(value));
	}
	
	protected IFormattableTextComponent genUndoLink(String undoCommand) {
		return new StringTextComponent("(").append(
		  new TranslationTextComponent("simpleconfig.command.action.undo").withStyle(
			 s -> s.applyTo(undoStyle)
				.withHoverEvent(new HoverEvent(
				  HoverEvent.Action.SHOW_TEXT, new TranslationTextComponent(
				  "simpleconfig.command.help.undo").withStyle(TextFormatting.GRAY))
				).withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, undoCommand)))
		).append(")").withStyle(TextFormatting.GRAY);
	}
	
	protected BaseCommand getBase(CommandContext<CommandSource> ctx, String modId, int args) {
		String command = ctx.getInput();
		List<? extends ParsedCommandNode<CommandSource>> nodes = ctx.getNodes();
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
		return new BaseCommand(b.toString(), hasModId? modId : null);
	}
	
	private Predicate<CommandSource> permission(String modId, boolean forWrite) {
		return s -> {
			try {
				ConfigPermission p = permissions.permissionFor(s.getPlayerOrException(), modId).getLeft();
				return forWrite? p.canEdit() : p.canView();
			} catch (CommandSyntaxException e) {
				return false;
			}
		};
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
