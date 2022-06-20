package endorh.simpleconfig.clothconfig2.gui;

import com.google.common.collect.Lists;
import net.minecraft.client.gui.DialogTexts;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;

import java.util.List;
import java.util.stream.Collectors;

public class ErrorDialog extends ProgressDialog {
	public ErrorDialog(
	  IOverlayCapableScreen screen, ITextComponent title, Throwable error
	) {this(screen, title, error, Lists.newArrayList());}
	
	public ErrorDialog(
	  IOverlayCapableScreen screen, ITextComponent title, List<ITextComponent> error
	) {this(screen, title, error, Lists.newArrayList());}
	
	public ErrorDialog(
	  IOverlayCapableScreen screen, ITextComponent title,
	  Throwable error, List<ITextComponent> body
	) {this(screen, title, error, body, new TranslationTextComponent("gui.ok"));}
	
	public ErrorDialog(
	  IOverlayCapableScreen screen, ITextComponent title,
	  List<ITextComponent> error, List<ITextComponent> body
	) {this(screen, title, error, body, new TranslationTextComponent("gui.ok"));}
	
	public ErrorDialog(
	  IOverlayCapableScreen screen, ITextComponent title,
	  List<ITextComponent> error, List<ITextComponent> body, ITextComponent confirmText
	) {
		super(screen, title, body, DialogTexts.GUI_CANCEL, null);
		cancelButton.visible = false;
		confirmButton.visible = true;
		setConfirmText(confirmText);
		setError(error.stream().map(t -> t.copy().withStyle(TextFormatting.RED))
		           .collect(Collectors.toList()));
		setIcon(null);
	}
	
	public ErrorDialog(
	  IOverlayCapableScreen screen, ITextComponent title,
	  Throwable error, List<ITextComponent> body, ITextComponent confirmText
	) {
		super(screen, title, body, DialogTexts.GUI_CANCEL, null);
		cancelButton.visible = false;
		confirmButton.visible = true;
		setConfirmText(confirmText);
		setError(error);
		setIcon(null);
	}
}
