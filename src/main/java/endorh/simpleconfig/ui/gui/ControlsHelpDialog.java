package endorh.simpleconfig.ui.gui;

import com.google.common.collect.Lists;
import endorh.simpleconfig.api.ui.hotkey.KeyBindMapping;
import endorh.simpleconfig.api.ui.icon.SimpleConfigIcons;
import endorh.simpleconfig.ui.gui.widget.CheckboxButton;
import endorh.simpleconfig.ui.gui.widget.MultiFunctionIconButton;
import endorh.simpleconfig.ui.gui.widget.MultiFunctionImageButton.ButtonAction;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.controls.ControlsScreen;
import net.minecraft.network.chat.*;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ControlsHelpDialog extends ConfirmDialog {
	protected ControlsHelpDialog(Component title) {
		super(title);
	}
	
	public static Builder of(String prefix) {
		return of(prefix, new TranslatableComponent("simpleconfig.ui.controls"));
	}
	
	public static Builder of(String prefix, Component title) {
		return new Builder(prefix, title);
	}
	
	public static class Builder {
		private static final Pattern COMMA = Pattern.compile("\\s*,\\s*");
		private static final Pattern SLASH = Pattern.compile("\\s*/\\s*");
		private final Component title;
		private final String prefix;
		private final List<Component> lines = Lists.newArrayList();
		private Style categoryStyle = Style.EMPTY.withBold(true).withUnderlined(true);
		private Style keyStyle = Style.EMPTY.withColor(ChatFormatting.DARK_AQUA);
		private Style keyBindStyle = Style.EMPTY.withColor(ChatFormatting.DARK_GREEN);
		private Style unboundKeyBindStyle = Style.EMPTY.withColor(ChatFormatting.DARK_RED);
		private Style modifierStyle = Style.EMPTY.withColor(ChatFormatting.DARK_PURPLE);
		private Style helpStyle = Style.EMPTY.withColor(ChatFormatting.GRAY);
		private CheckboxButton[] checkBoxes = new CheckboxButton[0];
		private ComplexDialogAction action = (v, s) -> {};
		private Component confirmText = CommonComponents.GUI_DONE;
		private @Nullable Function<Screen, Screen> controlsScreenSupplier = s -> new ControlsScreen(s, Minecraft.getInstance().options);
		
		private Builder(String prefix, Component title) {
			this.prefix = prefix.endsWith(".")? prefix : prefix + ".";
			this.title = title;
		}
		
		private MutableComponent parseSingle(String key) {
			return KeyBindMapping.parse(key).getDisplayName(keyStyle).copy();
			// if (key.endsWith("!"))
			// 	return ModifierKeyCode.parse(key.substring(0, key.length() - 1))
			// 	  .getLayoutAgnosticLocalizedName(modifierStyle, keyStyle).deepCopy();
			// return ModifierKeyCode.parse(key).getLocalizedName(modifierStyle, keyStyle).deepCopy();
		}
		
		private MutableComponent parse(String keys) {
			final MutableComponent comma = new TextComponent(", ").withStyle(keyStyle);
			final MutableComponent slash = new TextComponent("/").withStyle(keyStyle);
			return Arrays.stream(COMMA.split(keys)).map(
			  s -> Arrays.stream(SLASH.split(s))
			    .map(this::parseSingle)
				 .reduce((a, b) -> a.append(slash).append(b)).orElse(TextComponent.EMPTY.copy())
			).reduce((a, b) -> a.append(comma).append(b)).orElse(TextComponent.EMPTY.copy());
		}
		
		private static final Pattern TITLE_CASE_PATTERN = Pattern.compile("(?<!\\w)\\w");
		private static String toTitleCase(String s) {
			final Matcher m = TITLE_CASE_PATTERN.matcher(s.toLowerCase());
			final StringBuffer sb = new StringBuffer();
			while (m.find()) m.appendReplacement(sb, m.group().toUpperCase());
			m.appendTail(sb);
			return sb.toString();
		}
		
		public Builder withCategoryStyle(Function<Style, Style> styleModifier) {
			categoryStyle = styleModifier.apply(categoryStyle);
			return this;
		}
		
		public Builder withKeyStyle(Function<Style, Style> styleModifier) {
			keyStyle = styleModifier.apply(keyStyle);
			return this;
		}
		
		public Builder withKeyBindStyle(Function<Style, Style> styleModifier) {
			keyBindStyle = styleModifier.apply(keyBindStyle);
			return this;
		}
		
		public Builder withUnboundKeyBindStyle(Function<Style, Style> styleModifier) {
			unboundKeyBindStyle = styleModifier.apply(unboundKeyBindStyle);
			return this;
		}
		
		public Builder withModifierStyle(Function<Style, Style> styleModifier) {
			modifierStyle = styleModifier.apply(modifierStyle);
			return this;
		}
		
		public Builder withHelpStyle(Function<Style, Style> styleModifier) {
			helpStyle = styleModifier.apply(helpStyle);
			return this;
		}
		
		public Builder withAction(DialogAction action) {
			this.action = action::handle;
			return this;
		}
		
		public Builder withCheckboxes(
		  ComplexDialogAction action, CheckboxButton... checkBoxes
		) {
			this.action = action;
			this.checkBoxes = checkBoxes;
			return this;
		}
		
		public Builder withConfirmText(Component confirmText) {
			this.confirmText = confirmText;
			return this;
		}
		
		public Builder withControlsScreen(@Nullable Function<Screen, Screen> controlsScreenSupplier) {
			this.controlsScreenSupplier = controlsScreenSupplier;
			return this;
		}
		
		public Builder category(String name, Consumer<CategoryBuilder> builder) {
			lines.add(new TranslatableComponent(prefix + "category." + name)
			            .withStyle(categoryStyle));
			builder.accept(new CategoryBuilder(name));
			lines.add(TextComponent.EMPTY);
			return this;
		}
		
		public Builder text(String key) {
			return text(new TranslatableComponent(prefix + key));
		}
		
		public Builder text(Component text) {
			lines.add(text);
			return this;
		}
		
		public ControlsHelpDialog build() {
			final ControlsHelpDialog d = new ControlsHelpDialog(title);
			d.setBody(lines);
			d.setConfirmText(confirmText);
			d.withCheckBoxes(action, checkBoxes);
			d.removeButton(d.cancelButton);
			d.setIcon(SimpleConfigIcons.Buttons.KEYBOARD);
			if (controlsScreenSupplier != null) {
				d.addButton(0, MultiFunctionIconButton.of(
				  SimpleConfigIcons.Buttons.KEYBOARD, -1, -1, ButtonAction.of(() -> {
					  Minecraft.getInstance().setScreen(controlsScreenSupplier.apply(d.getScreen()));
					  d.cancel();
				  }).title(() -> new TranslatableComponent("simpleconfig.ui.controls.edit_controls"))
					 .tint(0x80815C2E)
				));
			}
			return d;
		}
		
		public class CategoryBuilder {
			private final String name;
			
			public CategoryBuilder(String name) {
				this.name = name;
			}
			
			public CategoryBuilder key(String help, String keys) {
				final MutableComponent keyHelp =
				  new TranslatableComponent(prefix + name + "." + help).withStyle(helpStyle);
				lines.add(
				  parse(keys)
					 .append(new TextComponent(": ").withStyle(ChatFormatting.DARK_GRAY))
					 .append(keyHelp));
				return this;
			}
			
			public CategoryBuilder key(String help, KeyMapping key) {
				final MutableComponent keyHelp = new TranslatableComponent(prefix + name + "." + help).withStyle(helpStyle);
				final String keyName = key.isUnbound()? "---" : toTitleCase(key.getTranslatedKeyMessage().getString());
				lines.add(
				  new TextComponent(keyName).withStyle(key.isUnbound()? unboundKeyBindStyle : keyBindStyle)
					 .append(new TextComponent(": ").withStyle(ChatFormatting.DARK_GRAY))
					 .append(keyHelp));
				return this;
			}
			
			public CategoryBuilder text(String key) {
				return text(new TranslatableComponent(prefix + name + "." + key));
			}
			
			public CategoryBuilder text(Component text) {
				lines.add(text);
				return this;
			}
		}
	}
}
