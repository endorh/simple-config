package endorh.simpleconfig.ui.api;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.gui.IGuiEventListener;
import net.minecraft.client.gui.IRenderable;
import net.minecraft.client.gui.widget.Widget;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Redirects all {@link IGuiEventListener} related events to a given target.<br>
 * Convenient to replace event listeners in multiple queues.
 */
public class RedirectGuiEventListener implements IExtendedDragAwareGuiEventListener, IRenderable {
	private @Nullable IGuiEventListener target;
	public RedirectGuiEventListener(@Nullable IGuiEventListener target) {
		this.target = target;
	}
	
	public @Nullable IGuiEventListener getTarget() {
		return target;
	}
	
	public void setTarget(@Nullable IGuiEventListener target) {
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
		if (button != 0 && !(target instanceof IExtendedDragAwareGuiEventListener)) return false;
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
	
	@Override public boolean changeFocus(boolean focus) {
		if (target == null) return false;
		return target.changeFocus(focus);
	}
	
	@Override public boolean isMouseOver(double mouseX, double mouseY) {
		if (target == null) return false;
		return target.isMouseOver(mouseX, mouseY);
	}
	
	// IExtendedDragAwareGuiEventListener methods
	
	@Override public void endDrag(double mouseX, double mouseY, int button) {
		if (target instanceof IExtendedDragAwareGuiEventListener)
			((IExtendedDragAwareGuiEventListener) target).endDrag(mouseX, mouseY, button);
	}
	
	// IRenderable methods
	
	@Override public void render(@NotNull MatrixStack mStack, int mouseX, int mouseY, float delta) {
		if (target instanceof IRenderable)
			((IRenderable) target).render(mStack, mouseX, mouseY, delta);
	}
	
	// Widget methods
	
	public int getWidth() {
		return target instanceof Widget? ((Widget) target).getWidth() : 0;
	}
	
	public int getHeight() {
		return target instanceof Widget? ((Widget) target).getHeight() : 0;
	}
}
