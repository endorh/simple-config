package endorh.simpleconfig.ui.gui;

import com.google.common.collect.Lists;
import endorh.simpleconfig.SimpleConfigMod;
import endorh.simpleconfig.api.ui.icon.Icon;
import endorh.simpleconfig.api.ui.icon.SimpleConfigIcons;
import endorh.simpleconfig.api.ui.math.Rectangle;
import endorh.simpleconfig.ui.api.ContainerEventHandlerEx;
import endorh.simpleconfig.ui.api.IDialogCapableScreen;
import endorh.simpleconfig.ui.api.IOverlayCapableContainer;
import endorh.simpleconfig.ui.api.RedirectGuiEventListener;
import endorh.simpleconfig.ui.gui.widget.MultiFunctionImageButton;
import endorh.simpleconfig.ui.gui.widget.MultiFunctionImageButton.ButtonAction;
import endorh.simpleconfig.ui.gui.widget.RectangleAnimator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.AbstractContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public abstract class AbstractDialog
  extends AbstractContainerEventHandler
  implements ContainerEventHandlerEx, IOverlayCapableContainer {
	protected Component title;
	protected MultiFunctionImageButton copyTextButton;
	protected RedirectGuiEventListener copyTextReference;
	protected int titleColor = 0xFFE0E0E0;
	protected int borderColor = 0xFF909090;
	protected int subBorderColor = 0xFF646464;
	protected int backgroundColor = 0xFF242424;
	protected int backgroundOverlayColor = 0xFF343434;
	protected int screenColor = 0x80101010;
	private Screen screen;
	protected List<GuiEventListener> listeners = Lists.newArrayList();
	/**
	 * Set to true to avoid cancelling when clicking outside
	 */
	private boolean persistent = false;
	protected boolean cancelled;
	protected Icon icon = Icon.EMPTY;
	
	protected Rectangle area = new Rectangle();
	protected Rectangle animatedArea = new Rectangle();
	protected RectangleAnimator areaAnimator = new RectangleAnimator(150L);
	protected Font font = Minecraft.getInstance().font;
	
	protected Pair<Integer, GuiEventListener> dragged = null;
	private final SortedOverlayCollection sortedOverlays = new SortedOverlayCollection();
	
	public AbstractDialog(Component title) {
		this.title = title;
		this.copyTextButton = new MultiFunctionImageButton(
		  0, 0, 18, 18, SimpleConfigIcons.Buttons.COPY, ButtonAction.of(this::copyText)
		  .tooltip(Component.translatable("simpleconfig.ui.copy_dialog")));
		copyTextReference = new RedirectGuiEventListener(copyTextButton);
		listeners.add(copyTextReference);
	}
	
	@Internal public void setScreen(IDialogCapableScreen screen) {
		if (!(screen instanceof Screen)) throw new IllegalArgumentException(
		  "Invalid screen type: " + screen.getClass().getName() + " does not implement " +
		  "IDialogCapableScreen");
		this.screen = (Screen) screen;
		int width = this.screen.width, height = this.screen.height;
		areaAnimator.setOrigin(new Rectangle(
		  width / 2 - 30, height / 2 - 20, 60, 40));
	}
	
	@SuppressWarnings("unchecked") public <T extends Screen & IDialogCapableScreen> T getScreen() {
		if (screen == null) throw new IllegalStateException(
		  "Cannot retrieve screen of dialog before showing the dialog");
		return (T) screen;
	}
	
	public void tick(boolean top) {}
	
	public void copyText() {
		Minecraft.getInstance().keyboardHandler.setClipboard(getText());
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
			copyTextButton.setX(getX() + getWidth() - 21);
			copyTextButton.setY(getY() + 3);
		} else copyTextReference.setTarget(null);
	}
	
	protected void animateLayout() {
		animatedArea = areaAnimator.getCurrentEaseOut();
	}
	
	public boolean render(GuiGraphics gg, int mouseX, int mouseY, float delta) {
		if (isCancelled()) return false;
		layout();
		animateLayout();
		boolean hideHover = shouldOverlaysSuppressHover(mouseX, mouseY);
		int mX = hideHover? -1 : mouseX, mY = hideHover? -1 : mouseY;
		renderBackground(gg, mX, mY, delta);
		renderTitle(gg, mX, mY, delta);
		renderBody(gg, mX, mY, delta);
		renderOverlays(gg, mouseX, mouseY, delta);
		return true;
	}
	
	public void renderTitle(GuiGraphics gg, int mouseX, int mouseY, float delta) {
		int x = getX(), y = getY(), w = getWidth();
		gg.fill(x + 1, y + 1, x + w - 1, y + 23, backgroundOverlayColor);
		gg.fill(x + 1, y + 23, x + w - 1, y + 24, subBorderColor);
		int tx = x + 8;
		if (icon != null && icon != Icon.EMPTY) {
			tx += 18;
			icon.renderCentered(gg, x + 2, y + 2, 20, 20);
		}
		gg.drawString(font, title, tx, y + 8, titleColor);
		if (canCopyText())
			copyTextButton.render(gg, mouseX, mouseY, delta);
	}
	
	public void renderBackground(GuiGraphics gg, int mouseX, int mouseY, float delta) {
		gg.fill(0, 0, getScreen().width, getScreen().height, screenColor);
		int x = getX(), y = getY(), w = getWidth(), h = getHeight();
		gg.fill(x - 8, y - 8, x + w + 8, y + h + 8, 0x24242424);
		gg.fill(x - 6, y - 6, x + w + 6, y + h + 6, 0x48242424);
		gg.fill(x - 4, y - 4, x + w + 4, y + h + 4, 0x80242424);
		gg.fill(x - 2, y - 2, x + w + 2, y + h + 2, 0xa0242424);
		gg.fill(x, y, x + w, y + h, borderColor);
		gg.fill(x + 1, y + 1, x + w - 1, y + h - 1, backgroundColor);
	}
	
	public abstract void renderBody(GuiGraphics gg, int mouseX, int mouseY, float delta);
	
	public boolean isMouseInside(double mouseX, double mouseY) {
		int x = getX(), y = getY();
		return mouseX >= x && mouseX < x + getWidth() && mouseY >= y && mouseY < y + getHeight();
	}
	
	@Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (handleOverlaysMouseClicked(mouseX, mouseY, button)) return true;
		if (!isMouseInside(mouseX, mouseY) && !isPersistent()) {
			cancel();
			Minecraft.getInstance().getSoundManager()
			  .play(SimpleSoundInstance.forUI(SimpleConfigMod.UI_TAP, 0.8F));
			return true;
		}
		return ContainerEventHandlerEx.super.mouseClicked(mouseX, mouseY, button);
	}
	
	@Override public boolean mouseReleased(double mouseX, double mouseY, int button) {
		if (handleOverlaysMouseReleased(mouseX, mouseY, button)) return true;
		return ContainerEventHandlerEx.super.mouseReleased(mouseX, mouseY, button);
	}
	
	@Override public boolean mouseDragged(
	  double mouseX, double mouseY, int button, double dragX, double dragY
	) {
		if (handleOverlaysMouseDragged(mouseX, mouseY, button, dragX, dragY)) return true;
		return ContainerEventHandlerEx.super.mouseDragged(
		  mouseX, mouseY, button, dragX, dragY);
	}
	
	@Override public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
		if (handleOverlaysMouseScrolled(mouseX, mouseY, delta)) return true;
		return super.mouseScrolled(mouseX, mouseY, delta);
	}
	
	@Override public void endDrag(double mouseX, double mouseY, int button) {
		if (handleOverlaysDragEnd(mouseX, mouseY, button)) return;
		ContainerEventHandlerEx.super.endDrag(mouseX, mouseY, button);
	}
	
	public boolean escapeKeyPressed() {
		if (handleOverlaysEscapeKey()) return true;
		if (!isPersistent()) cancel();
		return true;
	}
	
	// IExtendedDragAwareNestedGuiEventHandler
	
	@Override public @NotNull List<? extends GuiEventListener> children() {
		return listeners;
	}
	@Override public Pair<Integer, GuiEventListener> getDragged() {
		return dragged;
	}
	@Override public void setDragged(Pair<Integer, GuiEventListener> dragged) {
		this.dragged = dragged;
	}
	
	// IOverlayCapableContainer
	
	@Override public int getScreenWidth() {
		return getScreen().width;
	}
	@Override public int getScreenHeight() {
		return getScreen().height;
	}
	
	@Override public SortedOverlayCollection getSortedOverlays() {
		return sortedOverlays;
	}
	
	// Getters and Setters
	
	public Component getTitle() {
		return title;
	}
	public void setTitle(Component title) {
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
