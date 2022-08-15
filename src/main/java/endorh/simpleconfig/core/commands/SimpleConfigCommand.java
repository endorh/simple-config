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
import endorh.simpleconfig.config.ServerConfig.ConfigPermission;
import endorh.simpleconfig.config.ServerConfig.permissions;
import endorh.simpleconfig.core.AbstractConfigEntry;
import endorh.simpleconfig.core.AbstractSimpleConfigEntryHolder;
import endorh.simpleconfig.core.SimpleConfig;
import endorh.simpleconfig.core.SimpleConfig.*;
import endorh.simpleconfig.core.SimpleConfigModConfig;
import net.minecraft.command.CommandSource;
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
import static net.minecraft.command.Commands.argument;
import static net.minecraft.command.Commands.literal;

// Client commands aren't implemented until 1.18
@EventBusSubscriber(modid = SimpleConfigMod.MOD_ID)
public class SimpleConfigCommand {
	private static final SimpleConfigCommand INSTANCE = new SimpleConfigCommand();
	private static final DynamicCommandExceptionType UNSUPPORTED_CONFIG = new DynamicCommandExceptionType(
	  m -> new TranslationTextComponent("simpleconfig.command.error.unsupported_config", m));
	private static final DynamicCommandExceptionType NO_PERMISSION = new DynamicCommandExceptionType(
	  m -> new TranslationTextComponent("simpleconfig.command.error.no_permission", m));
	
	// Styles
	private Style keyStyle = Style.EMPTY.setColor(Color.fromInt(0xA080C0)).setItalic(true);
	private Style modNameStyle = Style.EMPTY.applyFormatting(TextFormatting.AQUA).setBold(false);
	private Style typeStyle = Style.EMPTY.applyFormatting(TextFormatting.LIGHT_PURPLE);
	private Style valueStyle = Style.EMPTY.applyFormatting(TextFormatting.DARK_AQUA);
	private Style undoStyle = Style.EMPTY.applyFormatting(TextFormatting.BLUE);
	private Style copyStyle = Style.EMPTY.setColor(Color.fromInt(0x808080));
	
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
		  literal("config").then(
			 literal("get").then(
				argument("modId", modId(isRemote())).then(getGetBuilder(null)))
		  ).then(
			 literal("set").then(
			   argument("modId", modId(isRemote())).then(getSetBuilder(null)))
		  ).then(
			 literal("reset").then(
			   argument("modId", modId(isRemote())).then(getResetBuilder(null))));
		dispatcher.register(command);
	}
	
	protected void registerRoot(
	  String modId, CommandDispatcher<CommandSource> dispatcher, LiteralArgumentBuilder<CommandSource> root
	) {
		// The open command is client only, so it's still unimplemented
		LiteralArgumentBuilder<CommandSource> command = root
		  .then(literal("get").requires(permission(modId, false)).then(getGetBuilder(modId))
		  ).then(literal("set").requires(permission(modId, true)).then(getSetBuilder(modId))
		  ).then(literal("reset").requires(permission(modId, true)).then(getResetBuilder(modId)));
		dispatcher.register(command);
	}
	
	private RequiredArgumentBuilder<CommandSource, EditType> getGetBuilder(@Nullable String modId) {
		return argument("type", type(modId, isRemote())).then(
		  argument("key", entryPath(modId, null, false))
			 .executes(s -> getEntryValue(s, modId)));
	}
	
	private RequiredArgumentBuilder<CommandSource, EditType> getSetBuilder(@Nullable String modId) {
		return argument("type", type(modId, isRemote())).then(
		  argument("key", entryPath(modId, null, false)).then(
			 argument("value", entryValue(modId, null))
				.executes(s -> setEntryValue(s, modId))));
	}
	
	private RequiredArgumentBuilder<CommandSource, EditType> getResetBuilder(@Nullable String modId) {
		return argument("type", type(modId, isRemote())).then(
		  argument("key", entryPath(modId, null, true))
			 .executes(s -> resetPath(s, modId)));
	}
	
	private String getModId(CommandContext<CommandSource> ctx) {
		return ctx.getArgument("modId", String.class);
	}
	
	private Type getType(CommandContext<CommandSource> ctx) {
		return ctx.getArgument("type", EditType.class).getType();
	}
	
	private SimpleConfig requireConfig(
	  CommandContext<CommandSource> ctx, String modId, Type type, boolean forWrite
	) throws CommandSyntaxException {
		PlayerEntity player = ctx.getSource().asPlayer();
		if (!SimpleConfig.hasConfig(modId, type)) {
			throw UNSUPPORTED_CONFIG.create(modId);
		} else if (
		  forWrite? !permissions.permissionFor(player, modId).getLeft().canEdit()
		          : !permissions.permissionFor(player, modId).getLeft().canView()
		) throw NO_PERMISSION.create(modId);
		return SimpleConfig.getConfig(modId, type);
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
		SimpleConfig config = requireConfig(ctx, modId, type, false);
		CommandSource src = ctx.getSource();
		String key = ctx.getArgument("key", String.class);
		try {
			AbstractConfigEntry<Object, Object, Object> entry = config.getEntry(key);
			BaseCommand base = getBase(ctx, modId, 3);
			String serialized = entry.getForCommand();
			if (serialized == null) src.sendErrorMessage(new TranslationTextComponent(
			  "simpleconfig.command.error.get.unexpected"));
			src.sendFeedback(new TranslationTextComponent(
			  "simpleconfig.command.msg.get",
			  formatKey(modId, key, type, 50), formatValue(base, type, key, serialized, 60)), false);
			return 0;
		} catch (NoSuchConfigEntryError e) {
			src.sendErrorMessage(new TranslationTextComponent(
			  "simpleconfig.command.error.no_such_entry", formatKey(modId, key, type, 60)));
		} catch (RuntimeException e) {
			src.sendErrorMessage(new TranslationTextComponent(
			  "simpleconfig.command.error.get.unexpected", formatKey(modId, key, type, 40)));
		}
		return 1;
	}
	
	protected int setEntryValue(
	  CommandContext<CommandSource> ctx, @Nullable String modId
	) throws CommandSyntaxException {
		if (modId == null) modId = getModId(ctx);
		Type type = getType(ctx);
		SimpleConfig config = requireConfig(ctx, modId, type, true);
		CommandSource src = ctx.getSource();
		String key = ctx.getArgument("key", String.class);
		String value = ctx.getArgument("value", String.class);
		PlayerEntity player = src.asPlayer();
		if (!permissions.permissionFor(player, config.getModId()).getLeft().canEdit()) {
			src.sendErrorMessage(new TranslationTextComponent(
			  "simpleconfig.command.error.no_permission", config.getModName()));
			return 1;
		}
		AbstractConfigEntry<Object, Object, Object> entry;
		try {
			entry = config.getEntry(key);
		} catch (NoSuchConfigEntryError e) {
			src.sendErrorMessage(new TranslationTextComponent(
			  "simpleconfig.command.error.no_such_entry", formatKey(modId, key, type, 45)));
			return 1;
		}
		try {
			String prev = entry.getForCommand();
			BaseCommand base = getBase(ctx, modId, 4);
			String undoCommand = base.resolve("set", type.asEditType(isRemote()).getAlias(), key, prev);
			
			Optional<ITextComponent> error = entry.getErrorFromCommand(value);
			if (error.isPresent()) {
				src.sendErrorMessage(new TranslationTextComponent(
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
			src.sendFeedback(new TranslationTextComponent(
			  "simpleconfig.command.msg.set",
			  formatKey(modId, key, type, 45), undoLink, formatPrev, formatvalue), false);
			broadcastToOtherOperators(player, new TranslationTextComponent(
			  "simpleconfig.command.msg.set.remote",
			  playerName(player), formatKey(modId, key, type, 40), undoLink, formatPrev, formatvalue));
			// The file watcher isn't always reliable
			config.sync();
			return 0;
		} catch (InvalidConfigValueTypeException e) {
			src.sendErrorMessage(new TranslationTextComponent(
			  "simpleconfig.command.error.set.invalid_type", entry.getConfigCommentTooltip()));
		} catch (InvalidConfigValueException e) {
			src.sendErrorMessage(new TranslationTextComponent(
			  "simpleconfig.command.error.set.invalid_value", entry.getConfigCommentTooltip()));
		} catch (RuntimeException e) {
			src.sendErrorMessage(new TranslationTextComponent(
			  "simpleconfig.command.error.set.unexpected", formatKey(modId, key, type, 20)));
		}
		return 1;
	}
	
	protected int resetPath(
	  CommandContext<CommandSource> ctx, @Nullable String modId
	) throws CommandSyntaxException {
		if (modId == null) modId = getModId(ctx);
		Type type = getType(ctx);
		SimpleConfig config = requireConfig(ctx, modId, type, true);
		CommandSource src = ctx.getSource();
		String key = ctx.getArgument("key", String.class);
		PlayerEntity player = src.asPlayer();
		if (!permissions.permissionFor(player, config.getModId()).getLeft().canEdit()) {
			src.sendErrorMessage(new TranslationTextComponent(
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
				src.sendFeedback(new TranslationTextComponent(
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
				).mergeStyle(TextFormatting.DARK_AQUA);
				src.sendFeedback(new TranslationTextComponent(
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
			src.sendErrorMessage(new TranslationTextComponent(
			  "simpleconfig.command.error.no_such_entry", formatKey(modId, key, type, 50)));
		} catch (RuntimeException e) {
			src.sendErrorMessage(new TranslationTextComponent(
			  "simpleconfig.command.error.reset.unexpected", formatKey(modId, key, type, 20)));
		}
		return 1;
	}
	
	// Feedback -------------------------------------------------------
	
	public static void broadcastToOtherOperators(PlayerEntity player, ITextComponent message) {
		ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayers().stream()
		  .filter(p -> p.hasPermissionLevel(2) && p != player)
		  .forEach(p -> p.sendMessage(message, Util.DUMMY_UUID));
	}
	
	public static IFormattableTextComponent playerName(PlayerEntity player) {
		return new StringTextComponent(player.getName().getString())
		  .mergeStyle(TextFormatting.DARK_GREEN).modifyStyle(style -> style
			 .setClickEvent(new ClickEvent(
				ClickEvent.Action.SUGGEST_COMMAND, "/msg " + player.getScoreboardName() + " "))
			 .setHoverEvent(new HoverEvent(
				HoverEvent.Action.SHOW_TEXT, player.getName().deepCopy()
			   .appendString("\n").append(new StringTextComponent(
				  player.getUniqueID().toString()).mergeStyle(TextFormatting.GRAY))))
			 .setInsertion(player.getScoreboardName()));
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
				 .appendString(")").mergeStyle(TextFormatting.GRAY);
		}
		final ClickEvent ce = clickEvent;
		IFormattableTextComponent wrapped;
		
		int length = text.length();
		if (length > width) {
			int cut = width < 23? width - 10 : 10;
			int rightCut = cut <= 0? 3 : length - width + cut + 3;
			IFormattableTextComponent ellipsis = new StringTextComponent("...")
			  .mergeStyle(style).mergeStyle(TextFormatting.GRAY);
			wrapped =
			  width <= 3
			  ? ellipsis : (cut <= 0? ellipsis : new StringTextComponent(
				 text.substring(0, cut)
			  ).mergeStyle(style).append(ellipsis)).append(
				 new StringTextComponent(text.substring(rightCut)).mergeStyle(style));
		} else wrapped = new StringTextComponent(text).mergeStyle(style);
		
		IFormattableTextComponent tooltip = new StringTextComponent("");
		if (tooltipTitle != null) tooltip.append(tooltipTitle).appendString("\n");
		tooltip.append(new StringTextComponent(text).mergeStyle(style));
		if (tooltipSubtitle != null) tooltip.appendString("\n").append(tooltipSubtitle);
		
		wrapped.modifyStyle(s -> s.setHoverEvent(
		  new HoverEvent(Action.SHOW_TEXT, tooltip)
		).setClickEvent(ce));
		
		return wrapped;
	}
	
	protected IFormattableTextComponent formatKey(String modId, String key, Type type, int width) {
		return wrap(key, width, keyStyle, new StringTextComponent(
		  getModNameOrId(modId)
		).mergeStyle(modNameStyle).appendString(" ").append(
		  new StringTextComponent(type.getAlias()).mergeStyle(typeStyle)
		), null, null).modifyStyle(s -> s.setInsertion(key));
	}
	
	private static String getModNameOrId(String modId) {
		final Optional<ModInfo> first = ModList.get().getMods().stream()
		  .filter(m -> modId.equals(m.getModId())).findFirst();
		if (first.isPresent())
			return first.get().getDisplayName();
		return modId;
	}
	
	protected IFormattableTextComponent createCopyButton(String text) {
		return new StringTextComponent("⧉").modifyStyle(s -> s
		  .applyFormatting(TextFormatting.DARK_GRAY)
		  .setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TranslationTextComponent(
			 "chat.copy"
		  ).mergeStyle(copyStyle))).setClickEvent(
			 new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, text)));
	}
	
	protected IFormattableTextComponent formatValue(
	  BaseCommand baseCommand, Type type, String key, String value, int width
	) {
		return wrap(
		  value, width, valueStyle, new StringTextComponent(key).mergeStyle(keyStyle),
		  new StringTextComponent("(").append(new TranslationTextComponent(
			 "simpleconfig.command.help.set"
		  )).appendString(")").mergeStyle(TextFormatting.GRAY), new ClickEvent(
			 ClickEvent.Action.SUGGEST_COMMAND,
			 baseCommand.resolve("set", type.asEditType(isRemote()).getAlias(), key, value))
		).appendString(" ").append(createCopyButton(value)).modifyStyle(s -> s.setInsertion(value));
	}
	
	protected IFormattableTextComponent genUndoLink(String undoCommand) {
		return new StringTextComponent("(").append(
		  new TranslationTextComponent("simpleconfig.command.action.undo").modifyStyle(
			 s -> s.mergeStyle(undoStyle)
				.setHoverEvent(new HoverEvent(
				  HoverEvent.Action.SHOW_TEXT, new TranslationTextComponent(
				  "simpleconfig.command.help.undo").mergeStyle(TextFormatting.GRAY))
				).setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, undoCommand)))
		).appendString(")").mergeStyle(TextFormatting.GRAY);
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
				ConfigPermission p = permissions.permissionFor(s.asPlayer(), modId).getLeft();
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
