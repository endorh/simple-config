package endorh.simpleconfig.ui.gui;

import com.mojang.blaze3d.matrix.MatrixStack;
import endorh.simpleconfig.ui.math.Rectangle;
import net.minecraft.client.gui.IGuiEventListener;
import net.minecraft.client.gui.IRenderable;
import net.minecraft.client.gui.widget.Widget;

import java.util.Collection;

public class WidgetUtils {
	public static void forceUnFocus(Iterable<IGuiEventListener> listeners) {
		for (IGuiEventListener listener : listeners) forceUnFocus(listener);
	}
	
	public static void forceUnFocus(IGuiEventListener... listeners) {
		for (IGuiEventListener listener : listeners) forceUnFocus(listener);
	}
	
	// IGuiEventListener could use a public setFocused() or unFocus() method
	public static void forceUnFocus(IGuiEventListener listener) {
		if (listener instanceof Widget && !((Widget) listener).isFocused()) return;
		for (int i = 0; i < 1000; i++) // Hanging here would be awkward
			if (!listener.changeFocus(true)) break;
	}
	
	public static void forceFocus(IGuiEventListener listener) {
		if (listener instanceof Widget && ((Widget) listener).isFocused()) return;
		forceUnFocus(listener);
		listener.changeFocus(true);
	}
	
	public static void forceSetFocus(IGuiEventListener listener, boolean focus) {
		if (listener instanceof Widget && ((Widget) listener).isFocused() == focus) return;
		if (focus) forceFocus(listener); else forceUnFocus(listener);
	}
	
	public static void forceTransferFocus(IGuiEventListener from, IGuiEventListener to) {
		forceUnFocus(from);
		forceFocus(to);
	}
	
	public static void pos(Widget widget, int x, int y) {
		widget.x = x;
		widget.y = y;
	}
	
	public static void pos(Widget widget, int x, int y, int w) {
		widget.x = x;
		widget.y = y;
		widget.setWidth(w);
	}
	
	public static void pos(Widget widget, int x, int y, int w, int h) {
		widget.x = x;
		widget.y = y;
		widget.setWidth(w);
		widget.setHeight(h);
	}
	
	public static void pos(Widget widget, Rectangle area) {
		pos(widget, area.x, area.y, area.width, area.height);
	}
	
	public static void renderAll(
	  MatrixStack mStack, int mouseX, int mouseY, float partialTicks,
	  Collection<Widget> widgets
	) {
		for (Widget widget : widgets) widget.render(mStack, mouseX, mouseY, partialTicks);
	}
	
	public static void renderAll(
	  MatrixStack mStack, int mouseX, int mouseY, float partialTicks,
	  IRenderable... widgets
	) {
		for (IRenderable widget : widgets) widget.render(mStack, mouseX, mouseY, partialTicks);
	}
}
