package endorh.simple_config.clothconfig2.gui.widget;

import endorh.simple_config.clothconfig2.gui.widget.DynamicElementListWidget.ElementEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.IGuiEventListener;
import net.minecraft.client.gui.INestedGuiEventHandler;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(value = Dist.CLIENT)
public abstract class DynamicElementListWidget<E extends ElementEntry<E>>
  extends DynamicNewSmoothScrollingEntryListWidget<E> {
	public DynamicElementListWidget(
	  Minecraft client, int width, int height, int top, int bottom,
	  ResourceLocation backgroundLocation
	) {
		super(client, width, height, top, bottom, backgroundLocation);
	}
	
	public boolean changeFocus(boolean boolean_1) {
		boolean boolean_2 = super.changeFocus(boolean_1);
		if (boolean_2) {
			this.ensureVisible(this.getFocused());
		}
		return boolean_2;
	}
	
	@Override
	protected boolean isSelected(int int_1) {
		return false;
	}
	
	@OnlyIn(value = Dist.CLIENT)
	public static abstract class ElementEntry<E extends ElementEntry<E>>
	  extends DynamicEntryListWidget.Entry<E>
	  implements INestedGuiEventHandler {
		private IGuiEventListener focused;
		private boolean dragging;
		
		public boolean isDragging() {
			return this.dragging;
		}
		
		public void setDragging(boolean boolean_1) {
			this.dragging = boolean_1;
		}
		
		public IGuiEventListener getListener() {
			return this.focused;
		}
		
		public void setListener(IGuiEventListener element_1) {
			this.focused = element_1;
		}
	}
}

