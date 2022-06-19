package endorh.simpleconfig.clothconfig2.gui;

import com.google.common.collect.Lists;
import endorh.simpleconfig.clothconfig2.gui.widget.TintedButton;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.DialogTexts;
import net.minecraft.util.Util;
import net.minecraft.util.text.*;

import java.util.List;
import java.util.function.Consumer;

public class ConfirmLinkDialog extends ConfirmDialog {
	protected TintedButton copyButton;
	protected String link;
	
	public ConfirmLinkDialog(
	  String link, IOverlayCapableScreen screen, boolean securityWarning
	) {
		this(link, new TranslationTextComponent("chat.link.confirmTrusted"),
		     Lists.newArrayList(), DialogTexts.GUI_CANCEL,
		     new TranslationTextComponent("chat.copy"),
		     new TranslationTextComponent("chat.link.open"), screen, securityWarning);
	}
	
	protected IFormattableTextComponent formatLink(String link) {
		if (link.length() > 256)
			link = link.substring(0, 253) + "...";
		return new StringTextComponent(link).mergeStyle(TextFormatting.GRAY);
	}
	
	public ConfirmLinkDialog(
	  String link, ITextComponent title, List<ITextComponent> body,
	  ITextComponent cancelText, ITextComponent copyText, ITextComponent confirmText,
	  IOverlayCapableScreen screen, boolean securityWarning
	) {
		super(screen, title, b -> {}, body, cancelText, confirmText);
		if (securityWarning)
			body.add(0, new TranslationTextComponent("chat.link.warning"));
		body.add(new TranslationTextComponent("simpleconfig.ui.link.display", formatLink(link)));
		this.link = link;
		super.setAction(this::action);
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
	
	@Override public void setAction(Consumer<Boolean> action) {
		throw new UnsupportedOperationException("Confirm link dialog cannot have a custom action");
	}
	
	public void setCopyButtonTint(int color) {
		copyButton.setTintColor(color);
	}
}
