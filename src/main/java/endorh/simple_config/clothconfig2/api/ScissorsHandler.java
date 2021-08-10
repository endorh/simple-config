package endorh.simple_config.clothconfig2.api;

import endorh.simple_config.clothconfig2.impl.ScissorsHandlerImpl;
import endorh.simple_config.clothconfig2.math.Rectangle;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.List;

@OnlyIn(Dist.CLIENT)
public interface ScissorsHandler {
   ScissorsHandler INSTANCE = ScissorsHandlerImpl.INSTANCE;

   void clearScissors();

   List<Rectangle> getScissorsAreas();

   void scissor(Rectangle var1);

   void removeLastScissor();

   void applyScissors();
}
