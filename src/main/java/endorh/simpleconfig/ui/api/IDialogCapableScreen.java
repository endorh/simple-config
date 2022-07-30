package endorh.simpleconfig.ui.api;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.matrix.MatrixStack;
import endorh.simpleconfig.ui.gui.AbstractDialog;
import net.minecraft.client.gui.INestedGuiEventHandler;
import net.minecraft.client.gui.screen.Screen;

import java.util.List;

/**
 * Feature interface for screens that can display an {@link AbstractDialog}.<br>
 * For convenience, extends the {@link IOverlayCapableContainer} and {@link IModalInputCapableScreen},
 * which some dialogs may benefit from.
 * <br><br>
 * Implementations should add hooks in {@link Screen#keyPressed(int, int, int)},
 * {@link Screen#keyReleased(int, int, int)}, {@link Screen#charTyped(char, int)}
 * {@link Screen#mouseClicked(double, double, int)},
 * {@link Screen#mouseReleased(double, double, int)},
 * {@link Screen#mouseScrolled(double, double, double)} and
 * {@link Screen#mouseDragged(double, double, int, double, double)}, calling the respective
 * {@link #handleDialogsKeyPressed(int, int, int)} methods.
 * <br><br>
 * The {@link #handleDialogsEscapeKey()} and {@link #handleDialogsChangeFocus(boolean)} do not
 * need to be called manually, as they're called by {@link #handleDialogsKeyPressed(int, int, int)}.
 * <br><br>
 * Implementations should also add the hooks required by {@link IOverlayCapableContainer},
 * {@link IModalInputCapableScreen} and {@link IMultiTooltipScreen}.
 */
public interface IDialogCapableScreen extends IOverlayCapableContainer, IModalInputCapableScreen, IMultiTooltipScreen {
	SortedDialogCollection getDialogs();
	
	default boolean hasDialogs() {
		return !getDialogs().isEmpty();
	}
	
	default void addDialog(AbstractDialog dialog) {
		dialog.setScreen(this);
		getDialogs().add(dialog);
	}
	
	default void removeDialog(AbstractDialog dialog) {
		getDialogs().remove(dialog);
	}
	
	default void renderDialogs(MatrixStack mStack, int mouseX, int mouseY, float delta) {
		SortedDialogCollection dialogs = getDialogs();
		List<AbstractDialog> current = dialogs.getDialogs();
		dialogs.update();
		mStack.push(); {
			mStack.translate(0D, 0D, 200D);
			for (AbstractDialog dialog : current)
				if (!dialog.render(mStack, mouseX, mouseY, delta)) dialogs.remove(dialog);
		} mStack.pop();
		dialogs.update();
		current = dialogs.getDialogs();
		if (!current.isEmpty() && this instanceof INestedGuiEventHandler)
			((INestedGuiEventHandler) this).setListener(current.get(current.size() - 1));
	}
	
	default void tickDialogs() {
		for (AbstractDialog dialog : getDialogs().getDialogs()) dialog.tick();
	}
	
	default boolean handleDialogsMouseClicked(double mouseX, double mouseY, int button) {
		SortedDialogCollection dialogs = getDialogs();
		for (AbstractDialog dialog : dialogs.getDescendingDialogs())
			if (dialog.mouseClicked(mouseX, mouseY, button)) return true;
		return !dialogs.isEmpty();
	}
	
	default boolean handleDialogsMouseScrolled(double mouseX, double mouseY, double delta) {
		SortedDialogCollection dialogs = getDialogs();
		for (AbstractDialog dialog : dialogs.getDescendingDialogs())
			if (dialog.mouseScrolled(mouseX, mouseY, delta)) return true;
		return !dialogs.isEmpty();
	}
	
	default boolean handleDialogsMouseDragged(
	  double mouseX, double mouseY, int button, double dragX, double dragY
	) {
		SortedDialogCollection dialogs = getDialogs();
		for (AbstractDialog dialog : dialogs.getDescendingDialogs())
			if (dialog.mouseDragged(mouseX, mouseY, button, dragX, dragY)) return true;
		return !dialogs.isEmpty();
	}
	
	default boolean handleDialogsMouseReleased(
	  double mouseX, double mouseY, int button
	) {
		SortedDialogCollection dialogs = getDialogs();
		for (AbstractDialog dialog : dialogs.getDescendingDialogs())
			if (dialog.mouseReleased(mouseX, mouseY, button)) return true;
		return !dialogs.isEmpty();
	}
	
	default boolean handleDialogsKeyPressed(int keyCode, int scanCode, int modifiers) {
		if (keyCode == 256) return handleDialogsEscapeKey(); // Esc
		if (keyCode == 258) return handleDialogsChangeFocus(!Screen.hasShiftDown());
		final SortedDialogCollection dialogs = getDialogs();
		for (AbstractDialog dialog : dialogs.getDescendingDialogs())
			if (dialog.keyPressed(keyCode, scanCode, modifiers)) return true;
		return !dialogs.isEmpty();
	}
	
	default boolean handleDialogsCharTyped(char codePoint, int modifiers) {
		final SortedDialogCollection dialogs = getDialogs();
		for (AbstractDialog dialog : dialogs.getDescendingDialogs())
			if (dialog.charTyped(codePoint, modifiers)) return true;
		return !dialogs.isEmpty();
	}
	
	default boolean handleDialogsKeyReleased(int keyCode, int scanCode, int modifiers) {
		final SortedDialogCollection dialogs = getDialogs();
		for (AbstractDialog dialog : dialogs.getDescendingDialogs())
			if (dialog.keyReleased(keyCode, scanCode, modifiers)) return true;
		return !dialogs.isEmpty();
	}
	
	default boolean handleDialogsEscapeKey() {
		final SortedDialogCollection dialogs = getDialogs();
		for (AbstractDialog dialog : dialogs.getDescendingDialogs())
			if (dialog.escapeKeyPressed()) return true;
		return !dialogs.isEmpty();
	}
	
	default boolean handleDialogsChangeFocus(boolean forward) {
		final SortedDialogCollection dialogs = getDialogs();
		List<AbstractDialog> current = dialogs.getDescendingDialogs();
		if (!current.isEmpty()) {
			AbstractDialog first = current.get(0);
			if (!first.changeFocus(forward)) first.changeFocus(forward);
			return true;
		}
		return false;
	}
	
	class SortedDialogCollection {
		private final List<AbstractDialog> dialogs = Lists.newArrayList();
		private final List<AbstractDialog> reversed = Lists.reverse(dialogs);
		private final List<AbstractDialog> added = Lists.newArrayList();
		private final List<AbstractDialog> removed = Lists.newArrayList();
		
		public List<AbstractDialog> getDialogs() {
			return dialogs;
		}
		
		public List<AbstractDialog> getDescendingDialogs() {
			return reversed;
		}
		
		public List<AbstractDialog> getAdded() {
			return added;
		}
		
		public List<AbstractDialog> getRemoved() {
			return removed;
		}
		
		public void update() {
			List<AbstractDialog> dialogs = getDialogs();
			List<AbstractDialog> added = getAdded();
			List<AbstractDialog> removed = getRemoved();
			dialogs.addAll(added);
			dialogs.removeAll(removed);
			added.clear();
			removed.clear();
		}
		
		public boolean isEmpty() {
			return dialogs.isEmpty();
		}
		
		public void add(AbstractDialog dialog) {
			added.add(dialog);
		}
		
		public void remove(AbstractDialog dialog) {
			removed.add(dialog);
		}
	}
}
