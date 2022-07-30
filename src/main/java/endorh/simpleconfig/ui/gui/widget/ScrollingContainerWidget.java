package endorh.simpleconfig.ui.gui.widget;

import com.mojang.blaze3d.matrix.MatrixStack;
import endorh.simpleconfig.ui.api.IExtendedDragAwareNestedGuiEventHandler;
import endorh.simpleconfig.ui.api.ScissorsHandler;
import endorh.simpleconfig.ui.api.ScrollingHandler;
import endorh.simpleconfig.ui.math.Rectangle;
import net.minecraft.client.gui.IGuiEventListener;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.util.math.MathHelper;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import static java.lang.Math.abs;
import static java.lang.Math.round;

public abstract class ScrollingContainerWidget extends ScrollingHandler
  implements IExtendedDragAwareNestedGuiEventHandler {
	public final Rectangle area;
	
	protected final List<IGuiEventListener> listeners = new ArrayList<>();
	protected IGuiEventListener listener;
	protected Pair<Integer, IGuiEventListener> dragged;
	protected boolean dragging;
	
	public ScrollingContainerWidget(int x, int y, int w, int h) {
		this(new Rectangle(x, y, w, h));
	}
	
	public ScrollingContainerWidget(Rectangle area) {
		this.area = area;
		init();
	}
	
	public void init() {}
	
	@Override public Rectangle getBounds() {
		return area;
	}
	
	public void updateScroll() {
		final double prev = scrollAmount;
		final int maxScroll = getMaxScroll();
		updatePosition(0F);
		scrollAmount = MathHelper.clamp(scrollAmount, 0, maxScroll);
	}
	
	public void scrollBy(double amount, boolean animated) {
		final int maxScroll = getMaxScroll();
		if (amount < 0 && scrollTarget > maxScroll)
			scrollTo(maxScroll + amount, animated);
		if (amount > 0 && scrollTarget < 0)
			scrollTo(amount, animated);
		else scrollTo(scrollTarget + amount, animated);
	}
	
	public void position() {}
	
	public void render(MatrixStack mStack, int mouseX, int mouseY, float delta) {
		updateScroll();
		position();
		ScissorsHandler.INSTANCE.pushScissor(area);
		renderInner(
		  mStack, area.x, (int) round(area.y - scrollAmount),
		  area.width - 8, area.height, mouseX, mouseY, delta);
		ScissorsHandler.INSTANCE.popScissor();
		renderScrollBar();
	}
	
	public abstract void renderInner(
	  MatrixStack mStack, int x, int y, int w, int h, int mouseX, int mouseY, float delta);
	
	public abstract int getInnerHeight();
	
	@Override public final int getMaxScrollHeight() {
		return getInnerHeight();
	}
	
	@Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (updateDraggingState(mouseX, mouseY, button)) return true;
		return IExtendedDragAwareNestedGuiEventHandler.super.mouseClicked(mouseX, mouseY, button);
	}
	
	@Override public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
		if (IExtendedDragAwareNestedGuiEventHandler.super.mouseScrolled(mouseX, mouseY, delta))
			return true;
		scrollBy(delta * -24, abs(delta) >= 1.0);
		return true;
	}
	
	@Override public boolean mouseDragged(double mouseX, double mouseY, int button, double dx, double dy) {
		if (super.mouseDragged(mouseX, mouseY, button, dx, dy)) return true;
		return IExtendedDragAwareNestedGuiEventHandler.super.mouseDragged(
		  mouseX, mouseY, button, dx, dy
		);
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
	@Override public boolean isDragging() {
		return dragging;
	}
	@Override public void setDragging(boolean dragging) {
		this.dragging = dragging;
	}
	@Nullable @Override public IGuiEventListener getListener() {
		return listener;
	}
	@Override public void setListener(@Nullable IGuiEventListener listener) {
		this.listener = listener;
	}
	
	@Override public boolean changeFocus(boolean focus) {
		final boolean change = IExtendedDragAwareNestedGuiEventHandler.super.changeFocus(focus);
		if (change) {
			final IGuiEventListener listener = getListener();
			if (listener != null) {
				if (listener instanceof Widget) {
					final Widget widget = (Widget) listener;
					int target = widget.y + widget.getHeightRealms() / 2 - area.y;
					scrollTo(target, true);
				}
			}
		}
		return change;
	}
	
	@Override public boolean isMouseOver(double mouseX, double mouseY) {
		return area.contains(mouseX, mouseY);
	}
}
