package endorh.simpleconfig.ui.api;

import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.components.events.GuiEventListener;
import org.jetbrains.annotations.NotNull;

public record RedirectPath(GuiEventListener redirect, ComponentPath childPath) implements ComponentPath {
   public static ComponentPath redirect(GuiEventListener redirect, ComponentPath path) {
      return new RedirectPath(redirect, path);
   }

   @Override public @NotNull GuiEventListener component() {
      return redirect;
   }

   @Override public void applyFocus(boolean focused) {
      redirect.setFocused(focused);
      childPath.applyFocus(focused);
   }
}
