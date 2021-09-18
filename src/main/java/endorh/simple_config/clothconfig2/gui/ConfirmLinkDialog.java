package endorh.simple_config.clothconfig2.gui;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.matrix.MatrixStack;
import endorh.simple_config.clothconfig2.gui.widget.TintedButton;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.DialogTexts;
import net.minecraft.util.Util;
import net.minecraft.util.text.*;

import java.util.List;
import java.util.function.Consumer;

import static java.lang.Math.min;

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
		super(b -> {}, title, body, cancelText, confirmText, screen);
		if (securityWarning)
			body.add(0, new TranslationTextComponent("chat.link.warning"));
		body.add(new TranslationTextComponent("simple-config.ui.link.display", formatLink(link)));
		this.link = link;
		super.setAction(this::action);
		copyButton = new TintedButton(0, 0, 120, 20, copyText, p -> copy());
		copyButton.setTintColor(0x802424BD);
		confirmButton.setTintColor(0x80A07010);
		listeners.add(1, copyButton);
	}
	
	@Override protected void position() {
		super.position();
		int bw = min(150, (w - 16) / 3);
		cancelButton.setWidth(bw);
		confirmButton.setWidth(bw);
		copyButton.setWidth(bw);
		copyButton.x = x + w / 2 - bw / 2;
		cancelButton.x = x + w / 2 - bw * 3 / 2 - 4;
		confirmButton.x = x + w / 2 + bw / 2 + 4;
		copyButton.y = cancelButton.y = confirmButton.y = this.y + h - 24;
	}
	
	@Override public void renderBody(
	  MatrixStack mStack, int mouseX, int mouseY, float delta
	) {
		super.renderBody(mStack, mouseX, mouseY, delta);
		copyButton.render(mStack, mouseX, mouseY, delta);
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
