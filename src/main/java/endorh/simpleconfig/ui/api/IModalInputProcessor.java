package endorh.simpleconfig.ui.api;

import endorh.simpleconfig.ui.gui.widget.KeyBindButton;
import net.minecraft.client.gui.FocusableGui;

/**
 * Feature interface intended for widgets that can capture modal input from an
 * {@link IModalInputCapableScreen}.<br>
 * Convenient for {@link KeyBindButton}s.
 */
public interface IModalInputProcessor {
	/**
	 * Handle a modal key event.
	 * @return true if modal input processing should continue, false if modal input is released.
	 * @see FocusableGui#keyPressed(int, int, int)
	 */
	default boolean modalKeyPressed(int keyCode, int scanCode, int modifiers) {
		return true;
	}
	
	/**
	 * Handle a modal key release event.
	 * @return true if modal input processing should continue, false if modal input is released.
	 * @see FocusableGui#keyReleased(int, int, int)
	 */
	default boolean modalKeyReleased(int keyCode, int scanCode, int modifiers) {
		return true;
	}
	
	/**
	 * Handle a modal character event.
	 * @return true if modal input processing should continue, false if modal input is released.
	 * @see FocusableGui#charTyped(char, int)
	 */
	default boolean modalCharTyped(char codePoint, int modifiers) {
		return true;
	}
	
	/**
	 * Handle a modal mouse event.
	 * @return true if modal input processing should continue, false if modal input is released.
	 * @see FocusableGui#mouseClicked(double, double, int)
	 */
	default boolean modalMouseClicked(double mouseX, double mouseY, int button) {
		return true;
	}
	
	/**
	 * Handle a modal mouse release event.
	 * @return true if modal input processing should continue, false if modal input is released.
	 * @see FocusableGui#mouseReleased(double, double, int)
	 */
	default boolean modalMouseReleased(double mouseX, double mouseY, int button) {
		return true;
	}
	
	/**
	 * Handle a modal mouse scroll event.
	 * @return true if modal input processing should continue, false if modal input is released.
	 * @see FocusableGui#mouseScrolled(double, double, double)
	 */
	default boolean modalMouseScrolled(double mouseX, double mouseY, double amount) {
		return true;
	}
	
	/**
	 * Return true to consume clicks.<br>
	 * Not consuming clicks implies that the modal input will be released on clicks.
	 */
	default boolean shouldConsumeModalClicks(double mouseX, double mouseY, int button) {
		return false;
	}
	
	/**
	 * Called when modal input is being forcefully withdrawn from the processor
	 * (such as taken by another processor).<br>
	 * You should not call {@link IModalInputCapableScreen#claimModalInput(IModalInputProcessor)}
	 * from this method, or you'll risk entering an endless loop if the other processor does too.
	 */
	default void cancelModalInputProcessing() {}
}
