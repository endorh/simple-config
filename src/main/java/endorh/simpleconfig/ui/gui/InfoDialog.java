package endorh.simpleconfig.ui.gui;

import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;

public class InfoDialog extends ConfirmDialog {
	public static InfoDialog create(
	  Component title, List<Component> body
	) {
		return create(title, body, null);
	}
	
	public static InfoDialog create(
	  Component title, List<Component> body,
	  @Nullable Consumer<InfoDialog> builder
	) {
		InfoDialog dialog = new InfoDialog(title, body);
		if (builder != null) builder.accept(dialog);
		return dialog;
	}
	
	protected InfoDialog(
	  Component title, List<Component> body
	) {
		super(title);
		setBody(body);
		removeButton(cancelButton);
		setConfirmText(CommonComponents.GUI_DONE);
	}
}
