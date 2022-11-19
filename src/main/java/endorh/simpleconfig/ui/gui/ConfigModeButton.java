package endorh.simpleconfig.ui.gui;

import com.google.common.collect.Lists;
import endorh.simpleconfig.api.SimpleConfig.EditType;
import endorh.simpleconfig.api.ui.icon.SimpleConfigIcons;
import endorh.simpleconfig.ui.gui.widget.MultiFunctionIconButton;
import endorh.simpleconfig.ui.gui.widget.MultiFunctionImageButton.ButtonAction;
import net.minecraft.network.chat.TranslatableComponent;

import java.util.Collections;

public class ConfigModeButton extends MultiFunctionIconButton {
	private final SimpleConfigScreen screen;
	private final EditType type;
	
	public ConfigModeButton(SimpleConfigScreen screen, EditType type) {
		super(0, 0, 20, 80, SimpleConfigIcons.Types.iconFor(type), ButtonAction.of(
			 () -> screen.showType(type)
		  ).title(() -> new TranslatableComponent(
			 "simpleconfig.config.category." + type.getAlias().replace('-', '.'))
		  ).tooltip(
			 () -> screen.hasType(type) && screen.getEditedType() != type
			       ? Lists.newArrayList(new TranslatableComponent(
			           "simpleconfig.ui.switch." + type.getAlias().replace('-', '.')))
			       : Collections.emptyList()
		  ).active(() -> screen.hasType(type)));
		this.screen = screen;
		this.type = type;
	}
	
	@Override public boolean isHoveredOrFocused() {
		return super.isHoveredOrFocused() || isSelected();
	}
	
	public boolean isSelected() {
		return screen.getEditedType() == type;
	}
}
