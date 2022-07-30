package endorh.simpleconfig.ui.gui;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.matrix.MatrixStack;
import endorh.simpleconfig.ui.api.IDialogCapableScreen;
import endorh.simpleconfig.ui.api.IExtendedDragAwareNestedGuiEventHandler;
import endorh.simpleconfig.ui.api.IOverlayCapableContainer;
import endorh.simpleconfig.ui.api.RedirectGuiEventListener;
import endorh.simpleconfig.ui.gui.widget.MultiFunctionImageButton;
import endorh.simpleconfig.ui.gui.widget.MultiFunctionImageButton.ButtonAction;
import endorh.simpleconfig.ui.gui.widget.RectangleAnimator;
import endorh.simpleconfig.ui.math.Rectangle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FocusableGui;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.IGuiEventListener;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public abstract class AbstractDialog
  extends FocusableGui
  implements IExtendedDragAwareNestedGuiEventHandler, IOverlayCapableContainer {
	protected ITextComponent title;
	protected MultiFunctionImageButton copyTextButton;
	protected RedirectGuiEventListener copyTextReference;
	protected int titleColor = 0xFFE0E0E0;
	protected int borderColor = 0xFF909090;
	protected int subBorderColor = 0xFF646464;
	protected int backgroundColor = 0xFF242424;
	protected int backgroundOverlayColor = 0xFF343434;
	protected int screenColor = 0x80101010;
	private Screen screen;
	protected List<IGuiEventListener> listeners = Lists.newArrayList();
	/**
	 * Set to true to avoid cancelling when clicking outside
	 */
	private boolean persistent = false;
	protected boolean cancelled;
	protected Icon icon = Icon.EMPTY;
	
	protected Rectangle area = new Rectangle();
	protected Rectangle animatedArea = new Rectangle();
	protected RectangleAnimator areaAnimator = new RectangleAnimator(150L);
	protected FontRenderer font = Minecraft.getInstance().fontRenderer;
	
	protected Pair<Integer, IGuiEventListener> dragged = null;
	private final SortedOverlayCollection sortedOverlays = new SortedOverlayCollection();
	
	public AbstractDialog(ITextComponent title) {
		this.title = title;
		this.copyTextButton = new MultiFunctionImageButton(
		  0, 0, 18, 18, SimpleConfigIcons.Buttons.COPY, ButtonAction.of(this::copyText)
		  .tooltip(new TranslationTextComponent("simpleconfig.ui.copy_dialog")));
		copyTextReference = new RedirectGuiEventListener(copyTextButton);
		listeners.add(copyTextReference);
	}
	
	@Internal public void setScreen(IDialogCapableScreen screen) {
		if (!(screen instanceof Screen)) throw new IllegalArgumentException(
		  "Invalid screen type: " + screen.getClass().getName() + " does not implement " +
		  "IDialogCapableScreen");
		this.screen = ((AbstractConfigScreen) screen);
		int width = this.screen.width, height = this.screen.height;
		areaAnimator.setOrigin(new Rectangle(
		  width / 2 - 30, height / 2 - 20, 60, 40));
	}
	
	@SuppressWarnings("unchecked") public <T extends Screen & IDialogCapableScreen> T getScreen() {
		if (screen == null) throw new IllegalStateException(
		  "Cannot retrieve screen of dialog before showing the dialog");
		return (T) screen;
	}
	
	public void tick() {}
	
	public void copyText() {
		Minecraft.getInstance().keyboardListener.setClipboardString(getText());
	}
	
	public String getText() {
		return title.getString();
	}
	
	public void cancel() { cancel(false); }
	
	public void cancel(boolean success) {
		setCancelled(true);
	}
	
	protected void layout() {
		int screenWidth = getScreen().width;
		int screenHeight = getScreen().height;
		int w = getArea().getWidth(), h = getArea().getHeight();
		setX(screenWidth / 2 - w / 2);
		setY(screenHeight / 2 - h / 2);
		if (canCopyText()) {
			copyTextReference.setTarget(copyTextButton);
			copyTextButton.x = getX() + getWidth() - 21;
			copyTextButton.y = getY() + 3;
		} else copyTextReference.setTarget(null);
	}
	
	protected void animateLayout() {
		animatedArea = areaAnimator.getCurrentEaseOut();
	}
	
	public boolean render(MatrixStack mStack, int mouseX, int mouseY, float delta) {
		if (isCancelled()) return false;
		layout();
		animateLayout();
		boolean hideHover = shouldOverlaysSuppressHover(mouseX, mouseY);
		int mX = hideHover? -1 : mouseX, mY = hideHover? -1 : mouseY;
		renderBackground(mStack, mX, mY, delta);
		renderTitle(mStack, mX, mY, delta);
		renderBody(mStack, mX, mY, delta);
		renderOverlays(mStack, mouseX, mouseY, delta);
		return true;
	}
	
	public void renderTitle(MatrixStack mStack, int mouseX, int mouseY, float delta) {
		int x = getX(), y = getY(), w = getWidth();
		fill(mStack, x + 1, y + 1, x + w - 1, y + 23, backgroundOverlayColor);
		fill(mStack, x + 1, y + 23, x + w - 1, y + 24, subBorderColor);
		int tx = x + 8;
		if (icon != null && icon != Icon.EMPTY) {
			tx += 18;
			icon.renderStretch(mStack, x + 2, y + 2, 20, 20);
		}
		drawString(mStack, font, title, tx, y + 8, titleColor);
		if (canCopyText())
			copyTextButton.render(mStack, mouseX, mouseY, delta);
	}
	
	public void renderBackground(MatrixStack mStack, int mouseX, int mouseY, float delta) {
		fill(mStack, 0, 0, getScreen().width, getScreen().height, screenColor);
		int x = getX(), y = getY(), w = getWidth(), h = getHeight();
		fill(mStack, x - 8, y - 8, x + w + 8, y + h + 8, 0x24242424);
		fill(mStack, x - 6, y - 6, x + w + 6, y + h + 6, 0x48242424);
		fill(mStack, x - 4, y - 4, x + w + 4, y + h + 4, 0x80242424);
		fill(mStack, x - 2, y - 2, x + w + 2, y + h + 2, 0xa0242424);
		fill(mStack, x, y, x + w, y + h, borderColor);
		fill(mStack, x + 1, y + 1, x + w - 1, y + h - 1, backgroundColor);
	}
	
	public abstract void renderBody(MatrixStack mStack, int mouseX, int mouseY, float delta);
	
	public boolean isMouseInside(double mouseX, double mouseY) {
		int x = getX(), y = getY();
		return mouseX >= x && mouseX < x + getWidth() && mouseY >= y && mouseY < y + getHeight();
	}
	
	@Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (handleOverlaysMouseClicked(mouseX, mouseY, button)) return true;
		if (!isMouseInside(mouseX, mouseY) && !isPersistent()) {
			cancel();
			return true;
		}
		return IExtendedDragAwareNestedGuiEventHandler.super.mouseClicked(mouseX, mouseY, button);
	}
	
	@Override public boolean mouseReleased(double mouseX, double mouseY, int button) {
		if (handleOverlaysMouseReleased(mouseX, mouseY, button)) return true;
		return IExtendedDragAwareNestedGuiEventHandler.super.mouseReleased(mouseX, mouseY, button);
	}
	
	@Override public boolean mouseDragged(
	  double mouseX, double mouseY, int button, double dragX, double dragY
	) {
		if (handleOverlaysMouseDragged(mouseX, mouseY, button, dragX, dragY)) return true;
		return IExtendedDragAwareNestedGuiEventHandler.super.mouseDragged(
		  mouseX, mouseY, button, dragX, dragY);
	}
	
	@Override public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
		if (handleOverlaysMouseScrolled(mouseX, mouseY, delta)) return true;
		return super.mouseScrolled(mouseX, mouseY, delta);
	}
	
	@Override public void endDrag(double mouseX, double mouseY, int button) {
		if (handleOverlaysDragEnd(mouseX, mouseY, button)) return;
		IExtendedDragAwareNestedGuiEventHandler.super.endDrag(mouseX, mouseY, button);
	}
	
	public boolean escapeKeyPressed() {
		if (handleOverlaysEscapeKey()) return true;
		if (!isPersistent()) cancel();
		return true;
	}
	
	// IExtendedDragAwareNestedGuiEventHandler
	
	@Override public @NotNull List<? extends IGuiEventListener> getEventListeners() {
		return listeners;
	}
	@Override public Pair<Integer, IGuiEventListener> getDragged() {
		return dragged;
	}
	@Override public void setDragged(Pair<Integer, IGuiEventListener> dragged) {
		this.dragged = dragged;
	}
	
	// IOverlayCapableContainer
	
	@Override public SortedOverlayCollection getSortedOverlays() {
		return sortedOverlays;
	}
	
	// Getters and Setters
	
	public ITextComponent getTitle() {
		return title;
	}
	public void setTitle(ITextComponent title) {
		this.title = title;
	}
	public boolean canCopyText() {
		return true;
	}
	public boolean isPersistent() {
		return persistent;
	}
	public void setPersistent(boolean persistent) {
		this.persistent = persistent;
	}
	public boolean isCancelled() {
		return cancelled;
	}
	public void setCancelled(boolean cancelled) {
		this.cancelled = cancelled;
	}
	public Icon getIcon() {
		return icon;
	}
	public void setIcon(Icon icon) {
		this.icon = icon;
	}
	protected void setCenterPos(int x, int y) {
	
	}
	
	public Rectangle getArea() {
		return area;
	}
	public int getX() {
		return animatedArea.getX();
	}
	public int getY() {
		return animatedArea.getY();
	}
	public int getWidth() {
		return animatedArea.getWidth();
	}
	public int getHeight() {
		return animatedArea.getHeight();
	}
	
	public void setX(int x) {
		boolean changed = area.x != x;
		area.x = x;
		if (changed) areaAnimator.setTarget(area);
	}
	public void setY(int y) {
		boolean changed = area.y != y;
		area.y = y;
		if (changed) areaAnimator.setTarget(area);
	}
	protected void setWidth(int width) {
		boolean changed = area.width != width;
		area.width = width;
		if (changed) areaAnimator.setTarget(area);
	}
	protected void setHeight(int height) {
		boolean changed = area.height != height;
		area.height = height;
		if (changed) areaAnimator.setTarget(area);
	}
}
