package endorh.simpleconfig.ui.api;

import net.minecraft.client.gui.components.events.GuiEventListener;

/**
 * Receives drag events for buttons other than 0
 */
public interface GuiEventListenerEx extends GuiEventListener {
	default void endDrag(double mouseX, double mouseY, int button) {}
}
