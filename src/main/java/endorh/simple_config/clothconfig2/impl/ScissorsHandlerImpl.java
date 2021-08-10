package endorh.simple_config.clothconfig2.impl;

import com.google.common.collect.Lists;
import endorh.simple_config.clothconfig2.api.ScissorsHandler;
import endorh.simple_config.clothconfig2.api.ScissorsScreen;
import endorh.simple_config.clothconfig2.math.Rectangle;
import net.minecraft.client.MainWindow;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.lwjgl.opengl.GL11;

import java.util.Collections;
import java.util.List;

@OnlyIn(Dist.CLIENT)
@Internal
public final class ScissorsHandlerImpl implements ScissorsHandler {
   @Internal
   public static final ScissorsHandler INSTANCE = new ScissorsHandlerImpl();
   private final List<Rectangle> scissorsAreas = Lists.newArrayList();

   public void clearScissors() {
      this.scissorsAreas.clear();
      this.applyScissors();
   }

   public List<Rectangle> getScissorsAreas() {
      return Collections.unmodifiableList(this.scissorsAreas);
   }

   public void scissor(Rectangle rectangle) {
      this.scissorsAreas.add(rectangle);
      this.applyScissors();
   }

   public void removeLastScissor() {
      if (!this.scissorsAreas.isEmpty()) {
         this.scissorsAreas.remove(this.scissorsAreas.size() - 1);
      }

      this.applyScissors();
   }

   public void applyScissors() {
      if (!this.scissorsAreas.isEmpty()) {
         Rectangle r = this.scissorsAreas.get(0).clone();

         for(int i = 1; i < this.scissorsAreas.size(); ++i) {
            Rectangle r1 = this.scissorsAreas.get(i);
            if (!r.intersects(r1)) {
               if (Minecraft.getInstance().currentScreen instanceof ScissorsScreen) {
                  this._applyScissor(((ScissorsScreen)Minecraft.getInstance().currentScreen).handleScissor(new Rectangle()));
               } else {
                  this._applyScissor(new Rectangle());
               }

               return;
            }

            r.setBounds(r.intersection(r1));
         }

         r.setBounds(Math.min(r.x, r.x + r.width), Math.min(r.y, r.y + r.height), Math.abs(r.width), Math.abs(r.height));
         if (Minecraft.getInstance().currentScreen instanceof ScissorsScreen) {
            this._applyScissor(((ScissorsScreen)Minecraft.getInstance().currentScreen).handleScissor(r));
         } else {
            this._applyScissor(r);
         }
      } else if (Minecraft.getInstance().currentScreen instanceof ScissorsScreen) {
         this._applyScissor(((ScissorsScreen)Minecraft.getInstance().currentScreen).handleScissor(
           null));
      } else {
         this._applyScissor(null);
      }

   }

   public void _applyScissor(Rectangle r) {
      if (r != null) {
         GL11.glEnable(3089);
         if (r.isEmpty()) {
            GL11.glScissor(0, 0, 0, 0);
         } else {
            MainWindow window = Minecraft.getInstance().getMainWindow();
            double scaleFactor = window.getGuiScaleFactor();
            GL11.glScissor((int)((double)r.x * scaleFactor), (int)((double)(window.getScaledHeight() - r.height - r.y) * scaleFactor), (int)((double)r.width * scaleFactor), (int)((double)r.height * scaleFactor));
         }
      } else {
         GL11.glDisable(3089);
      }

   }
}
