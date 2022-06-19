package endorh.simpleconfig.clothconfig2.api;

import endorh.simpleconfig.clothconfig2.math.Rectangle;
import org.jetbrains.annotations.ApiStatus.Experimental;
import org.jetbrains.annotations.Nullable;

@Experimental
public interface ScissorsScreen {
	@Nullable Rectangle handleScissor(@Nullable Rectangle area);
}

