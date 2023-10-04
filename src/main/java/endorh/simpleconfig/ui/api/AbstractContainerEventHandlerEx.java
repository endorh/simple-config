package endorh.simpleconfig.ui.api;

import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;


@OnlyIn(Dist.CLIENT)
public abstract class AbstractContainerEventHandlerEx extends GuiComponent implements ContainerEventHandlerEx {
   private @Nullable GuiEventListener focused;
   private @Nullable Pair<Integer, GuiEventListener> dragged;

   // Drag
   @Override public final boolean isDragging() {
      return dragged != null;
   }
   @Override public final void setDragging(boolean dragging) {
      dragged = dragging? Pair.of(0, focused) : null;
   }

   @Override public @Nullable Pair<Integer, GuiEventListener> getDragged() {
      return dragged;
   }
   @Override public void setDragged(Pair<Integer, GuiEventListener> dragged) {
      this.dragged = dragged;
   }

   // Focus
   @Override public @Nullable GuiEventListener getFocused() {
      return focused;
   }
   @Override public void setFocused(@Nullable GuiEventListener l) {
      if (focused != null) focused.setFocused(false);
      if (l != null) l.setFocused(true);
      focused = l;
   }
}
