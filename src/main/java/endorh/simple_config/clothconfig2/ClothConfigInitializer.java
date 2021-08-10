package endorh.simple_config.clothconfig2;

import endorh.simple_config.clothconfig2.api.ScrollingContainer;
import endorh.simple_config.clothconfig2.impl.EasingMethod;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.ApiStatus.ScheduledForRemoval;

@OnlyIn(Dist.CLIENT)
public class ClothConfigInitializer {
   public static final Logger LOGGER = LogManager.getFormatterLogger("ClothConfig");
   public static final String MOD_ID = "cloth-config";

   /** @deprecated */
   @Deprecated
   @ScheduledForRemoval
   public static double handleScrollingPosition(double[] target, double scroll, double maxScroll, float delta, double start, double duration) {
      return ScrollingContainer.handleScrollingPosition(target, scroll, maxScroll, delta, start, duration);
   }

   /** @deprecated */
   @Deprecated
   @ScheduledForRemoval
   public static double expoEase(double start, double end, double amount) {
      return ScrollingContainer.ease(start, end, amount, getEasingMethod());
   }

   /** @deprecated */
   @Deprecated
   @ScheduledForRemoval
   public static double clamp(double v, double maxScroll) {
      return ScrollingContainer.clampExtension(v, maxScroll);
   }

   /** @deprecated */
   @Deprecated
   @ScheduledForRemoval
   public static double clamp(double v, double maxScroll, double clampExtension) {
      return ScrollingContainer.clampExtension(v, -clampExtension, maxScroll + clampExtension);
   }

   public static EasingMethod getEasingMethod() {
      return EasingMethod.EasingMethodImpl.NONE;
   }

   public static long getScrollDuration() {
      return 0L;
   }

   public static double getScrollStep() {
      return 16.0D;
   }

   public static double getBounceBackMultiplier() {
      return -10.0D;
   }
}
