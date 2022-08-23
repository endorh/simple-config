package endorh.simpleconfig.ui.api;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Widget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Redirects all {@link IGuiEventListener} related events to a given target.<br>
 * Convenient to replace event listeners in multiple queues.
 */
public class RedirectGuiEventListener implements IExtendedDragAwareGuiEventListener, Widget {
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
	
	@Override public void render(@NotNull PoseStack mStack, int mouseX, int mouseY, float delta) {
		if (target instanceof Widget)
			((Widget) target).render(mStack, mouseX, mouseY, delta);
	}
	
	// Widget methods
	
	public int getWidth() {
		return target instanceof AbstractWidget? ((AbstractWidget) target).getWidth() : 0;
	}
	
	public int getHeight() {
		return target instanceof AbstractWidget? ((AbstractWidget) target).getHeight() : 0;
	}
}
