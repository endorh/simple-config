package endorh.simpleconfig.ui.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import endorh.simpleconfig.api.ui.math.Rectangle;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Widget;
import net.minecraft.client.gui.components.events.GuiEventListener;

import java.util.Collection;

public class WidgetUtils {
	public static void forceUnFocus(Iterable<GuiEventListener> listeners) {
		for (GuiEventListener listener : listeners) forceUnFocus(listener);
	}
	
	public static void forceUnFocus(GuiEventListener... listeners) {
		for (GuiEventListener listener : listeners) forceUnFocus(listener);
	}
	
	// IGuiEventListener could use a public setFocused() or unFocus() method
	public static void forceUnFocus(GuiEventListener listener) {
		if (listener instanceof AbstractWidget && !((AbstractWidget) listener).isFocused()) return;
		for (int i = 0; i < 1000; i++) // Hanging here would be awkward
			if (!listener.changeFocus(true)) break;
	}
	
	public static void forceFocus(GuiEventListener listener) {
		if (listener instanceof AbstractWidget && ((AbstractWidget) listener).isFocused()) return;
		forceUnFocus(listener);
		listener.changeFocus(true);
	}
	
	public static void forceSetFocus(GuiEventListener listener, boolean focus) {
		if (listener instanceof AbstractWidget && ((AbstractWidget) listener).isFocused() == focus) return;
		if (focus) forceFocus(listener); else forceUnFocus(listener);
	}
	
	public static void forceTransferFocus(GuiEventListener from, GuiEventListener to) {
		forceUnFocus(from);
		forceFocus(to);
	}
	
	public static void pos(AbstractWidget widget, int x, int y) {
		widget.x = x;
		widget.y = y;
	}
	
	public static void pos(AbstractWidget widget, int x, int y, int w) {
		widget.x = x;
		widget.y = y;
		widget.setWidth(w);
	}
	
	public static void pos(AbstractWidget widget, int x, int y, int w, int h) {
		widget.x = x;
		widget.y = y;
		widget.setWidth(w);
		widget.setHeight(h);
	}
	
	public static void pos(AbstractWidget widget, Rectangle area) {
		pos(widget, area.x, area.y, area.width, area.height);
	}
	
	public static void renderAll(
	  PoseStack mStack, int mouseX, int mouseY, float partialTicks,
	  Collection<AbstractWidget> widgets
	) {
		for (AbstractWidget widget : widgets) widget.render(mStack, mouseX, mouseY, partialTicks);
	}
	
	public static void renderAll(
	  PoseStack mStack, int mouseX, int mouseY, float partialTicks,
	  Widget... widgets
	) {
		for (Widget widget : widgets) widget.render(mStack, mouseX, mouseY, partialTicks);
	}
}
