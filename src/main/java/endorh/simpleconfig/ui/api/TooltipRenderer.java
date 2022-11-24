package endorh.simpleconfig.ui.api;

import net.minecraft.util.text.ITextComponent;

import java.util.List;

public interface TooltipRenderer {
	void renderTooltip(List<ITextComponent> tooltip, int mouseX, int mouseY);
}
