package endorh.simple_config.clothconfig2.gui.widget;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.IGuiEventListener;
import net.minecraft.client.gui.INestedGuiEventHandler;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public abstract class DynamicElementListWidget<E extends DynamicElementListWidget.ElementEntry<E>> extends DynamicNewSmoothScrollingEntryListWidget<E> {
   public DynamicElementListWidget(Minecraft client, int width, int height, int top, int bottom, ResourceLocation backgroundLocation) {
      super(client, width, height, top, bottom, backgroundLocation);
   }

   public boolean changeFocus(boolean boolean_1) {
      boolean boolean_2 = super.changeFocus(boolean_1);
      if (boolean_2) {
         this.ensureVisible(this.getFocused());
      }

      return boolean_2;
   }

   protected boolean isSelected(int int_1) {
      return false;
   }

   @OnlyIn(Dist.CLIENT)
   public abstract static class ElementEntry<E extends DynamicElementListWidget.ElementEntry<E>> extends DynamicEntryListWidget.Entry<E> implements INestedGuiEventHandler {
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
