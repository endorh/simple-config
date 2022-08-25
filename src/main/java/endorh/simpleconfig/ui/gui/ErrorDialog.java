package endorh.simpleconfig.ui.gui;

import endorh.simpleconfig.api.ui.icon.SimpleConfigIcons;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;

public class ErrorDialog extends ProgressDialog {
	public static ErrorDialog create(
	  Component title, Throwable error
	) {
		return create(title, error, null);
	}
	
	public static ErrorDialog create(
	  Component title, Throwable error, @Nullable Consumer<ErrorDialog> builder
	) {
		ErrorDialog dialog = new ErrorDialog(title, error);
		if (builder != null) builder.accept(dialog);
		return dialog;
	}
	
	public static ErrorDialog create(
	  Component title, List<Component> error
	) {
		return create(title, error, null);
	}
	
	public static ErrorDialog create(
	  Component title, List<Component> error,
	  @Nullable Consumer<ErrorDialog> builder
	) {
		ErrorDialog dialog = new ErrorDialog(title, error);
		if (builder != null) builder.accept(dialog);
		return dialog;
	}
	
	public ErrorDialog(
	  Component title, Throwable error
	) {
		super(title, null);
		setError(error);
		removeButton(cancelButton);
		setConfirmText(Component.translatable("gui.ok"));
		setIcon(SimpleConfigIcons.Status.ERROR);
	}
	
	public ErrorDialog(
	  Component title, List<Component> error
	) {
		super(title, null);
		setError(error);
		removeButton(cancelButton);
		setConfirmText(Component.translatable("gui.ok"));
		setIcon(SimpleConfigIcons.Status.ERROR);
	}
}
