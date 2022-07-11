package endorh.simpleconfig.ui.api;

import endorh.simpleconfig.ui.math.Rectangle;
import org.jetbrains.annotations.ApiStatus.Experimental;
import org.jetbrains.annotations.Nullable;

@Experimental
public interface ScissorsScreen {
	@Nullable Rectangle handleScissor(@Nullable Rectangle area);
}

