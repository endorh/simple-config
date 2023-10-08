package endorh.simpleconfig.ui.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Renderable;

import java.util.Collection;

public class WidgetUtils {
	public static void renderAll(
      GuiGraphics gg, int mouseX, int mouseY, float partialTicks,
      Collection<AbstractWidget> widgets
	) {
		for (AbstractWidget widget : widgets) widget.render(gg, mouseX, mouseY, partialTicks);
	}

	public static void renderAll(
      GuiGraphics gg, int mouseX, int mouseY, float partialTicks,
      Renderable... widgets
	) {
		for (Renderable widget : widgets) widget.render(gg, mouseX, mouseY, partialTicks);
	}
}
