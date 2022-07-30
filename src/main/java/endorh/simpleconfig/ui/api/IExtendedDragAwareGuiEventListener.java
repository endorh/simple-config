package endorh.simpleconfig.ui.api;

import net.minecraft.client.gui.IGuiEventListener;

/**
 * Receives drag events for buttons other than 0
 */
public interface IExtendedDragAwareGuiEventListener extends IGuiEventListener {
	default void endDrag(double mouseX, double mouseY, int button) {}
}
