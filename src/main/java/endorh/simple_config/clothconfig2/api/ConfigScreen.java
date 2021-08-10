package endorh.simple_config.clothconfig2.api;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public interface ConfigScreen {
   void setSavingRunnable(@Nullable Runnable var1);

   void setAfterInitConsumer(@Nullable Consumer<Screen> var1);

   ResourceLocation getBackgroundLocation();

   boolean isRequiresRestart();

   boolean isEdited();

   /** @deprecated */
   @Deprecated
   void setEdited(boolean var1);

   /** @deprecated */
   @Deprecated
   void setEdited(boolean var1, boolean var2);

   void saveAll(boolean var1);

   void addTooltip(Tooltip var1);
}
