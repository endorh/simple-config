package endorh.simpleconfig.ui.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

/**
 * Screen that wraps a dialog as a screen on its own.
 */
public class DialogScreen extends AbstractDialogScreen {
	private final Screen parent;
	private final AbstractDialog dialog;
	
	public DialogScreen(Screen parent, AbstractDialog dialog) {
		super(dialog.getTitle());
		this.parent = parent;
		this.dialog = dialog;
		addDialog(dialog);
	}
	
	@Override public void screenTick() {
		if (dialog.isCancelled()) Minecraft.getInstance().setScreen(parent);
	}
}
