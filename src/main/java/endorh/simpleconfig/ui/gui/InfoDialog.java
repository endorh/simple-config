package endorh.simpleconfig.ui.gui;

import net.minecraft.client.gui.DialogTexts;
import net.minecraft.util.text.ITextComponent;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;

public class InfoDialog extends ConfirmDialog {
	public static InfoDialog create(
	  ITextComponent title, List<ITextComponent> body
	) {
		return create(title, body, null);
	}
	
	public static InfoDialog create(
	  ITextComponent title, List<ITextComponent> body,
	  @Nullable Consumer<InfoDialog> builder
	) {
		InfoDialog dialog = new InfoDialog(title, body);
		if (builder != null) builder.accept(dialog);
		return dialog;
	}
	
	protected InfoDialog(
	  ITextComponent title, List<ITextComponent> body
	) {
		super(title);
		setBody(body);
		removeButton(cancelButton);
		setConfirmText(DialogTexts.GUI_DONE);
	}
}
