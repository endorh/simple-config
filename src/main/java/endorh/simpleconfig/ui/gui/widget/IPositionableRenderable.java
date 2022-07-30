package endorh.simpleconfig.ui.gui.widget;

import com.mojang.blaze3d.matrix.MatrixStack;
import endorh.simpleconfig.ui.gui.WidgetUtils;
import endorh.simpleconfig.ui.math.Rectangle;
import net.minecraft.client.gui.IGuiEventListener;
import net.minecraft.client.gui.IRenderable;
import net.minecraft.client.gui.widget.Widget;
import org.jetbrains.annotations.NotNull;

/**
 * Generic interface useful for layout controllers.<br>
 * Use {@link #wrap(Widget)} to wrap a {@link Widget} in this interface.
 */
public interface IPositionableRenderable extends IGuiEventListener, IRenderable {
	static WidgetPositionableWrapper wrap(Widget widget) {
		return new WidgetPositionableWrapper(widget);
	}
	
	int getX();
	void setX(int x);
	
	int getY();
	void setY(int y);
	
	int getWidth();
	void setWidth(int w);
	
	int getHeight();
	void setHeight(int h);
	
	default boolean isFocused() {
		return false;
	}
	default void setFocused(boolean focused) {}
	
	default void setPosition(int x, int y) {
		setX(x);
		setY(y);
	}
	
	default void setPosition(int x, int y, int w) {
		setX(x);
		setY(y);
		setWidth(w);
	}
	
	default void setPosition(int x, int y, int w, int h) {
		setX(x);
		setY(y);
		setWidth(w);
		setHeight(h);
	}
	
	default void setDimensions(int w, int h) {
		setWidth(w);
		setHeight(h);
	}
	
	interface IRectanglePositionableRenderable extends IPositionableRenderable {
		Rectangle getArea();
		
		default int getX() {
			return getArea().x;
		}
		default void setX(int x) {
			getArea().x = x;
		}
		
		default int getY() {
			return getArea().y;
		}
		default void setY(int y) {
			getArea().y = y;
		}
		
		default int getWidth() {
			return getArea().width;
		}
		default void setWidth(int w) {
			getArea().width = w;
		}
		
		default int getHeight() {
			return getArea().height;
		}
		default void setHeight(int h) {
			getArea().height = h;
		}
	}
	
	class WidgetPositionableWrapper implements IPositionableRenderable, IGuiEventListener {
		private Widget widget;
		public WidgetPositionableWrapper(Widget widget) {
			this.widget = widget;
		}
		
		public Widget getWidget() {
			return widget;
		}
		
		public void setWidget(Widget widget) {
			this.widget = widget;
		}
		
		@Override public int getX() {
			return widget.x;
		}
		@Override public void setX(int x) {
			widget.x = x;
		}
		
		@Override public int getY() {
			return widget.y;
		}
		@Override public void setY(int y) {
			widget.y = y;
		}
		
		@Override public int getWidth() {
			return widget.getWidth();
		}
		@Override public void setWidth(int w) {
			widget.setWidth(w);
		}
		
		@Override public int getHeight() {
			return widget.getHeightRealms();
		}
		@Override public void setHeight(int h) {
			widget.setHeight(h);
		}
		
		@Override public void render(
		  @NotNull MatrixStack mStack, int mouseX, int mouseY, float partialTicks
		) {
			widget.render(mStack, mouseX, mouseY, partialTicks);
		}
		
		@Override public void mouseMoved(double xPos, double mouseY) {
			widget.mouseMoved(xPos, mouseY);
		}
		@Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
			return widget.mouseClicked(mouseX, mouseY, button);
		}
		@Override public boolean mouseReleased(double mouseX, double mouseY, int button) {
			return widget.mouseReleased(mouseX, mouseY, button);
		}
		@Override public boolean mouseDragged(
		  double mouseX, double mouseY, int button, double dragX, double dragY
		) {
			return widget.mouseDragged(mouseX, mouseY, button, dragX, dragY);
		}
		@Override public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
			return widget.mouseScrolled(mouseX, mouseY, delta);
		}
		
		@Override public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
			return widget.keyPressed(keyCode, scanCode, modifiers);
		}
		@Override public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
			return widget.keyReleased(keyCode, scanCode, modifiers);
		}
		@Override public boolean charTyped(char codePoint, int modifiers) {
			return widget.charTyped(codePoint, modifiers);
		}
		
		@Override public boolean changeFocus(boolean focus) {
			return widget.changeFocus(focus);
		}
		
		@Override public boolean isMouseOver(double mouseX, double mouseY) {
			return widget.isMouseOver(mouseX, mouseY);
		}
		
		@Override public boolean isFocused() {
			return widget.isFocused();
		}
		
		@Override public void setFocused(boolean focused) {
			WidgetUtils.forceSetFocus(widget, focused);
		}
	}
}
