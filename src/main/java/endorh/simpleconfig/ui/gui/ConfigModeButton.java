package endorh.simpleconfig.ui.gui;

import com.google.common.collect.Lists;
import endorh.simpleconfig.api.SimpleConfig.EditType;
import endorh.simpleconfig.api.ui.icon.SimpleConfigIcons;
import endorh.simpleconfig.ui.gui.widget.MultiFunctionIconButton;
import endorh.simpleconfig.ui.gui.widget.MultiFunctionImageButton.ButtonAction;
import net.minecraft.util.text.TranslationTextComponent;

import java.util.Collections;

public class ConfigModeButton extends MultiFunctionIconButton {
	private final SimpleConfigScreen screen;
	private final EditType type;
	
	public ConfigModeButton(SimpleConfigScreen screen, EditType type) {
		super(0, 0, 20, 80, SimpleConfigIcons.Types.iconFor(type), ButtonAction.of(
			 () -> screen.showType(type)
		  ).title(() -> new TranslationTextComponent(
			 "simpleconfig.config.category." + type.getAlias().replace('-', '.'))
		  ).tooltip(
			 () -> screen.hasType(type) && screen.getEditedType() != type
			       ? Lists.newArrayList(new TranslationTextComponent(
			           "simpleconfig.ui.switch." + type.getAlias().replace('-', '.')))
			       : Collections.emptyList()
		  ).active(() -> screen.hasType(type)));
		this.screen = screen;
		this.type = type;
	}
	
	@Override public boolean isHovered() {
		return super.isHovered() || isSelected();
	}
	
	public boolean isSelected() {
		return screen.getEditedType() == type;
	}
}
