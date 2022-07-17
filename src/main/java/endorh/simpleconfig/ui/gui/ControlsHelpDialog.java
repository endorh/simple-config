package endorh.simpleconfig.ui.gui;

import com.google.common.collect.Lists;
import endorh.simpleconfig.ui.api.ModifierKeyCode;
import endorh.simpleconfig.ui.gui.widget.CheckboxButton;
import endorh.simpleconfig.ui.gui.widget.MultiFunctionIconButton;
import endorh.simpleconfig.ui.gui.widget.MultiFunctionImageButton.ButtonAction;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.DialogTexts;
import net.minecraft.client.gui.screen.ControlsScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.text.*;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ControlsHelpDialog extends ConfirmDialog {
	protected ControlsHelpDialog(ITextComponent title) {
		super(title);
	}
	
	public static Builder of(String prefix) {
		return of(prefix, new TranslationTextComponent("simpleconfig.ui.controls"));
	}
	
	public static Builder of(String prefix, ITextComponent title) {
		return new Builder(prefix, title);
	}
	
	public static class Builder {
		private static final Pattern COMMA = Pattern.compile("\\s*,\\s*");
		private static final Pattern SLASH = Pattern.compile("\\s*/\\s*");
		private final ITextComponent title;
		private final String prefix;
		private final List<ITextComponent> lines = Lists.newArrayList();
		private Style categoryStyle = Style.EMPTY.setBold(true).func_244282_c(true);
		private Style keyStyle = Style.EMPTY.setFormatting(TextFormatting.DARK_AQUA);
		private Style keyBindStyle = Style.EMPTY.setFormatting(TextFormatting.DARK_GREEN);
		private Style unboundKeyBindStyle = Style.EMPTY.setFormatting(TextFormatting.DARK_RED);
		private Style modifierStyle = Style.EMPTY.setFormatting(TextFormatting.DARK_PURPLE);
		private Style helpStyle = Style.EMPTY.setFormatting(TextFormatting.GRAY);
		private CheckboxButton[] checkBoxes = new CheckboxButton[0];
		private ComplexDialogAction action = (v, s) -> {};
		private ITextComponent confirmText = DialogTexts.GUI_DONE;
		private @Nullable Function<Screen, Screen> controlsScreenSupplier = s -> new ControlsScreen(s, Minecraft.getInstance().gameSettings);
		
		private Builder(String prefix, ITextComponent title) {
			this.prefix = prefix.endsWith(".")? prefix : prefix + ".";
			this.title = title;
		}
		
		private IFormattableTextComponent parseSingle(String key) {
			if (key.endsWith("!"))
				return ModifierKeyCode.parse(key.substring(0, key.length() - 1))
				  .getLayoutAgnosticLocalizedName(modifierStyle, keyStyle).deepCopy();
			return ModifierKeyCode.parse(key).getLocalizedName(modifierStyle, keyStyle).deepCopy();
		}
		
		private IFormattableTextComponent parse(String keys) {
			final IFormattableTextComponent comma = new StringTextComponent(", ").mergeStyle(keyStyle);
			final IFormattableTextComponent slash = new StringTextComponent("/").mergeStyle(keyStyle);
			return Arrays.stream(COMMA.split(keys)).map(
			  s -> Arrays.stream(SLASH.split(s))
			    .map(this::parseSingle)
				 .reduce((a, b) -> a.append(slash).append(b)).orElse(StringTextComponent.EMPTY.deepCopy())
			).reduce((a, b) -> a.append(comma).append(b)).orElse(StringTextComponent.EMPTY.deepCopy());
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
		
		public Builder withConfirmText(ITextComponent confirmText) {
			this.confirmText = confirmText;
			return this;
		}
		
		public Builder withControlsScreen(@Nullable Function<Screen, Screen> controlsScreenSupplier) {
			this.controlsScreenSupplier = controlsScreenSupplier;
			return this;
		}
		
		public Builder category(String name, Consumer<CategoryBuilder> builder) {
			lines.add(new TranslationTextComponent(prefix + "category." + name)
			            .mergeStyle(categoryStyle));
			builder.accept(new CategoryBuilder(name));
			lines.add(StringTextComponent.EMPTY);
			return this;
		}
		
		public Builder text(String key) {
			return text(new TranslationTextComponent(prefix + key));
		}
		
		public Builder text(ITextComponent text) {
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
					  Minecraft.getInstance().displayGuiScreen(controlsScreenSupplier.apply(d.getScreen()));
					  d.cancel();
				  }).title(() -> new TranslationTextComponent("simpleconfig.ui.controls.edit_controls"))
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
				final IFormattableTextComponent keyHelp =
				  new TranslationTextComponent(prefix + name + "." + help).mergeStyle(helpStyle);
				lines.add(
				  parse(keys)
					 .append(new StringTextComponent(": ").mergeStyle(TextFormatting.DARK_GRAY))
					 .append(keyHelp));
				return this;
			}
			
			public CategoryBuilder key(String help, KeyBinding key) {
				final IFormattableTextComponent keyHelp = new TranslationTextComponent(prefix + name + "." + help).mergeStyle(helpStyle);
				final String keyName = key.isInvalid()? "---" : toTitleCase(key.func_238171_j_().getString());
				lines.add(
				  new StringTextComponent(keyName).mergeStyle(key.isInvalid()? unboundKeyBindStyle : keyBindStyle)
					 .append(new StringTextComponent(": ").mergeStyle(TextFormatting.DARK_GRAY))
					 .append(keyHelp));
				return this;
			}
			
			public CategoryBuilder text(String key) {
				return text(new TranslationTextComponent(prefix + name + "." + key));
			}
			
			public CategoryBuilder text(ITextComponent text) {
				lines.add(text);
				return this;
			}
		}
	}
}
