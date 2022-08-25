package endorh.simpleconfig.ui.gui;

import com.google.common.collect.Lists;
import endorh.simpleconfig.ui.gui.widget.TintedButton;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
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
		this(link, Component.translatable("chat.link.confirmTrusted"),
		     Lists.newArrayList(), CommonComponents.GUI_CANCEL,
		     Component.translatable("chat.copy"),
		     Component.translatable("chat.link.open"), securityWarning);
	}
	
	protected MutableComponent formatLink(String link) {
		if (link.length() > 256)
			link = link.substring(0, 253) + "...";
		return Component.literal(link).withStyle(ChatFormatting.GRAY);
	}
	
	public ConfirmLinkDialog(
	  String link, Component title, List<Component> body,
	  Component cancelText, Component copyText, Component confirmText,
	  boolean securityWarning
	) {
		super(title);
		setCancelText(cancelText);
		setConfirmText(confirmText);
		withAction(this::action);
		if (securityWarning)
			body.add(0, Component.translatable("chat.link.warning"));
		body.add(Component.translatable("simpleconfig.ui.link.display", formatLink(link)));
		setBody(body);
		this.link = link;
		copyButton = new TintedButton(0, 0, 120, 20, copyText, p -> copy());
		copyButton.setTintColor(0x802424BD);
		addButton(1, copyButton);
		confirmButton.setTintColor(0x80A07010);
	}
	
	public void copy() {
		cancel(false);
		Minecraft.getInstance().keyboardHandler.setClipboard(link);
	}
	
	public void action(boolean go) {
		if (go) Util.getPlatform().openUri(link);
	}
	
	public void setCopyButtonTint(int color) {
		copyButton.setTintColor(color);
	}
}
