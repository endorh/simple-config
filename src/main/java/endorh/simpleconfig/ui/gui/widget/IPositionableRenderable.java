package endorh.simpleconfig.ui.gui.widget;

import endorh.simpleconfig.api.ui.math.Rectangle;
import endorh.simpleconfig.ui.api.RedirectPath;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Generic interface useful for layout controllers.<br>
 * Use {@link #wrap(AbstractWidget)} to wrap a {@link Renderable} in this interface.<br>
 *
 * Subclasses should consider implementing the optional methods
 * {@link #isFocused()}, {@link #isActive()} and their setters.
 */
public interface IPositionableRenderable extends Renderable, GuiEventListener {
	static <W extends AbstractWidget> WidgetPositionableWrapper<W> wrap(W widget) {
		return new WidgetPositionableWrapper<>(widget);
	}
	
	int getX();
	void setX(int x);
	
	int getY();
	void setY(int y);
	
	int getWidth();
	void setWidth(int w);
	
	int getHeight();
	void setHeight(int h);
	
	default boolean isActive() {
		return true;
	}
	default void setActive(boolean active) {}
	
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
		
		@Override default int getX() {
			return getArea().x;
		}
		@Override default void setX(int x) {
			getArea().x = x;
		}
		
		@Override default int getY() {
			return getArea().y;
		}
		@Override default void setY(int y) {
			getArea().y = y;
		}
		
		@Override default int getWidth() {
			return getArea().width;
		}
		@Override default void setWidth(int w) {
			getArea().width = w;
		}
		
		@Override default int getHeight() {
			return getArea().height;
		}
		@Override default void setHeight(int h) {
			getArea().height = h;
		}
	}
	
	class WidgetPositionableWrapper<W extends AbstractWidget>
	  implements IPositionableRenderable {
		private W widget;
		public WidgetPositionableWrapper(W widget) {
			this.widget = widget;
		}
		
		public W getWidget() {
			return widget;
		}
		public void setWidget(W widget) {
			this.widget = widget;
		}
		
		@Override public int getX() {
			return widget.getX();
		}
		@Override public void setX(int x) {
			widget.setX(x);
		}
		
		@Override public int getY() {
			return widget.getY();
		}
		@Override public void setY(int y) {
			widget.setY(y);
		}
		
		@Override public int getWidth() {
			return widget.getWidth();
		}
		@Override public void setWidth(int w) {
			widget.setWidth(w);
		}
		
		@Override public int getHeight() {
			return widget.getHeight();
		}
		@Override public void setHeight(int h) {
			widget.setHeight(h);
		}
		
		@Override public void render(
         @NotNull GuiGraphics gg, int mouseX, int mouseY, float partialTicks
		) {
			widget.render(gg, mouseX, mouseY, partialTicks);
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

		@Nullable @Override public ComponentPath nextFocusPath(@NotNull FocusNavigationEvent e) {
			ComponentPath path = widget.nextFocusPath(e);
			return path != null? RedirectPath.redirect(this, path) : null;
		}
		@Nullable @Override public ComponentPath getCurrentFocusPath() {
			ComponentPath path = widget.getCurrentFocusPath();
			return path != null? RedirectPath.redirect(this, path) : null;
		}

		@Override public @NotNull ScreenRectangle getRectangle() {
			return widget.getRectangle();
		}

		@Override public boolean charTyped(char codePoint, int modifiers) {
			return widget.charTyped(codePoint, modifiers);
		}
		@Override public boolean isMouseOver(double mouseX, double mouseY) {
			return widget.isMouseOver(mouseX, mouseY);
		}
		
		@Override public boolean isFocused() {
			return widget.isFocused();
		}
		@Override public void setFocused(boolean focused) {
			((GuiEventListener) widget).setFocused(focused);
		}
		
		@Override public boolean isActive() {
			return widget.active;
		}
		@Override public void setActive(boolean active) {
			widget.active = active;
		}
	}
	
	interface IDelegatedPositionableRenderable extends IPositionableRenderable {
		@NotNull IPositionableRenderable getDelegate();
		
		@Override default int getX() {
			return getDelegate().getX();
		}
		@Override default void setX(int x) {
			getDelegate().setX(x);
		}
		
		@Override default int getY() {
			return getDelegate().getY();
		}
		@Override default void setY(int y) {
			getDelegate().setY(y);
		}
		
		@Override default int getWidth() {
			return getDelegate().getWidth();
		}
		@Override default void setWidth(int w) {
			getDelegate().setWidth(w);
		}
		
		@Override default int getHeight() {
			return getDelegate().getHeight();
		}
		@Override default void setHeight(int h) {
			getDelegate().setHeight(h);
		}
		
		@Override default boolean isFocused() {
			return getDelegate().isFocused();
		}
		@Override default void setFocused(boolean focused) {
			getDelegate().setFocused(focused);
		}
		
		@Override default boolean isActive() {
			return getDelegate().isActive();
		}
		@Override default void setActive(boolean active) {
			getDelegate().setActive(active);
		}
		
		@Override default void setPosition(int x, int y) {
			getDelegate().setPosition(x, y);
		}
		@Override default void setPosition(int x, int y, int w) {
			getDelegate().setPosition(x, y, w);
		}
		@Override default void setPosition(int x, int y, int w, int h) {
			getDelegate().setPosition(x, y, w, h);
		}
		@Override default void setDimensions(int w, int h) {
			getDelegate().setDimensions(w, h);
		}
		
		@Override default void render(@NotNull GuiGraphics gg, int mouseX, int mouseY, float partialTicks) {
			getDelegate().render(gg, mouseX, mouseY, partialTicks);
		}
		
		@Override default boolean mouseClicked(double mouseX, double mouseY, int button) {
			return getDelegate().mouseClicked(mouseX, mouseY, button);
		}
		@Override default boolean mouseReleased(double mouseX, double mouseY, int button) {
			return getDelegate().mouseReleased(mouseX, mouseY, button);
		}
		@Override default boolean mouseDragged(
		  double mouseX, double mouseY, int button, double deltaX, double deltaY
		) {
			return getDelegate().mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
		}
		@Override default boolean mouseScrolled(double mouseX, double mouseY, double delta) {
			return getDelegate().mouseScrolled(mouseX, mouseY, delta);
		}
		@Override default void mouseMoved(double xPos, double mouseY) {
			getDelegate().mouseMoved(xPos, mouseY);
		}
		@Override default boolean keyPressed(int keyCode, int scanCode, int modifiers) {
			return getDelegate().keyPressed(keyCode, scanCode, modifiers);
		}
		@Override default boolean keyReleased(int keyCode, int scanCode, int modifiers) {
			return getDelegate().keyReleased(keyCode, scanCode, modifiers);
		}
		@Override default boolean charTyped(char codePoint, int modifiers) {
			return getDelegate().charTyped(codePoint, modifiers);
		}
		@Override default boolean isMouseOver(double mouseX, double mouseY) {
			return getDelegate().isMouseOver(mouseX, mouseY);
		}

		@Nullable @Override default ComponentPath nextFocusPath(@NotNull FocusNavigationEvent e) {
			ComponentPath path = getDelegate().nextFocusPath(e);
			return path != null ? RedirectPath.redirect(this, path) : null;
		}
		@Nullable @Override default ComponentPath getCurrentFocusPath() {
			ComponentPath path = getDelegate().getCurrentFocusPath();
			return path != null ? RedirectPath.redirect(this, path) : null;
		}
		@Override default @NotNull ScreenRectangle getRectangle() {
			return getDelegate().getRectangle();
		}
	}
}
