package endorh.simpleconfig.clothconfig2.gui;

import net.minecraft.client.gui.IGuiEventListener;
import net.minecraft.client.gui.widget.Widget;

public class WidgetUtils {
	public static void forceUnFocus(Iterable<IGuiEventListener> listeners) {
		for (IGuiEventListener listener : listeners) forceUnFocus(listener);
	}
	
	public static void forceUnFocus(IGuiEventListener... listeners) {
		for (IGuiEventListener listener : listeners) forceUnFocus(listener);
	}
	
	// IGuiEventListener could use a setFocused() or unFocus() method
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
}
