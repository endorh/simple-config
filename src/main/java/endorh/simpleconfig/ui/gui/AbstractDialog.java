package endorh.simpleconfig.ui.gui;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.matrix.MatrixStack;
import endorh.simpleconfig.ui.gui.widget.MultiFunctionImageButton;
import endorh.simpleconfig.ui.gui.widget.MultiFunctionImageButton.ButtonAction;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FocusableGui;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.IGuiEventListener;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public abstract class AbstractDialog
  extends FocusableGui implements IExtendedDragAwareNestedGuiEventHandler {
	protected ITextComponent title;
	protected MultiFunctionImageButton copyTextButton;
	protected int titleColor = 0xffe0e0e0;
	protected int borderColor = 0xff909090;
	protected int subBorderColor = 0xff646464;
	protected int backgroundColor = 0xff242424;
	protected int backgroundOverlayColor = 0xff343434;
	protected int screenColor = 0x80101010;
	protected IOverlayCapableScreen overlayScreen;
	protected Screen screen;
	protected List<IGuiEventListener> listeners = Lists.newArrayList();
	/**
	 * Set to true to avoid cancelling when clicking outside
	 */
	protected boolean persistent = false;
	protected boolean cancelled;
	protected Icon icon = Icon.EMPTY;
	
	protected int x, y, w, h;
	protected FontRenderer font = Minecraft.getInstance().fontRenderer;
	
	protected Pair<Integer, IGuiEventListener> dragged = null;
	
	public AbstractDialog(
	  IOverlayCapableScreen screen, ITextComponent title
	) {
		this.title = title;
		this.overlayScreen = screen;
		if (!(screen instanceof Screen))
			throw new IllegalArgumentException("Unknown Screen class: " + screen.getClass().getName());
		this.screen = (Screen) screen;
		this.copyTextButton = new MultiFunctionImageButton(
		  0, 0, 18, 18, SimpleConfigIcons.COPY, ButtonAction.of(this::copyText)
		  .tooltip(new TranslationTextComponent("simpleconfig.ui.copy_dialog")));
		listeners.add(copyTextButton);
	}
	
	public void copyText() {
		Minecraft.getInstance().keyboardListener.setClipboardString(getText());
	}
	
	public String getText() {
		return title.getString();
	}
	
	public void cancel() { cancel(false); }
	
	public void cancel(boolean success) {
		cancelled = true;
	}
	
	protected void layout() {
		final int width = screen.width;
		final int height = screen.height;
		x = width / 2 - w / 2;
		y = height / 2 - h / 2;
		copyTextButton.x = x + w - 21;
		copyTextButton.y = y + 3;
	}
	
	public boolean render(MatrixStack mStack, int mouseX, int mouseY, float delta) {
		if (cancelled) return false;
		layout();
		renderBackground(mStack, mouseX, mouseY, delta);
		renderTitle(mStack, mouseX, mouseY, delta);
		renderBody(mStack, mouseX, mouseY, delta);
		return true;
	}
	
	public void renderTitle(MatrixStack mStack, int mouseX, int mouseY, float delta) {
		fill(mStack, x + 1, y + 1, x + w - 1, y + 23, backgroundOverlayColor);
		fill(mStack, x + 1, y + 23, x + w - 1, y + 24, subBorderColor);
		int tx = x + 8;
		if (icon != null && icon != Icon.EMPTY) {
			tx += 18;
			icon.renderStretch(mStack, x + 2, y + 2, 20, 20);
		}
		drawString(mStack, font, title, tx, y + 8, titleColor);
		copyTextButton.render(mStack, mouseX, mouseY, delta);
	}
	
	public void renderBackground(MatrixStack mStack, int mouseX, int mouseY, float delta) {
		fill(mStack, 0, 0, screen.width, screen.height, screenColor);
		fill(mStack, x - 8, y - 8, x + w + 8, y + h + 8, 0x24242424);
		fill(mStack, x - 6, y - 6, x + w + 6, y + h + 6, 0x48242424);
		fill(mStack, x - 4, y - 4, x + w + 4, y + h + 4, 0x80242424);
		fill(mStack, x - 2, y - 2, x + w + 2, y + h + 2, 0xa0242424);
		fill(mStack, x, y, x + w, y + h, borderColor);
		fill(mStack, x + 1, y + 1, x + w - 1, y + h - 1, backgroundColor);
	}
	
	public abstract void renderBody(MatrixStack mStack, int mouseX, int mouseY, float delta);
	
	public boolean isMouseInside(double mouseX, double mouseY) {
		return mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
	}
	
	@Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (!isMouseInside(mouseX, mouseY) && !persistent) {
			cancel();
			return true;
		}
		return IExtendedDragAwareNestedGuiEventHandler.super.mouseClicked(mouseX, mouseY, button);
	}
	
	public boolean escapeKeyPressed() {
		if (!persistent) cancel();
		return true;
	}
	
	@Override public @NotNull List<? extends IGuiEventListener> getEventListeners() {
		return listeners;
	}
	@Override public Pair<Integer, IGuiEventListener> getDragged() {
		return dragged;
	}
	@Override public void setDragged(Pair<Integer, IGuiEventListener> dragged) {
		this.dragged = dragged;
	}
	
	public ITextComponent getTitle() {
		return title;
	}
	public void setTitle(ITextComponent title) {
		this.title = title;
	}
	public boolean isPersistent() {
		return persistent;
	}
	public void setPersistent(boolean persistent) {
		this.persistent = persistent;
	}
	public Icon getIcon() {
		return icon;
	}
	public void setIcon(Icon icon) {
		this.icon = icon;
	}
}
