package endorh.simpleconfig.ui.api;

import net.minecraft.network.chat.Component;

import java.util.List;

public interface TooltipRenderer {
	void renderTooltip(List<Component> tooltip, int mouseX, int mouseY);
}
