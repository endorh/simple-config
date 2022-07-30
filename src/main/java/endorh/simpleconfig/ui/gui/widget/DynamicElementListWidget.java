package endorh.simpleconfig.ui.gui.widget;

import endorh.simpleconfig.ui.api.IExtendedDragAwareNestedGuiEventHandler;
import endorh.simpleconfig.ui.gui.widget.DynamicElementListWidget.ElementEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.IGuiEventListener;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.commons.lang3.tuple.Pair;

@OnlyIn(value = Dist.CLIENT)
public abstract class DynamicElementListWidget<E extends ElementEntry>
  extends DynamicNewSmoothScrollingEntryListWidget<E> {
	public DynamicElementListWidget(
	  Minecraft client, int width, int height, int top, int bottom,
	  ResourceLocation backgroundLocation
	) {
		super(client, width, height, top, bottom, backgroundLocation);
	}
	
	public boolean changeFocus(boolean focus) {
		boolean change = super.changeFocus(focus);
		if (change) ensureFocusedVisible();
		return change;
	}
	
	@OnlyIn(value = Dist.CLIENT)
	public static abstract class ElementEntry
	  extends ListEntry
	  implements IExtendedDragAwareNestedGuiEventHandler {
		private IGuiEventListener focused;
		private boolean dragging;
		private Pair<Integer, IGuiEventListener> dragged = null;
		
		public boolean isDragging() {
			return this.dragging;
		}
		
		public void setDragging(boolean dragging) {
			this.dragging = dragging;
		}
		
		public IGuiEventListener getListener() {
			return this.focused;
		}
		public void setListener(IGuiEventListener listener) {
			this.focused = listener;
		}
		
		@Override public Pair<Integer, IGuiEventListener> getDragged() {
			return dragged;
		}
		@Override public void setDragged(Pair<Integer, IGuiEventListener> dragged) {
			this.dragged = dragged;
		}
	}
}

