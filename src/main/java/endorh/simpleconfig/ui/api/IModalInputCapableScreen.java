package endorh.simpleconfig.ui.api;

import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Nullable;

/**
 * Feature interface for {@link Screen}s that support {@link IModalInputProcessor}s.
 */
public interface IModalInputCapableScreen {
	@Nullable IModalInputProcessor getModalInputProcessor();
	/**
	 * Should not be called directly, instead use
	 * {@link #claimModalInput(IModalInputProcessor)}.
	 */
	@Internal void setModalInputProcessor(IModalInputProcessor processor);
	
	/**
	 * Claim modal input for this screen.<br>
	 * There can only be a single modal input processor at a time.
	 */
	default void claimModalInput(IModalInputProcessor processor) {
		IModalInputProcessor prev = getModalInputProcessor();
		setModalInputProcessor(processor);
		if (prev != null) prev.cancelModalInputProcessing();
	}
	
	/**
	 * Cancel the current modal input processor.
	 */
	default void cancelModalInput() {
		IModalInputProcessor prev = getModalInputProcessor();
		setModalInputProcessor(null);
		if (prev != null) prev.cancelModalInputProcessing();
	}
	
	/**
	 * Call this method from the top of {@link Screen#keyPressed(int, int, int)},
	 * forwarding {@code true} return values.
	 */
	default boolean handleModalKeyPressed(int keyCode, int scanCode, int modifiers) {
		IModalInputProcessor proc = getModalInputProcessor();
		if (proc != null) {
			if (!proc.modalKeyPressed(keyCode, scanCode, modifiers))
				cancelModalInput();
			return true;
		}
		return false;
	}
	
	/**
	 * Call this method from the top of {@link Screen#keyReleased(int, int, int)},
	 * forwarding {@code true} return values.
	 */
	default boolean handleModalKeyReleased(int keyCode, int scanCode, int modifiers) {
		IModalInputProcessor proc = getModalInputProcessor();
		if (proc != null) {
			if (!proc.modalKeyReleased(keyCode, scanCode, modifiers))
				cancelModalInput();
			return true;
		}
		return false;
	}
	
	/**
	 * Call this method from the top of {@link Screen#charTyped(char, int)},
	 * forwarding {@code true} return values.
	 */
	default boolean handleModalCharTyped(char codePoint, int modifiers) {
		IModalInputProcessor proc = getModalInputProcessor();
		if (proc != null) {
			if (!proc.modalCharTyped(codePoint, modifiers))
				cancelModalInput();
			return true;
		}
		return false;
	}
	
	/**
	 * Call this method from the top of {@link Screen#mouseClicked(double, double, int)},
	 * forwarding {@code true} return values.
	 */
	default boolean handleModalMouseClicked(double mouseX, double mouseY, int button) {
		IModalInputProcessor proc = getModalInputProcessor();
		if (proc != null) {
			if (!proc.shouldConsumeModalClicks(mouseX, mouseY, button)) {
				cancelModalInput();
				return false;
			}
			if (!proc.modalMouseClicked(mouseX, mouseY, button))
				cancelModalInput();
			return true;
		}
		return false;
	}
	
	/**
	 * Call this method from the top of {@link Screen#mouseReleased(double, double, int)},
	 * forwarding {@code true} return values.
	 */
	default boolean handleModalMouseReleased(double mouseX, double mouseY, int button) {
		IModalInputProcessor proc = getModalInputProcessor();
		if (proc != null) {
			if (!proc.modalMouseReleased(mouseX, mouseY, button))
				cancelModalInput();
			return true;
		}
		return false;
	}
	
	/**
	 * Call this method from the top of {@link Screen#mouseScrolled(double, double, double)},
	 * forwarding {@code true} return values.
	 */
	default boolean handleModalMouseScrolled(double mouseX, double mouseY, double scrollDelta) {
		IModalInputProcessor proc = getModalInputProcessor();
		if (proc != null) {
			if (!proc.modalMouseScrolled(mouseX, mouseY, scrollDelta))
				cancelModalInput();
			return true;
		}
		return false;
	}
}
