package endorh.simpleconfig.clothconfig2.gui;

import net.minecraft.client.gui.DialogTexts;
import net.minecraft.util.text.ITextComponent;

import java.util.List;

public class InfoDialog extends ConfirmDialog {
	public InfoDialog(
	  IOverlayCapableScreen screen, ITextComponent title, List<ITextComponent> body
	) {
		this(screen, title, body, DialogTexts.GUI_DONE);
	}
	
	public InfoDialog(
	  IOverlayCapableScreen screen, ITextComponent title,
	  List<ITextComponent> body, ITextComponent buttonText
	) {
		super(screen, title, b -> {}, body, DialogTexts.GUI_CANCEL, buttonText);
		removeButton(cancelButton);
	}
}
