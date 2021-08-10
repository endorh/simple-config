package endorh.simple_config.clothconfig2.gui.widget;

import com.mojang.blaze3d.matrix.MatrixStack;
import endorh.simple_config.clothconfig2.ClothConfigInitializer;
import endorh.simple_config.clothconfig2.api.ScrollingContainer;
import endorh.simple_config.clothconfig2.math.Rectangle;
import endorh.simple_config.clothconfig2.math.impl.PointHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.Iterator;

@OnlyIn(Dist.CLIENT)
public abstract class DynamicNewSmoothScrollingEntryListWidget<E extends DynamicEntryListWidget.Entry<E>> extends DynamicEntryListWidget<E> {
   protected double target;
   protected boolean smoothScrolling = true;
   protected long start;
   protected long duration;

   public DynamicNewSmoothScrollingEntryListWidget(Minecraft client, int width, int height, int top, int bottom, ResourceLocation backgroundLocation) {
      super(client, width, height, top, bottom, backgroundLocation);
   }

   public boolean isSmoothScrolling() {
      return this.smoothScrolling;
   }

   public void setSmoothScrolling(boolean smoothScrolling) {
      this.smoothScrolling = smoothScrolling;
   }

   public void capYPosition(double double_1) {
      if (!this.smoothScrolling) {
         this.scroll = MathHelper.clamp(double_1, 0.0D, this.getMaxScroll());
      } else {
         this.scroll = ScrollingContainer.clampExtension(double_1, this.getMaxScroll());
         this.target = ScrollingContainer.clampExtension(double_1, this.getMaxScroll());
      }

   }

   public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
      if (!this.smoothScrolling) {
         return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
      } else if (this.getFocused() != null && this.isDragging() && button == 0 && this.getFocused().mouseDragged(mouseX, mouseY, button, deltaX, deltaY)) {
         return true;
      } else if (button == 0 && this.scrolling) {
         if (mouseY < (double)this.top) {
            this.capYPosition(0.0D);
         } else if (mouseY > (double)this.bottom) {
            this.capYPosition(this.getMaxScroll());
         } else {
            double double_5 = Math.max(1, this.getMaxScroll());
            int int_2 = this.bottom - this.top;
            int int_3 = MathHelper.clamp((int)((float)(int_2 * int_2) / (float)this.getMaxScrollPosition()), 32, int_2 - 8);
            double double_6 = Math.max(1.0D, double_5 / (double)(int_2 - int_3));
            this.capYPosition(MathHelper.clamp(this.getScroll() + deltaY * double_6, 0.0D,
                                               this.getMaxScroll()));
         }

         return true;
      } else {
         return false;
      }
   }

   public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
      Iterator var7 = this.getEventListeners().iterator();

      DynamicEntryListWidget.Entry entry;
      do {
         if (!var7.hasNext()) {
            if (!this.smoothScrolling) {
               this.scroll += 16.0D * -amount;
               this.scroll = MathHelper.clamp(amount, 0.0D, this.getMaxScroll());
               return true;
            }

            this.offset(ClothConfigInitializer.getScrollStep() * -amount, true);
            return true;
         }

         entry = (DynamicEntryListWidget.Entry)var7.next();
      } while(!entry.mouseScrolled(mouseX, mouseY, amount));

      return true;
   }

   public void offset(double value, boolean animated) {
      this.scrollTo(this.target + value, animated);
   }

   public void scrollTo(double value, boolean animated) {
      this.scrollTo(value, animated, ClothConfigInitializer.getScrollDuration());
   }

   public void scrollTo(double value, boolean animated, long duration) {
      this.target = ScrollingContainer.clampExtension(value, this.getMaxScroll());
      if (animated) {
         this.start = System.currentTimeMillis();
         this.duration = duration;
      } else {
         this.scroll = this.target;
      }

   }

   public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
      double[] target = new double[]{this.target};
      this.scroll = ScrollingContainer.handleScrollingPosition(target, this.scroll,
                                                               this.getMaxScroll(), delta, (double)this.start, (double)this.duration);
      this.target = target[0];
      super.render(matrices, mouseX, mouseY, delta);
   }

   protected void renderScrollBar(MatrixStack matrices, Tessellator tessellator, BufferBuilder buffer, int maxScroll, int scrollbarPositionMinX, int scrollbarPositionMaxX) {
      if (!this.smoothScrolling) {
         super.renderScrollBar(matrices, tessellator, buffer, maxScroll, scrollbarPositionMinX, scrollbarPositionMaxX);
      } else if (maxScroll > 0) {
         int height = (this.bottom - this.top) * (this.bottom - this.top) / this.getMaxScrollPosition();
         height = MathHelper.clamp(height, 32, this.bottom - this.top - 8);
         height = (int)((double)height - Math.min(this.scroll < 0.0D ? (int)(-this.scroll) : (this.scroll > (double)this.getMaxScroll() ? (int)this.scroll - this.getMaxScroll() : 0), (double)height * 0.95D));
         height = Math.max(10, height);
         int minY = Math.min(Math.max((int)this.getScroll() * (this.bottom - this.top - height) / maxScroll + this.top, this.top), this.bottom - height);
         int bottomc = (new Rectangle(scrollbarPositionMinX, minY, scrollbarPositionMaxX - scrollbarPositionMinX, height)).contains(PointHelper.ofMouse()) ? 168 : 128;
         int topc = (new Rectangle(scrollbarPositionMinX, minY, scrollbarPositionMaxX - scrollbarPositionMinX, height)).contains(PointHelper.ofMouse()) ? 222 : 172;
         Matrix4f matrix = matrices.getLast().getMatrix();
         buffer.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
         buffer.pos(matrix, (float)scrollbarPositionMinX, (float)this.bottom, 0.0F).tex(0.0F, 1.0F).color(0, 0, 0, 255).endVertex();
         buffer.pos(matrix, (float)scrollbarPositionMaxX, (float)this.bottom, 0.0F).tex(1.0F, 1.0F).color(0, 0, 0, 255).endVertex();
         buffer.pos(matrix, (float)scrollbarPositionMaxX, (float)this.top, 0.0F).tex(1.0F, 0.0F).color(0, 0, 0, 255).endVertex();
         buffer.pos(matrix, (float)scrollbarPositionMinX, (float)this.top, 0.0F).tex(0.0F, 0.0F).color(0, 0, 0, 255).endVertex();
         tessellator.draw();
         buffer.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
         buffer.pos(matrix, (float)scrollbarPositionMinX, (float)(minY + height), 0.0F).tex(0.0F, 1.0F).color(bottomc, bottomc, bottomc, 255).endVertex();
         buffer.pos(matrix, (float)scrollbarPositionMaxX, (float)(minY + height), 0.0F).tex(1.0F, 1.0F).color(bottomc, bottomc, bottomc, 255).endVertex();
         buffer.pos(matrix, (float)scrollbarPositionMaxX, (float)minY, 0.0F).tex(1.0F, 0.0F).color(bottomc, bottomc, bottomc, 255).endVertex();
         buffer.pos(matrix, (float)scrollbarPositionMinX, (float)minY, 0.0F).tex(0.0F, 0.0F).color(bottomc, bottomc, bottomc, 255).endVertex();
         tessellator.draw();
         buffer.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
         buffer.pos(matrix, (float)scrollbarPositionMinX, (float)(minY + height - 1), 0.0F).tex(0.0F, 1.0F).color(topc, topc, topc, 255).endVertex();
         buffer.pos(matrix, (float)(scrollbarPositionMaxX - 1), (float)(minY + height - 1), 0.0F).tex(1.0F, 1.0F).color(topc, topc, topc, 255).endVertex();
         buffer.pos(matrix, (float)(scrollbarPositionMaxX - 1), (float)minY, 0.0F).tex(1.0F, 0.0F).color(topc, topc, topc, 255).endVertex();
         buffer.pos(matrix, (float)scrollbarPositionMinX, (float)minY, 0.0F).tex(0.0F, 0.0F).color(topc, topc, topc, 255).endVertex();
         tessellator.draw();
      }

   }

   public static class Precision {
      public static final float FLOAT_EPSILON = 0.001F;
      public static final double DOUBLE_EPSILON = 1.0E-7D;

      public static boolean almostEquals(float value1, float value2, float acceptableDifference) {
         return Math.abs(value1 - value2) <= acceptableDifference;
      }

      public static boolean almostEquals(double value1, double value2, double acceptableDifference) {
         return Math.abs(value1 - value2) <= acceptableDifference;
      }
   }

   public static class Interpolation {
      public static double expoEase(double start, double end, double amount) {
         return start + (end - start) * ClothConfigInitializer.getEasingMethod().apply(amount);
      }
   }
}
