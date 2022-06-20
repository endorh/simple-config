package endorh.simpleconfig.clothconfig2.gui.widget;

import endorh.simpleconfig.clothconfig2.gui.IExtendedDragAwareNestedGuiEventHandler;
import endorh.simpleconfig.clothconfig2.gui.widget.DynamicElementListWidget.ElementEntry;
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
	
	@Override
	protected boolean isSelected(int index) {
		return false;
	}
	
	@OnlyIn(value = Dist.CLIENT)
	public static abstract class ElementEntry
	  extends DynamicEntryListWidget.Entry
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
		
		public IGuiEventListener getFocused() {
			return this.focused;
		}
		public void setFocused(IGuiEventListener listener) {
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

