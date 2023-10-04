package endorh.simpleconfig.ui.api;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Redirects all {@link GuiEventListener} related events to a given target.<br>
 * Convenient to replace event listeners in multiple queues.
 */
public class RedirectGuiEventListener implements GuiEventListenerEx, Renderable {
	private @Nullable GuiEventListener target;
	public RedirectGuiEventListener(@Nullable GuiEventListener target) {
		this.target = target;
	}
	
	public @Nullable GuiEventListener getTarget() {
		return target;
	}
	
	public void setTarget(@Nullable GuiEventListener target) {
		this.target = target;
	}
	
	@Override public void mouseMoved(double mouseX, double mouseY) {
		if (target == null) return;
		target.mouseMoved(mouseX, mouseY);
	}
	
	@Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (target == null) return false;
		return target.mouseClicked(mouseX, mouseY, button);
	}
	
	@Override public boolean mouseReleased(double mouseX, double mouseY, int button) {
		if (target == null) return false;
		return target.mouseReleased(mouseX, mouseY, button);
	}
	
	@Override public boolean mouseDragged(
	  double mouseX, double mouseY, int button, double dragX, double dragY
	) {
		if (target == null) return false;
		// Hide non-left click drag events if not aware
		if (button != 0 && !(target instanceof GuiEventListenerEx)) return false;
		return target.mouseDragged(mouseX, mouseY, button, dragX, dragY);
	}
	
	@Override public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
		if (target == null) return false;
		return target.mouseScrolled(mouseX, mouseY, delta);
	}
	
	@Override public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (target == null) return false;
		return target.keyPressed(keyCode, scanCode, modifiers);
	}
	
	@Override public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
		if (target == null) return false;
		return target.keyReleased(keyCode, scanCode, modifiers);
	}
	
	@Override public boolean charTyped(char codePoint, int modifiers) {
		if (target == null) return false;
		return target.charTyped(codePoint, modifiers);
	}

	@Nullable @Override public ComponentPath nextFocusPath(@NotNull FocusNavigationEvent e) {
		if (target == null) return null;
		ComponentPath path = target.nextFocusPath(e);
		if (path == null) return null;
		return RedirectPath.redirect(this, path);
	}

	@Nullable @Override public ComponentPath getCurrentFocusPath() {
		if (target == null) return null;
		ComponentPath path = target.getCurrentFocusPath();
		if (path == null) return null;
		return RedirectPath.redirect(this, path);
	}

	@Override public @NotNull ScreenRectangle getRectangle() {
		return target != null? target.getRectangle() : ScreenRectangle.empty();
	}

	@Override public void setFocused(boolean focus) {
		if (target != null) target.setFocused(focus);
	}
	@Override public boolean isFocused() {
		return target != null && target.isFocused();
	}
	
	@Override public boolean isMouseOver(double mouseX, double mouseY) {
		if (target == null) return false;
		return target.isMouseOver(mouseX, mouseY);
	}

	// GuiEventListenerEx methods

	@Override public void endDrag(double mouseX, double mouseY, int button) {
		if (target instanceof GuiEventListenerEx)
			((GuiEventListenerEx) target).endDrag(mouseX, mouseY, button);
	}

	// IRenderable methods
	
	@Override public void render(@NotNull PoseStack mStack, int mouseX, int mouseY, float delta) {
		if (target instanceof Renderable)
			((Renderable) target).render(mStack, mouseX, mouseY, delta);
	}
	
	// Widget methods
	
	public int getWidth() {
		return target == null ? 0 : target.getRectangle().width();
	}
	
	public int getHeight() {
		return target == null ? 0 : target.getRectangle().height();
	}
}
