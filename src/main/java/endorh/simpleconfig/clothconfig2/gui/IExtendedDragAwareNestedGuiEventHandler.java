package endorh.simpleconfig.clothconfig2.gui;


import net.minecraft.client.gui.IGuiEventListener;
import net.minecraft.client.gui.INestedGuiEventHandler;
import org.apache.commons.lang3.tuple.Pair;

/**
 * Propagates drag events with other buttons than 0<br>
 * Adds the method {@link IExtendedDragAwareNestedGuiEventHandler#endDrag(double, double, int)}
 */
public interface IExtendedDragAwareNestedGuiEventHandler extends INestedGuiEventHandler {
	
	Pair<Integer, IGuiEventListener> getDragged();
	void setDragged(Pair<Integer, IGuiEventListener> dragged);
	
	@Override default boolean mouseClicked(double mouseX, double mouseY, int button) {
		for(IGuiEventListener listener : this.children()) {
			if (listener.mouseClicked(mouseX, mouseY, button)) {
				this.setFocused(listener);
				if ((!isDragging() || (getDragged() != null && getDragged().getLeft() == button)) && (
				  button == 0
				  || listener instanceof IExtendedDragAwareNestedGuiEventHandler
				  || listener instanceof IExtendedDragAwareGuiEventListener)
				) {
					this.setDragging(true);
					this.setDragged(Pair.of(button, listener));
				}
				return true;
			}
		}
		return false;
	}
	
	@Override default boolean mouseDragged(
	  double mouseX, double mouseY, int button, double dragX, double dragY
	) {
		return this.getFocused() != null && this.isDragging()
		       && (button == 0
		           || getFocused() instanceof IExtendedDragAwareNestedGuiEventHandler
		           || getFocused() instanceof IExtendedDragAwareGuiEventListener)
		       && this.getFocused().mouseDragged(mouseX, mouseY, button, dragX, dragY);
	}
	
	default void endDrag(double mouseX, double mouseY, int button) {
		final Pair<Integer, IGuiEventListener> dragged = getDragged();
		if (dragged != null) {
			if (dragged.getLeft() != button)
				button = -1;
			if (dragged.getRight() instanceof IExtendedDragAwareNestedGuiEventHandler) {
				((IExtendedDragAwareNestedGuiEventHandler) dragged.getRight())
				  .endDrag(mouseX, mouseY, button);
				setDragging(false);
				setDragged(null);
			} else if (dragged.getRight() instanceof IExtendedDragAwareGuiEventListener) {
				if (dragged.getLeft() == button)
					((IExtendedDragAwareGuiEventListener) dragged.getRight())
					  .endDrag(mouseX, mouseY, button);
				setDragging(false);
				setDragged(null);
			} else {
				setDragging(false);
				setDragged(null);
			}
		}
	}
	
	@Override default boolean mouseReleased(double mouseX, double mouseY, int button) {
		if (getDragged() != null) {
			if (button == getDragged().getLeft())
				endDrag(mouseX, mouseY, button);
		}
		return INestedGuiEventHandler.super.mouseReleased(mouseX, mouseY, button);
	}
}
