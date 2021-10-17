package endorh.simple_config.clothconfig2.gui;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.gui.INestedGuiEventHandler;
import net.minecraft.client.gui.screen.Screen;

import java.util.List;

public interface IDialogCapableScreen {
	List<AbstractDialog> getDialogs();
	
	default boolean hasDialogs() {
		return !getDialogs().isEmpty();
	}
	
	default void addDialog(AbstractDialog dialog) {
		getDialogs().add(dialog);
	}
	
	default void renderDialogs(MatrixStack mStack, int mouseX, int mouseY, float delta) {
		final List<AbstractDialog> dialogs = getDialogs();
		final List<AbstractDialog> removed = Lists.newLinkedList();
		mStack.push(); {
			mStack.translate(0D, 0D, 200D);
			for (AbstractDialog dialog : dialogs) {
				if (!dialog.render(mStack, mouseX, mouseY, delta))
					removed.add(dialog);
			}
		} mStack.pop();
		dialogs.removeAll(removed);
		if (!dialogs.isEmpty() && this instanceof INestedGuiEventHandler)
			((INestedGuiEventHandler) this).setListener(dialogs.get(dialogs.size() - 1));
	}
	
	default boolean handleDialogsEscapeKey() {
		final List<AbstractDialog> dialogs = getDialogs();
		for (AbstractDialog dialog : Lists.reverse(dialogs)) {
			if (dialog.escapeKeyPressed())
				return true;
		}
		return !dialogs.isEmpty();
	}
	
	default boolean handleDialogsMouseClicked(double mouseX, double mouseY, int button) {
		final List<AbstractDialog> dialogs = getDialogs();
		for (AbstractDialog dialog : Lists.reverse(dialogs)) {
			if (dialog.mouseClicked(mouseX, mouseY, button))
				return true;
		}
		return !dialogs.isEmpty();
	}
	
	default boolean handleDialogsKeyPressed(int keyCode, int scanCode, int modifiers) {
		if (keyCode == 256) return handleDialogsEscapeKey(); // Esc
		if (keyCode == 258) return handleDialogsChangeFocus(!Screen.hasShiftDown());
		final List<AbstractDialog> dialogs = getDialogs();
		for (AbstractDialog dialog : Lists.reverse(dialogs)) {
			if (dialog.keyPressed(keyCode, scanCode, modifiers))
				return true;
		}
		return !dialogs.isEmpty();
	}
	
	default boolean handleDialogsCharTyped(char codePoint, int modifiers) {
		final List<AbstractDialog> dialogs = getDialogs();
		for (AbstractDialog dialog : Lists.reverse(dialogs)) {
			if (dialog.charTyped(codePoint, modifiers))
				return true;
		}
		return !dialogs.isEmpty();
	}
	
	default boolean handleDialogsChangeFocus(boolean forward) {
		final List<AbstractDialog> dialogs = getDialogs();
		if (!dialogs.isEmpty()) {
			final AbstractDialog dialog = dialogs.get(dialogs.size() - 1);
			if (!dialog.changeFocus(forward)) dialog.changeFocus(forward);
			return true;
		}
		return false;
	}
}
