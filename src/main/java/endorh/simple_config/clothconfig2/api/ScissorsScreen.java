package endorh.simple_config.clothconfig2.api;

import endorh.simple_config.clothconfig2.math.Rectangle;
import org.jetbrains.annotations.ApiStatus.Experimental;
import org.jetbrains.annotations.Nullable;

@Experimental
public interface ScissorsScreen {
   @Nullable
   Rectangle handleScissor(@Nullable Rectangle var1);
}
