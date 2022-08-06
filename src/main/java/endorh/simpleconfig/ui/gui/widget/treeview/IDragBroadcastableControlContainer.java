package endorh.simpleconfig.ui.gui.widget.treeview;

import endorh.simpleconfig.ui.api.IExtendedDragAwareNestedGuiEventHandler;
import endorh.simpleconfig.ui.gui.widget.IPositionableRenderable;
import endorh.simpleconfig.ui.gui.widget.treeview.DragBroadcastableControl.DragBroadcastableAction;
import net.minecraft.client.gui.IGuiEventListener;
import net.minecraft.client.gui.INestedGuiEventHandler;

public interface IDragBroadcastableControlContainer
  extends IExtendedDragAwareNestedGuiEventHandler {
	default <W extends IPositionableRenderable> void startDragBroadcastableAction(
	  DragBroadcastableAction<W> action, DragBroadcastableControl<W> source
	) {
		setDragBroadcastableAction(action, source);
		action.start(source.getControl());
	}
	
	default <W extends IPositionableRenderable> void applyDragBroadcastAction(
	  DragBroadcastableControl<W> target
	) {
		DragBroadcastableAction<W> action = target.getAction();
		if (action == getDragBroadcastableAction()) {
			//noinspection unchecked
			action.apply(target.getControl(), (W) getDragBroadcastableSource().getControl());
		}
	}
	
	<W extends IPositionableRenderable> void setDragBroadcastableAction(
	  DragBroadcastableAction<W> action, DragBroadcastableControl<W> source);
	
	DragBroadcastableAction<?> getDragBroadcastableAction();
	
	DragBroadcastableControl<?> getDragBroadcastableSource();
	@Override default boolean mouseDragged(
	  double mouseX, double mouseY, int button, double dragX, double dragY
	) {
		DragBroadcastableAction<?> action = getDragBroadcastableAction();
		if (action != null) {
			IGuiEventListener listener = this;
			while (listener instanceof INestedGuiEventHandler)
				listener = ((INestedGuiEventHandler) listener)
				  .getEventListenerForPos(mouseX, mouseY).orElse(null);
			if (listener instanceof DragBroadcastableControl)
				applyDragBroadcastAction(((DragBroadcastableControl<?>) listener));
			return true;
		}
		return IExtendedDragAwareNestedGuiEventHandler.super.mouseDragged(
		  mouseX, mouseY, button, dragX, dragY);
	}
	
	@Override default void endDrag(double mouseX, double mouseY, int button) {
		DragBroadcastableControl<?> source = getDragBroadcastableSource();
		if (source != null) endDragBroadcastableAction(source);
		IExtendedDragAwareNestedGuiEventHandler.super.endDrag(mouseX, mouseY, button);
	}
	
	default <W extends IPositionableRenderable> void endDragBroadcastableAction(
	  DragBroadcastableControl<W> source
	) {
		setDragBroadcastableAction(null, null);
		source.getAction().end(source.getControl());
	}
}
