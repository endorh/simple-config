package endorh.simple_config.clothconfig2.api;

import com.mojang.blaze3d.systems.RenderSystem;
import endorh.simple_config.clothconfig2.ClothConfigInitializer;
import endorh.simple_config.clothconfig2.impl.EasingMethod;
import endorh.simple_config.clothconfig2.math.Rectangle;
import endorh.simple_config.clothconfig2.math.impl.PointHelper;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.math.MathHelper;

public abstract class ScrollingContainer {
   public double scrollAmount;
   public double scrollTarget;
   public long start;
   public long duration;
   public boolean draggingScrollBar = false;

   public abstract Rectangle getBounds();

   public Rectangle getScissorBounds() {
      Rectangle bounds = this.getBounds();
      return this.hasScrollBar() ? new Rectangle(bounds.x, bounds.y, bounds.width - 6, bounds.height) : bounds;
   }

   public int getScrollBarX() {
      return this.hasScrollBar() ? this.getBounds().getMaxX() - 6 : this.getBounds().getMaxX();
   }

   public boolean hasScrollBar() {
      return this.getMaxScrollHeight() > this.getBounds().height;
   }

   public abstract int getMaxScrollHeight();

   public final int getMaxScroll() {
      return Math.max(0, this.getMaxScrollHeight() - this.getBounds().height);
   }

   public final double clamp(double v) {
      return this.clamp(v, 200.0D);
   }

   public final double clamp(double v, double clampExtension) {
      return MathHelper.clamp(v, -clampExtension, (double)this.getMaxScroll() + clampExtension);
   }

   public final void offset(double value, boolean animated) {
      this.scrollTo(this.scrollTarget + value, animated);
   }

   public final void scrollTo(double value, boolean animated) {
      this.scrollTo(value, animated, ClothConfigInitializer.getScrollDuration());
   }

   public final void scrollTo(double value, boolean animated, long duration) {
      this.scrollTarget = this.clamp(value);
      if (animated) {
         this.start = System.currentTimeMillis();
         this.duration = duration;
      } else {
         this.scrollAmount = this.scrollTarget;
      }

   }

   public void updatePosition(float delta) {
      double[] target = new double[]{this.scrollTarget};
      this.scrollAmount = handleScrollingPosition(target, this.scrollAmount, this.getMaxScroll(), delta, (double)this.start, (double)this.duration);
      this.scrollTarget = target[0];
   }

   public static double handleScrollingPosition(double[] target, double scroll, double maxScroll, float delta, double start, double duration) {
      return handleScrollingPosition(target, scroll, maxScroll, delta, start, duration, ClothConfigInitializer.getBounceBackMultiplier(), ClothConfigInitializer.getEasingMethod());
   }

   public static double handleScrollingPosition(double[] target, double scroll, double maxScroll, float delta, double start, double duration, double bounceBackMultiplier, EasingMethod easingMethod) {
      if (bounceBackMultiplier >= 0.0D) {
         target[0] = clampExtension(target[0], maxScroll);
         if (target[0] < 0.0D) {
            target[0] -= target[0] * (1.0D - bounceBackMultiplier) * (double)delta / 3.0D;
         } else if (target[0] > maxScroll) {
            target[0] = (target[0] - maxScroll) * (1.0D - (1.0D - bounceBackMultiplier) * (double)delta / 3.0D) + maxScroll;
         }
      } else {
         target[0] = clampExtension(target[0], maxScroll, 0.0D);
      }

      return ease(scroll, target[0], Math.min(((double)System.currentTimeMillis() - start) / duration * (double)delta * 3.0D, 1.0D), easingMethod);
   }

   public static double ease(double start, double end, double amount, EasingMethod easingMethod) {
      return start + (end - start) * easingMethod.apply(amount);
   }

   public static double clampExtension(double value, double maxScroll) {
      return clampExtension(value, maxScroll, 200.0D);
   }

   public static double clampExtension(double v, double maxScroll, double clampExtension) {
      return MathHelper.clamp(v, -clampExtension, maxScroll + clampExtension);
   }

   public void renderScrollBar() {
      this.renderScrollBar(0, 1.0F, 1.0F);
   }

   public void renderScrollBar(int background, float alpha, float scrollBarAlphaOffset) {
      if (this.hasScrollBar()) {
         Rectangle bounds = this.getBounds();
         int maxScroll = this.getMaxScroll();
         int height = bounds.height * bounds.height / this.getMaxScrollHeight();
         height = MathHelper.clamp(height, 32, bounds.height);
         height = (int)((double)height - Math.min(this.scrollAmount < 0.0D ? (int)(-this.scrollAmount) : (this.scrollAmount > (double)maxScroll ? (int)this.scrollAmount - maxScroll : 0), (double)height * 0.95D));
         height = Math.max(10, height);
         int minY = Math.min(Math.max((int)this.scrollAmount * (bounds.height - height) / maxScroll + bounds.y, bounds.y), bounds.getMaxY() - height);
         int scrollbarPositionMinX = this.getScrollBarX();
         int scrollbarPositionMaxX = scrollbarPositionMinX + 6;
         boolean hovered = (new Rectangle(scrollbarPositionMinX, minY, scrollbarPositionMaxX - scrollbarPositionMinX, height)).contains(PointHelper.ofMouse());
         float bottomC = (hovered ? 0.67F : 0.5F) * scrollBarAlphaOffset;
         float topC = (hovered ? 0.87F : 0.67F) * scrollBarAlphaOffset;
         RenderSystem.disableTexture();
         RenderSystem.enableBlend();
         RenderSystem.disableAlphaTest();
         RenderSystem.blendFuncSeparate(770, 771, 1, 0);
         RenderSystem.shadeModel(7425);
         Tessellator tessellator = Tessellator.getInstance();
         BufferBuilder buffer = tessellator.getBuffer();
         float a = (float)(background >> 24 & 255) / 255.0F;
         float r = (float)(background >> 16 & 255) / 255.0F;
         float g = (float)(background >> 8 & 255) / 255.0F;
         float b = (float)(background & 255) / 255.0F;
         buffer.begin(7, DefaultVertexFormats.POSITION_COLOR);
         buffer.pos(scrollbarPositionMinX, bounds.getMaxY(), 0.0D).color(r, g, b, a).endVertex();
         buffer.pos(scrollbarPositionMaxX, bounds.getMaxY(), 0.0D).color(r, g, b, a).endVertex();
         buffer.pos(scrollbarPositionMaxX, bounds.y, 0.0D).color(r, g, b, a).endVertex();
         buffer.pos(scrollbarPositionMinX, bounds.y, 0.0D).color(r, g, b, a).endVertex();
         tessellator.draw();
         buffer.begin(7, DefaultVertexFormats.POSITION_COLOR);
         buffer.pos(scrollbarPositionMinX, minY + height, 0.0D).color(bottomC, bottomC, bottomC, alpha).endVertex();
         buffer.pos(scrollbarPositionMaxX, minY + height, 0.0D).color(bottomC, bottomC, bottomC, alpha).endVertex();
         buffer.pos(scrollbarPositionMaxX, minY, 0.0D).color(bottomC, bottomC, bottomC, alpha).endVertex();
         buffer.pos(scrollbarPositionMinX, minY, 0.0D).color(bottomC, bottomC, bottomC, alpha).endVertex();
         tessellator.draw();
         buffer.begin(7, DefaultVertexFormats.POSITION_COLOR);
         buffer.pos(scrollbarPositionMinX, minY + height - 1, 0.0D).color(topC, topC, topC, alpha).endVertex();
         buffer.pos(scrollbarPositionMaxX - 1, minY + height - 1, 0.0D).color(topC, topC, topC, alpha).endVertex();
         buffer.pos(scrollbarPositionMaxX - 1, minY, 0.0D).color(topC, topC, topC, alpha).endVertex();
         buffer.pos(scrollbarPositionMinX, minY, 0.0D).color(topC, topC, topC, alpha).endVertex();
         tessellator.draw();
         RenderSystem.shadeModel(7424);
         RenderSystem.disableBlend();
         RenderSystem.enableAlphaTest();
         RenderSystem.enableTexture();
      }

   }

   public boolean mouseDragged(double mouseX, double mouseY, int button, double dx, double dy) {
      return this.mouseDragged(mouseX, mouseY, button, dx, dy, false, 0.0D);
   }

   public boolean mouseDragged(double mouseX, double mouseY, int button, double dx, double dy, boolean snapToRows, double rowSize) {
      if (button == 0 && this.draggingScrollBar) {
         float height = (float)this.getMaxScrollHeight();
         Rectangle bounds = this.getBounds();
         int actualHeight = bounds.height;
         if (mouseY >= (double)bounds.y && mouseY <= (double)bounds.getMaxY()) {
            double maxScroll = Math.max(1, this.getMaxScroll());
            double int_3 = MathHelper.clamp((double)(actualHeight * actualHeight) / (double)height, 32.0D,
                                            actualHeight - 8);
            double double_6 = Math.max(1.0D, maxScroll / ((double)actualHeight - int_3));
            float to = MathHelper.clamp((float)(this.scrollAmount + dy * double_6), 0.0F, (float)this.getMaxScroll());
            if (snapToRows) {
               double nearestRow = (double)Math.round((double)to / rowSize) * rowSize;
               this.scrollTo(nearestRow, false);
            } else {
               this.scrollTo(to, false);
            }
         }

         return true;
      } else {
         return false;
      }
   }

   public boolean updateDraggingState(double mouseX, double mouseY, int button) {
      if (!this.hasScrollBar()) {
         return false;
      } else {
         double height = this.getMaxScroll();
         Rectangle bounds = this.getBounds();
         int actualHeight = bounds.height;
         if (height > (double)actualHeight && mouseY >= (double)bounds.y && mouseY <= (double)bounds.getMaxY()) {
            double scrollbarPositionMinX = this.getScrollBarX();
            if (mouseX >= scrollbarPositionMinX - 1.0D & mouseX <= scrollbarPositionMinX + 8.0D) {
               this.draggingScrollBar = true;
               return true;
            }
         }

         this.draggingScrollBar = false;
         return false;
      }
   }
}
