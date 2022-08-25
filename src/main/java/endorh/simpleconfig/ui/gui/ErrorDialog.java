package endorh.simpleconfig.ui.gui;

import endorh.simpleconfig.api.ui.icon.SimpleConfigIcons;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;

public class ErrorDialog extends ProgressDialog {
	public static ErrorDialog create(
	  ITextComponent title, Throwable error
	) {
		return create(title, error, null);
	}
	
	public static ErrorDialog create(
	  ITextComponent title, Throwable error, @Nullable Consumer<ErrorDialog> builder
	) {
		ErrorDialog dialog = new ErrorDialog(title, error);
		if (builder != null) builder.accept(dialog);
		return dialog;
	}
	
	public static ErrorDialog create(
	  ITextComponent title, List<ITextComponent> error
	) {
		return create(title, error, null);
	}
	
	public static ErrorDialog create(
	  ITextComponent title, List<ITextComponent> error,
	  @Nullable Consumer<ErrorDialog> builder
	) {
		ErrorDialog dialog = new ErrorDialog(title, error);
		if (builder != null) builder.accept(dialog);
		return dialog;
	}
	
	public ErrorDialog(
	  ITextComponent title, Throwable error
	) {
		super(title, null);
		setError(error);
		removeButton(cancelButton);
		setConfirmText(new TranslationTextComponent("gui.ok"));
		setIcon(SimpleConfigIcons.Status.ERROR);
	}
	
	public ErrorDialog(
	  ITextComponent title, List<ITextComponent> error
	) {
		super(title, null);
		setError(error);
		removeButton(cancelButton);
		setConfirmText(new TranslationTextComponent("gui.ok"));
		setIcon(SimpleConfigIcons.Status.ERROR);
	}
}
