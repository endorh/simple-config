package endorh.simpleconfig.ui.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Renderable;

import java.util.Collection;

public class WidgetUtils {
	public static void renderAll(
	  PoseStack mStack, int mouseX, int mouseY, float partialTicks,
	  Collection<AbstractWidget> widgets
	) {
		for (AbstractWidget widget : widgets) widget.render(mStack, mouseX, mouseY, partialTicks);
	}

	public static void renderAll(
	  PoseStack mStack, int mouseX, int mouseY, float partialTicks,
	  Renderable... widgets
	) {
		for (Renderable widget : widgets) widget.render(mStack, mouseX, mouseY, partialTicks);
	}
}
