package endorh.simpleconfig.clothconfig2.gui;

import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;

public class ErrorDialog extends ProgressDialog {
	public static ErrorDialog create(
	  IOverlayCapableScreen screen, ITextComponent title, Throwable error
	) {
		return create(screen, title, error, null);
	}
	
	public static ErrorDialog create(
	  IOverlayCapableScreen screen, ITextComponent title, Throwable error, @Nullable Consumer<ErrorDialog> builder
	) {
		ErrorDialog dialog = new ErrorDialog(screen, title, error);
		if (builder != null) builder.accept(dialog);
		return dialog;
	}
	
	public static ErrorDialog create(
	  IOverlayCapableScreen screen, ITextComponent title, List<ITextComponent> error
	) {
		return create(screen, title, error, null);
	}
	
	public static ErrorDialog create(
	  IOverlayCapableScreen screen, ITextComponent title, List<ITextComponent> error,
	  @Nullable Consumer<ErrorDialog> builder
	) {
		ErrorDialog dialog = new ErrorDialog(screen, title, error);
		if (builder != null) builder.accept(dialog);
		return dialog;
	}
	
	public ErrorDialog(
	  IOverlayCapableScreen screen, ITextComponent title, Throwable error
	) {
		super(screen, title, null);
		setError(error);
		removeButton(cancelButton);
		setConfirmText(new TranslationTextComponent("gui.ok"));
		setIcon(SimpleConfigIcons.ERROR);
	}
	
	public ErrorDialog(
	  IOverlayCapableScreen screen, ITextComponent title, List<ITextComponent> error
	) {
		super(screen, title, null);
		setError(error);
		removeButton(cancelButton);
		setConfirmText(new TranslationTextComponent("gui.ok"));
		setIcon(SimpleConfigIcons.ERROR);
	}
}
