package endorh.simpleconfig.ui.api;

import endorh.simpleconfig.ui.impl.ScissorsHandlerImpl;
import endorh.simpleconfig.ui.math.Rectangle;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.List;

@OnlyIn(value = Dist.CLIENT)
public interface ScissorsHandler {
	ScissorsHandler INSTANCE = ScissorsHandlerImpl.INSTANCE;
	
	void clearScissors();
	
	List<Rectangle> getScissorsAreas();
	
	void scissor(Rectangle var1);
	
	void removeLastScissor();
	
	void applyScissors();
}

