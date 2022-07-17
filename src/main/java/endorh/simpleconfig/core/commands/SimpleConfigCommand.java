package endorh.simpleconfig.core.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.context.ParsedCommandNode;
import com.mojang.brigadier.context.StringRange;
import endorh.simpleconfig.core.AbstractConfigEntry;
import endorh.simpleconfig.core.AbstractSimpleConfigEntryHolder;
import endorh.simpleconfig.core.SimpleConfig;
import endorh.simpleconfig.core.SimpleConfig.InvalidConfigValueException;
import endorh.simpleconfig.core.SimpleConfig.InvalidConfigValueTypeException;
import endorh.simpleconfig.core.SimpleConfig.NoSuchConfigEntryError;
import net.minecraft.command.CommandSource;
import net.minecraft.util.text.*;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraft.util.text.event.HoverEvent;
import net.minecraft.util.text.event.HoverEvent.Action;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.config.ModConfig.Type;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static endorh.simpleconfig.core.commands.SimpleConfigKeyArgumentType.entryPath;
import static endorh.simpleconfig.core.commands.SimpleConfigValueArgumentType.entryValue;
import static java.lang.Math.min;
import static net.minecraft.command.Commands.argument;
import static net.minecraft.command.Commands.literal;

public class SimpleConfigCommand {
	private static final Map<SimpleConfig, SimpleConfigCommand> COMMANDS = new HashMap<>();
	private final SimpleConfig config;
	private final LiteralArgumentBuilder<CommandSource> root;
	
	// Styles
	private Style keyStyle = Style.EMPTY.setColor(Color.fromInt(0xFFA080C0)).setItalic(true);
	private Style modNameStyle = Style.EMPTY.applyFormatting(TextFormatting.AQUA).setBold(false);
	private Style valueStyle = Style.EMPTY.applyFormatting(TextFormatting.DARK_AQUA);
	private Style undoStyle = Style.EMPTY.applyFormatting(TextFormatting.BLUE);
	private Style copyStyle = Style.EMPTY.setColor(Color.fromInt(0xFF808080));
	
	public SimpleConfigCommand(SimpleConfig config, LiteralArgumentBuilder<CommandSource> root) {
		this.config = config;
		this.root = root;
		MinecraftForge.EVENT_BUS.register(this);
	}
	
	@SubscribeEvent public void onRegisterCommands(RegisterCommandsEvent event) {
		register(event.getDispatcher());
	}
	
	protected String getType() {
		return config.getType().extension();
	}
	
	protected void register(
	  CommandDispatcher<CommandSource> dispatcher
	) {
		// Client commands aren't implemented until 1.18
		if (config.getType() != Type.SERVER) return;
		String type = getType();
		LiteralArgumentBuilder<CommandSource> getBuilder = literal(type).then(
		  argument("key", entryPath(config, false))
			 .executes(this::getEntryValue));
		LiteralArgumentBuilder<CommandSource> setBuilder = literal(type).then(
		  argument("key", entryPath(config, false)).then(
			 argument("value", entryValue(config))
				.executes(this::setEntryValue)));
		LiteralArgumentBuilder<CommandSource> resetBuilder = literal(type).then(
		  argument("key", entryPath(config, true))
			 .executes(this::resetPath));
		
		// Register under custom root
		if (root != null) {
			// The open command is client only, so it's still unimplemented
			LiteralArgumentBuilder<CommandSource> command = root
			  .then(literal("get").then(getBuilder)
			).then(literal("set").then(setBuilder)
			).then(literal("reset").then(resetBuilder));
			dispatcher.register(command);
		}
		
		// Register under global /config command
		// For the global command, the mod id is passed instead as "parameter" to the action
		String modId = config.getModId();
		LiteralArgumentBuilder<CommandSource> command =
		  literal("config").then(
			 literal("get").then(
				literal(modId).then(getBuilder))
		  ).then(
			 literal("set").then(
				literal(modId).then(setBuilder))
		  ).then(
			 literal("reset").then(
				literal(modId).then(resetBuilder)));
		dispatcher.register(command);
	}
	
	// Commands -------------------------------------------------------
	
	@OnlyIn(Dist.CLIENT) protected int openGUI(CommandContext<CommandSource> ctx) {
		// Must wait until next tick, otherwise the GUI isn't properly initialized
		// ...
		// SimpleConfigGUIManager.showConfigGUI(config.getModId());
		return 0;
	}
	
	protected int getEntryValue(CommandContext<CommandSource> ctx) {
		CommandSource src = ctx.getSource();
		String key = ctx.getArgument("key", String.class);
		try {
			AbstractConfigEntry<Object, Object, Object, ?> entry = config.getEntry(key);
			BaseCommand base = getBase(ctx, 3);
			String serialized = entry.getForCommand();
			if (serialized == null) src.sendErrorMessage(new TranslationTextComponent(
			  "simpleconfig.command.error.get.unexpected"));
			src.sendFeedback(new TranslationTextComponent(
			  "simpleconfig.command.msg.get",
			  formatKey(key, 50), formatValue(base, key, serialized, 60)), false);
			return 0;
		} catch (NoSuchConfigEntryError e) {
			src.sendErrorMessage(new TranslationTextComponent(
			  "simpleconfig.command.error.no_such_entry", formatKey(key, 60)));
		} catch (RuntimeException e) {
			src.sendErrorMessage(new TranslationTextComponent(
			  "simpleconfig.command.error.get.unexpected", formatKey(key, 40)));
		}
		return 1;
	}
	
	protected int setEntryValue(CommandContext<? extends CommandSource> ctx) {
		CommandSource src = ctx.getSource();
		String key = ctx.getArgument("key", String.class);
		String value = ctx.getArgument("value", String.class);
		AbstractConfigEntry<Object, Object, Object, ?> entry;
		try {
			entry = config.getEntry(key);
		} catch (NoSuchConfigEntryError e) {
			src.sendErrorMessage(new TranslationTextComponent(
			  "simpleconfig.command.error.no_such_entry", formatKey(key, 45)));
			return 1;
		}
		try {
			String prev = entry.getForCommand();
			BaseCommand base = getBase(ctx, 4);
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
			src.sendFeedback(new TranslationTextComponent(
			  "simpleconfig.command.msg.set",
			  formatKey(key, 45), genUndoLink(undoCommand),
			  formatValue(base, key, prev, prevWidth), formatValue(base, key, value, valueWidth)
			), false);
			return 0;
		} catch (InvalidConfigValueTypeException e) {
			src.sendErrorMessage(new TranslationTextComponent(
			  "simpleconfig.command.error.set.invalid_type", entry.getConfigCommentTooltip()));
		} catch (InvalidConfigValueException e) {
			src.sendErrorMessage(new TranslationTextComponent(
			  "simpleconfig.command.error.set.invalid_value", entry.getConfigCommentTooltip()));
		} catch (RuntimeException e) {
			src.sendErrorMessage(new TranslationTextComponent(
			  "simpleconfig.command.error.set.unexpected", formatKey(key, 20)));
		}
		return 1;
	}
	
	protected int resetPath(CommandContext<? extends CommandSource> ctx) {
		CommandSource src = ctx.getSource();
		String key = ctx.getArgument("key", String.class);
		try {
			if (config.hasEntry(key)) {
				AbstractConfigEntry<Object, Object, Object, ?> entry = config.getEntry(key);
				String prev = entry.getForCommand();
				BaseCommand base = getBase(ctx, 3);
				String undoCommand = base.resolve("set", getType(), key, prev);
				config.reset(key);
				String value = entry.getForCommand();
				int valueWidth = min(40, value.length());
				int prevWidth = min(60 - valueWidth, prev.length());
				src.sendFeedback(new TranslationTextComponent(
				  "simpleconfig.command.msg.reset",
				  formatKey(key, 45), genUndoLink(undoCommand),
				  formatValue(base, key, prev, prevWidth), formatValue(base, key, value, valueWidth)
				), false);
			} else if (config.hasChild(key)) {
				AbstractSimpleConfigEntryHolder group = config.getChild(key);
				group.reset();
				src.sendFeedback(new TranslationTextComponent(
				  "simpleconfig.command.msg.reset_group", formatKey(key, 45),
				  group.getPaths(false).size()), false);
			} else throw new NoSuchConfigEntryError(key);
			return 0;
		} catch (NoSuchConfigEntryError e) {
			src.sendErrorMessage(new TranslationTextComponent(
			  "simpleconfig.command.error.no_such_entry", formatKey(key, 50)));
		} catch (RuntimeException e) {
			src.sendErrorMessage(new TranslationTextComponent(
			  "simpleconfig.command.error.reset.unexpected", formatKey(key, 20)));
		}
		return 1;
	}
	
	// Feedback -------------------------------------------------------
	
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
	
	protected IFormattableTextComponent formatKey(String key, int width) {
		return wrap(key, width, keyStyle, new StringTextComponent(
		  config.getModName()
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
	
	protected BaseCommand getBase(CommandContext<? extends CommandSource> ctx, int args) {
		String command = ctx.getInput();
		List<? extends ParsedCommandNode<? extends CommandSource>> nodes = ctx.getNodes();
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
		String modId = b.substring(modIdRange.getStart(), end);
		b.delete(nodes.get(nodes.size() - i - 1).getRange().getEnd(), end);
		boolean hasModId = config.getModId().equals(modId);
		
		// In that case, remove one more
		if (hasModId) b.delete(
		  nodes.get(nodes.size() - ++i - 1).getRange().getEnd(),
		  nodes.get(nodes.size() - i).getRange().getEnd());
		return new BaseCommand(b.toString(), hasModId? modId : null);
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
