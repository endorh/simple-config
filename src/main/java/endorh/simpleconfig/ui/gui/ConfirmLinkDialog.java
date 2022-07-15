package endorh.simpleconfig.ui.gui;

import com.google.common.collect.Lists;
import endorh.simpleconfig.ui.gui.widget.TintedButton;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.DialogTexts;
import net.minecraft.util.Util;
import net.minecraft.util.text.*;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;

public class ConfirmLinkDialog extends ConfirmDialog {
	protected TintedButton copyButton;
	protected String link;
	
	public static ConfirmLinkDialog create(
	  String link, boolean securityWarning
	) {
		return create(link, securityWarning, null);
	}
	
	public static ConfirmLinkDialog create(
	  String link, boolean securityWarning,
	  @Nullable Consumer<ConfirmLinkDialog> builder
	) {
		ConfirmLinkDialog dialog = new ConfirmLinkDialog(link, securityWarning);
		if (builder != null) builder.accept(dialog);
		return dialog;
	}
	
	public ConfirmLinkDialog(
	  String link, boolean securityWarning
	) {
		this(link, new TranslationTextComponent("chat.link.confirmTrusted"),
		     Lists.newArrayList(), DialogTexts.GUI_CANCEL,
		     new TranslationTextComponent("chat.copy"),
		     new TranslationTextComponent("chat.link.open"), securityWarning);
	}
	
	protected IFormattableTextComponent formatLink(String link) {
		if (link.length() > 256)
			link = link.substring(0, 253) + "...";
		return new StringTextComponent(link).mergeStyle(TextFormatting.GRAY);
	}
	
	public ConfirmLinkDialog(
	  String link, ITextComponent title, List<ITextComponent> body,
	  ITextComponent cancelText, ITextComponent copyText, ITextComponent confirmText,
	  boolean securityWarning
	) {
		super(title);
		setCancelText(cancelText);
		setConfirmText(confirmText);
		withAction(this::action);
		if (securityWarning)
			body.add(0, new TranslationTextComponent("chat.link.warning"));
		body.add(new TranslationTextComponent("simpleconfig.ui.link.display", formatLink(link)));
		setBody(body);
		this.link = link;
		copyButton = new TintedButton(0, 0, 120, 20, copyText, p -> copy());
		copyButton.setTintColor(0x802424BD);
		addButton(1, copyButton);
		confirmButton.setTintColor(0x80A07010);
	}
	
	public void copy() {
		cancel(false);
		Minecraft.getInstance().keyboardListener.setClipboardString(link);
	}
	
	public void action(boolean go) {
		if (go) Util.getOSType().openURI(link);
	}
	
	public void setCopyButtonTint(int color) {
		copyButton.setTintColor(color);
	}
}
