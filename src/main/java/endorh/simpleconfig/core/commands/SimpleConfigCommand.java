package endorh.simpleconfig.core.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.context.ParsedCommandNode;
import com.mojang.brigadier.context.StringRange;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import endorh.simpleconfig.SimpleConfigMod;
import endorh.simpleconfig.SimpleConfigMod.ConfigPermission;
import endorh.simpleconfig.SimpleConfigMod.ServerConfig.permissions;
import endorh.simpleconfig.core.AbstractConfigEntry;
import endorh.simpleconfig.core.AbstractSimpleConfigEntryHolder;
import endorh.simpleconfig.core.SimpleConfig;
import endorh.simpleconfig.core.SimpleConfig.InvalidConfigValueException;
import endorh.simpleconfig.core.SimpleConfig.InvalidConfigValueTypeException;
import endorh.simpleconfig.core.SimpleConfig.NoSuchConfigEntryError;
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
import net.minecraftforge.fml.config.ModConfig.Type;
import net.minecraftforge.fml.server.ServerLifecycleHooks;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import static endorh.simpleconfig.core.SimpleConfigWrapper.getConfigs;
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
	private Style valueStyle = Style.EMPTY.applyFormatting(TextFormatting.DARK_AQUA);
	private Style undoStyle = Style.EMPTY.applyFormatting(TextFormatting.BLUE);
	private Style copyStyle = Style.EMPTY.setColor(Color.fromInt(0x808080));
	
	protected String getType() {
		return Type.SERVER.extension();
	}
	
	@SubscribeEvent public static void onRegisterCommands(RegisterCommandsEvent event) {
		CommandDispatcher<CommandSource> dispatcher = event.getDispatcher();
		INSTANCE.register(dispatcher);
		ModList.get().forEachModContainer((modId, container) -> {
			EnumMap<Type, ModConfig> configs = getConfigs(container);
			ModConfig config = configs.get(Type.SERVER);
			LiteralArgumentBuilder<CommandSource> root =
			  config instanceof SimpleConfigModConfig
			  ? ((SimpleConfigModConfig) config).getSimpleConfig().getCommandRoot() : null;
			if (root != null) INSTANCE.registerRoot(modId, dispatcher, root);
		});
	}
	
	protected void register(
	  CommandDispatcher<CommandSource> dispatcher
	) {
		
		// Register under global /config command
		// For the global command, the mod id is passed instead as "parameter" to the action
		LiteralArgumentBuilder<CommandSource> command =
		  literal("config").then(
			 literal("get").then(
				argument("modId", modId()).then(getGetBuilder(null)))
		  ).then(
			 literal("set").then(
			   argument("modId", modId()).then(getSetBuilder(null)))
		  ).then(
			 literal("reset").then(
			   argument("modId", modId()).then(getResetBuilder(null))));
		dispatcher.register(command);
	}
	
	@NotNull private static SimpleConfigModIdArgumentType modId() {
		return SimpleConfigModIdArgumentType.modId(Type.SERVER);
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
	
	private LiteralArgumentBuilder<CommandSource> getGetBuilder(@Nullable String modId) {
		return literal(getType()).then(
		  argument("key", entryPath(modId, false))
			 .executes(s -> getEntryValue(s, modId)));
	}
	
	private LiteralArgumentBuilder<CommandSource> getSetBuilder(@Nullable String modId) {
		return literal(getType()).then(
		  argument("key", entryPath(modId, false)).then(
			 argument("value", entryValue(modId))
				.executes(s -> setEntryValue(s, modId))));
	}
	
	private LiteralArgumentBuilder<CommandSource> getResetBuilder(@Nullable String modId) {
		return literal(getType()).then(
		  argument("key", entryPath(modId, true))
			 .executes(s -> resetPath(s, modId)));
	}
	
	private SimpleConfigKeyArgumentType entryPath(@Nullable String modId, boolean includeGroups) {
		return SimpleConfigKeyArgumentType.entryPath(modId, Type.SERVER, includeGroups);
	}
	
	@NotNull private SimpleConfigValueArgumentType entryValue(@Nullable String modId) {
		return SimpleConfigValueArgumentType.entryValue(modId, Type.SERVER);
	}
	
	private String getModId(CommandContext<CommandSource> ctx) {
		return ctx.getArgument("modId", String.class);
	}
	
	private SimpleConfig requireConfig(
	  CommandContext<CommandSource> ctx, String modId, boolean forWrite
	) throws CommandSyntaxException {
		PlayerEntity player = ctx.getSource().asPlayer();
		if (!SimpleConfig.hasConfig(modId, Type.SERVER)) {
			throw UNSUPPORTED_CONFIG.create(modId);
		} else if (
		  forWrite? !permissions.permissionFor(player, modId).getLeft().canEdit()
		          : !permissions.permissionFor(player, modId).getLeft().canView()
		) throw NO_PERMISSION.create(modId);
		return SimpleConfig.getConfig(modId, Type.SERVER);
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
		SimpleConfig config = requireConfig(ctx, modId, false);
		CommandSource src = ctx.getSource();
		String key = ctx.getArgument("key", String.class);
		try {
			AbstractConfigEntry<Object, Object, Object, ?> entry = config.getEntry(key);
			BaseCommand base = getBase(ctx, modId, 3);
			String serialized = entry.getForCommand();
			if (serialized == null) src.sendErrorMessage(new TranslationTextComponent(
			  "simpleconfig.command.error.get.unexpected"));
			src.sendFeedback(new TranslationTextComponent(
			  "simpleconfig.command.msg.get",
			  formatKey(modId, key, 50), formatValue(base, key, serialized, 60)), false);
			return 0;
		} catch (NoSuchConfigEntryError e) {
			src.sendErrorMessage(new TranslationTextComponent(
			  "simpleconfig.command.error.no_such_entry", formatKey(modId, key, 60)));
		} catch (RuntimeException e) {
			src.sendErrorMessage(new TranslationTextComponent(
			  "simpleconfig.command.error.get.unexpected", formatKey(modId, key, 40)));
		}
		return 1;
	}
	
	protected int setEntryValue(
	  CommandContext<CommandSource> ctx, @Nullable String modId
	) throws CommandSyntaxException {
		if (modId == null) modId = getModId(ctx);
		SimpleConfig config = requireConfig(ctx, modId, true);
		CommandSource src = ctx.getSource();
		String key = ctx.getArgument("key", String.class);
		String value = ctx.getArgument("value", String.class);
		PlayerEntity player = src.asPlayer();
		if (!permissions.permissionFor(player, config.getModId()).getLeft().canEdit()) {
			src.sendErrorMessage(new TranslationTextComponent(
			  "simpleconfig.command.error.no_permission", config.getModName()));
			return 1;
		}
		AbstractConfigEntry<Object, Object, Object, ?> entry;
		try {
			entry = config.getEntry(key);
		} catch (NoSuchConfigEntryError e) {
			src.sendErrorMessage(new TranslationTextComponent(
			  "simpleconfig.command.error.no_such_entry", formatKey(modId, key, 45)));
			return 1;
		}
		try {
			String prev = entry.getForCommand();
			BaseCommand base = getBase(ctx, modId, 4);
			String undoCommand = base.resolve("set", getType(), key, prev);
			
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
			  formatvalue = formatValue(base, key, value, valueWidth),
			  formatPrev = formatValue(base, key, prev, prevWidth);
			src.sendFeedback(new TranslationTextComponent(
			  "simpleconfig.command.msg.set",
			  formatKey(modId, key, 45), undoLink, formatPrev, formatvalue), false);
			broadcastToOtherOperators(player, new TranslationTextComponent(
			  "simpleconfig.command.msg.set.remote",
			  playerName(player), formatKey(modId, key, 40), undoLink, formatPrev, formatvalue));
			return 0;
		} catch (InvalidConfigValueTypeException e) {
			src.sendErrorMessage(new TranslationTextComponent(
			  "simpleconfig.command.error.set.invalid_type", entry.getConfigCommentTooltip()));
		} catch (InvalidConfigValueException e) {
			src.sendErrorMessage(new TranslationTextComponent(
			  "simpleconfig.command.error.set.invalid_value", entry.getConfigCommentTooltip()));
		} catch (RuntimeException e) {
			src.sendErrorMessage(new TranslationTextComponent(
			  "simpleconfig.command.error.set.unexpected", formatKey(modId, key, 20)));
		}
		return 1;
	}
	
	protected int resetPath(
	  CommandContext<CommandSource> ctx, @Nullable String modId
	) throws CommandSyntaxException {
		if (modId == null) modId = getModId(ctx);
		SimpleConfig config = requireConfig(ctx, modId, true);
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
				AbstractConfigEntry<Object, Object, Object, ?> entry = config.getEntry(key);
				String prev = entry.getForCommand();
				BaseCommand base = getBase(ctx, modId, 3);
				String undoCommand = base.resolve("set", getType(), key, prev);
				config.reset(key);
				String value = entry.getForCommand();
				int valueWidth = min(40, value.length());
				int prevWidth = min(60 - valueWidth, prev.length());
				IFormattableTextComponent undoLink = genUndoLink(undoCommand);
				IFormattableTextComponent formatPrev =
				  formatValue(base, key, prev, prevWidth);
				IFormattableTextComponent formatValue =
				  formatValue(base, key, value, valueWidth);
				src.sendFeedback(new TranslationTextComponent(
				  "simpleconfig.command.msg.reset",
				  formatKey(modId, key, 45), undoLink, formatPrev, formatValue), false);
				broadcastToOtherOperators(player, new TranslationTextComponent(
				  "simpleconfig.command.msg.reset.remote",
				  playerName(player), formatKey(modId, key, 40), undoLink, formatPrev, formatValue));
			} else if (config.hasChild(key)) {
				AbstractSimpleConfigEntryHolder group = config.getChild(key);
				group.reset();
				IFormattableTextComponent count = new StringTextComponent(
				  String.valueOf(group.getPaths(false).size())
				).mergeStyle(TextFormatting.DARK_AQUA);
				src.sendFeedback(new TranslationTextComponent(
				  "simpleconfig.command.msg.reset_group",
				  formatKey(modId, key, 45), count), false);
				broadcastToOtherOperators(player, new TranslationTextComponent(
				  "simpleconfig.command.msg.reset_group.remote",
				  playerName(player), formatKey(modId, key, 45), count));
			} else throw new NoSuchConfigEntryError(key);
			return 0;
		} catch (NoSuchConfigEntryError e) {
			src.sendErrorMessage(new TranslationTextComponent(
			  "simpleconfig.command.error.no_such_entry", formatKey(modId, key, 50)));
		} catch (RuntimeException e) {
			src.sendErrorMessage(new TranslationTextComponent(
			  "simpleconfig.command.error.reset.unexpected", formatKey(modId, key, 20)));
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
				ClickEvent.Action.SUGGEST_COMMAND, "/tell " + player.getScoreboardName()))
			 .setHoverEvent(new HoverEvent(
				HoverEvent.Action.SHOW_TEXT, player.getName().deepCopy()
			   .appendString("\n").append(new StringTextComponent(
				  player.getUniqueID().toString()).mergeStyle(TextFormatting.GRAY)))));
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
	
	protected IFormattableTextComponent formatKey(String modId, String key, int width)
	  throws CommandSyntaxException {
		return wrap(key, width, keyStyle, new StringTextComponent(
		  SimpleConfig.getModNameOrId(modId)
		).mergeStyle(modNameStyle), null, null);
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
	  BaseCommand baseCommand, String key, String value, int width
	) {
		return wrap(
		  value, width, valueStyle, new StringTextComponent(key).mergeStyle(keyStyle),
		  new StringTextComponent("(").append(new TranslationTextComponent(
			 "simpleconfig.command.help.set"
		  )).appendString(")").mergeStyle(TextFormatting.GRAY), new ClickEvent(
			 ClickEvent.Action.SUGGEST_COMMAND,
			 baseCommand.resolve("set", getType(), key, value))
		).appendString(" ").append(createCopyButton(value));
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
}
