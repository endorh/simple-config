package endorh.simpleconfig.ui.api;

import endorh.simpleconfig.ui.impl.ScissorsHandlerImpl;
import endorh.simpleconfig.api.ui.math.Rectangle;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.ApiStatus.Internal;

import java.util.Collection;

@OnlyIn(value = Dist.CLIENT)
public interface ScissorsHandler {
	ScissorsHandler INSTANCE = ScissorsHandlerImpl.INSTANCE;
	
	@Internal void clearScissors();
	Collection<Rectangle> getScissorsAreas();
	
	void pushScissor(Rectangle clipArea);
	void popScissor();
	
	default void withScissor(Rectangle clipArea, Runnable runnable) {
		pushScissor(clipArea);
		runnable.run();
		popScissor();
	}
	
	void withoutScissors(Runnable runnable);
}

