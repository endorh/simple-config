package endorh.simpleconfig.ui.gui.widget;

import com.mojang.blaze3d.matrix.MatrixStack;
import endorh.simpleconfig.api.ui.math.Rectangle;
import endorh.simpleconfig.ui.api.IExtendedDragAwareNestedGuiEventHandler;
import endorh.simpleconfig.ui.api.IOverlayCapableContainer.IOverlayRenderer;
import endorh.simpleconfig.ui.gui.widget.IPositionableRenderable.IRectanglePositionableRenderable;
import net.minecraft.client.gui.FocusableGui;
import net.minecraft.client.gui.IGuiEventListener;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public abstract class FocusableOverlayRenderer extends FocusableGui
  implements IRectanglePositionableRenderable,
             IExtendedDragAwareNestedGuiEventHandler,
             IOverlayRenderer {
	private final Rectangle area = new Rectangle();
	private boolean hidden = false;
	private Pair<Integer, IGuiEventListener> dragged = null;
	private boolean active;
	protected List<IGuiEventListener> listeners = new ArrayList<>();
	
	@Override public boolean renderOverlay(
	  MatrixStack mStack, Rectangle area, int mouseX, int mouseY, float delta
	) {
		if (area != getArea()) return false;
		if (hidden) {
			hidden = false;
			return false;
		}
		render(mStack, mouseX, mouseY, delta);
		return true;
	}
	
	public void hide() {
		hidden = true;
	}
	
	@Override
	public boolean overlayMouseClicked(Rectangle area, double mouseX, double mouseY, int button) {
		if (area != getArea()) return false;
		return mouseClicked(mouseX, mouseY, button) || isMouseOver(mouseX, mouseY);
	}
	
	@Override public void overlayMouseClickedOutside(
	  Rectangle area, double mouseX, double mouseY, int button
	) {
		if (area == getArea()) hide();
	}
	
	@Override public boolean overlayMouseDragged(
	  Rectangle area, double mouseX, double mouseY, int button, double dragX, double dragY
	) {
		if (area != getArea()) return false;
		return mouseDragged(mouseX, mouseY, button, dragX, dragY);
	}
	
	@Override
	public void overlayMouseDragEnd(Rectangle area, double mouseX, double mouseY, int button) {
		if (area == getArea()) return;
		endDrag(mouseX, mouseY, button);
	}
	
	@Override
	public boolean overlayMouseReleased(Rectangle area, double mouseX, double mouseY, int button) {
		if (area != getArea()) return false;
		mouseReleased(mouseX, mouseY, button);
		return true;
	}
	
	@Override public boolean overlayMouseScrolled(
	  Rectangle area, double mouseX, double mouseY, double amount
	) {
		if (area != getArea()) return false;
		return mouseScrolled(mouseX, mouseY, amount) || isMouseOver(mouseX, mouseY);
	}
	
	@Override public boolean isOverlayDragging() {
		return isDragging();
	}
	
	@Override public boolean overlayEscape() {
		hide();
		return true;
	}
	
	@Override public Pair<Integer, IGuiEventListener> getDragged() {
		return dragged;
	}
	@Override public void setDragged(Pair<Integer, IGuiEventListener> dragged) {
		this.dragged = dragged;
	}
	
	@Override public @NotNull List<IGuiEventListener> getEventListeners() {
		return listeners;
	}
	
	@Override public Rectangle getArea() {
		return area;
	}
	@Override public boolean isMouseOver(double mouseX, double mouseY) {
		return getArea().contains(mouseX, mouseY);
	}
	
	@Override public boolean isFocused() {
		return getListener() != null;
	}
	@Override public void setFocused(boolean focused) {
		setListener(focused && !listeners.isEmpty()? listeners.get(0) : null);
	}
	
	@Override public boolean isActive() {
		return active;
	}
	@Override public void setActive(boolean active) {
		this.active = active;
	}
}
