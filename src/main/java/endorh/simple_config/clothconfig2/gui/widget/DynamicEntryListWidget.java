package endorh.simple_config.clothconfig2.gui.widget;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import endorh.simple_config.clothconfig2.api.ScissorsHandler;
import endorh.simple_config.clothconfig2.math.Rectangle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.gui.FocusableGui;
import net.minecraft.client.gui.IGuiEventListener;
import net.minecraft.client.gui.IRenderable;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

import java.util.*;

@OnlyIn(Dist.CLIENT)
public abstract class DynamicEntryListWidget<E extends DynamicEntryListWidget.Entry<E>> extends FocusableGui implements IRenderable {
   protected static final int DRAG_OUTSIDE = -2;
   protected final Minecraft client;
   private final List<E> entries = new DynamicEntryListWidget.Entries();
   public int width;
   public int height;
   public int top;
   public int bottom;
   public int right;
   public int left;
   protected boolean verticallyCenter = true;
   protected int yDrag = -2;
   protected boolean selectionVisible = true;
   protected boolean renderSelection;
   protected int headerHeight;
   protected double scroll;
   protected boolean scrolling;
   protected E selectedItem;
   protected ResourceLocation backgroundLocation;

   public DynamicEntryListWidget(Minecraft client, int width, int height, int top, int bottom, ResourceLocation backgroundLocation) {
      this.client = client;
      this.width = width;
      this.height = height;
      this.top = top;
      this.bottom = bottom;
      this.left = 0;
      this.right = width;
      this.backgroundLocation = backgroundLocation;
   }

   public void setRenderSelection(boolean boolean_1) {
      this.selectionVisible = boolean_1;
   }

   protected void setRenderHeader(boolean boolean_1, int headerHeight) {
      this.renderSelection = boolean_1;
      this.headerHeight = headerHeight;
      if (!boolean_1) {
         this.headerHeight = 0;
      }

   }

   public int getItemWidth() {
      return 220;
   }

   public E getSelectedItem() {
      return this.selectedItem;
   }

   public void selectItem(E item) {
      this.selectedItem = item;
   }

   public E getFocused() {
      //noinspection unchecked
      return (E) super.getListener();
   }

   public final @NotNull List<E> getEventListeners() {
      return this.entries;
   }

   protected final void clearItems() {
      this.entries.clear();
   }

   protected E getItem(int index) {
      return this.getEventListeners().get(index);
   }

   protected int addItem(E item) {
      this.entries.add(item);
      return this.entries.size() - 1;
   }

   protected int getItemCount() {
      return this.getEventListeners().size();
   }

   protected boolean isSelected(int index) {
      return Objects.equals(this.getSelectedItem(), this.getEventListeners().get(index));
   }

   protected final E getItemAtPosition(double mouseX, double mouseY) {
      int listMiddleX = this.left + this.width / 2;
      int minX = listMiddleX - this.getItemWidth() / 2;
      int maxX = listMiddleX + this.getItemWidth() / 2;
      int currentY = MathHelper.floor(mouseY - (double)this.top) - this.headerHeight + (int)this.getScroll() - 4;
      int itemY = 0;
      int itemIndex = -1;

      for(int i = 0; i < this.entries.size(); ++i) {
         E item = this.getItem(i);
         itemY += item.getItemHeight();
         if (itemY > currentY) {
            itemIndex = i;
            break;
         }
      }

      return mouseX < (double)this.getScrollbarPosition() && mouseX >= (double)minX && mouseX <= (double)maxX && itemIndex >= 0 && currentY >= 0 && itemIndex < this.getItemCount() ? this.getEventListeners().get(itemIndex)
                                                                                                                                                                                    : null;
   }

   public void updateSize(int width, int height, int top, int bottom) {
      this.width = width;
      this.height = height;
      this.top = top;
      this.bottom = bottom;
      this.left = 0;
      this.right = width;
   }

   public void setLeftPos(int left) {
      this.left = left;
      this.right = left + this.width;
   }

   protected int getMaxScrollPosition() {
      List<Integer> list = new ArrayList();
      int i = this.headerHeight;
   
      for (E e : this.entries) {
         i += e.getItemHeight();
         if (e.getMorePossibleHeight() >= 0) {
            list.add(i + e.getMorePossibleHeight());
         }
      }

      list.add(i);
      return list.stream().max(Integer::compare).orElse(0);
   }

   protected void clickedHeader(int int_1, int int_2) {
   }

   protected void renderHeader(MatrixStack matrices, int rowLeft, int startY, Tessellator tessellator) {
   }

   protected void drawBackground() {
   }

   protected void renderDecorations(MatrixStack matrices, int mouseX, int mouseY) {
   }

   /** @deprecated */
   @Deprecated
   protected void renderBackBackground(MatrixStack matrices, BufferBuilder buffer, Tessellator tessellator) {
      this.client.getTextureManager().bindTexture(this.backgroundLocation);
      RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
      Matrix4f matrix = matrices.getLast().getMatrix();
      float float_2 = 32.0F;
      buffer.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
      buffer.pos(matrix, (float)this.left, (float)this.bottom, 0.0F).tex((float)this.left / 32.0F, (float)(this.bottom + (int)this.getScroll()) / 32.0F).color(32, 32, 32, 255).endVertex();
      buffer.pos(matrix, (float)this.right, (float)this.bottom, 0.0F).tex((float)this.right / 32.0F, (float)(this.bottom + (int)this.getScroll()) / 32.0F).color(32, 32, 32, 255).endVertex();
      buffer.pos(matrix, (float)this.right, (float)this.top, 0.0F).tex((float)this.right / 32.0F, (float)(this.top + (int)this.getScroll()) / 32.0F).color(32, 32, 32, 255).endVertex();
      buffer.pos(matrix, (float)this.left, (float)this.top, 0.0F).tex((float)this.left / 32.0F, (float)(this.top + (int)this.getScroll()) / 32.0F).color(32, 32, 32, 255).endVertex();
      tessellator.draw();
   }

   public void render(@NotNull MatrixStack matrices, int mouseX, int mouseY, float delta) {
      this.drawBackground();
      int scrollbarPosition = this.getScrollbarPosition();
      int int_4 = scrollbarPosition + 6;
      RenderSystem.disableLighting();
      RenderSystem.disableFog();
      Tessellator tessellator = Tessellator.getInstance();
      BufferBuilder buffer = tessellator.getBuffer();
      this.renderBackBackground(matrices, buffer, tessellator);
      int rowLeft = this.getRowLeft();
      int startY = this.top + 4 - (int)this.getScroll();
      if (this.renderSelection) {
         this.renderHeader(matrices, rowLeft, startY, tessellator);
      }

      ScissorsHandler.INSTANCE.scissor(new Rectangle(this.left, this.top, this.width, this.bottom - this.top));
      this.renderList(matrices, rowLeft, startY, mouseX, mouseY, delta);
      ScissorsHandler.INSTANCE.removeLastScissor();
      RenderSystem.disableDepthTest();
      this.renderHoleBackground(matrices, 0, this.top, 255, 255);
      this.renderHoleBackground(matrices, this.bottom, this.height, 255, 255);
      RenderSystem.enableBlend();
      RenderSystem.blendFuncSeparate(770, 771, 0, 1);
      RenderSystem.disableAlphaTest();
      RenderSystem.shadeModel(7425);
      RenderSystem.disableTexture();
      Matrix4f matrix = matrices.getLast().getMatrix();
      buffer.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
      buffer.pos(matrix, (float)this.left, (float)(this.top + 4), 0.0F).tex(0.0F, 1.0F).color(0, 0, 0, 0).endVertex();
      buffer.pos(matrix, (float)this.right, (float)(this.top + 4), 0.0F).tex(1.0F, 1.0F).color(0, 0, 0, 0).endVertex();
      buffer.pos(matrix, (float)this.right, (float)this.top, 0.0F).tex(1.0F, 0.0F).color(0, 0, 0, 255).endVertex();
      buffer.pos(matrix, (float)this.left, (float)this.top, 0.0F).tex(0.0F, 0.0F).color(0, 0, 0, 255).endVertex();
      tessellator.draw();
      buffer.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
      buffer.pos(matrix, (float)this.left, (float)this.bottom, 0.0F).tex(0.0F, 1.0F).color(0, 0, 0, 255).endVertex();
      buffer.pos(matrix, (float)this.right, (float)this.bottom, 0.0F).tex(1.0F, 1.0F).color(0, 0, 0, 255).endVertex();
      buffer.pos(matrix, (float)this.right, (float)(this.bottom - 4), 0.0F).tex(1.0F, 0.0F).color(0, 0, 0, 0).endVertex();
      buffer.pos(matrix, (float)this.left, (float)(this.bottom - 4), 0.0F).tex(0.0F, 0.0F).color(0, 0, 0, 0).endVertex();
      tessellator.draw();
      int maxScroll = this.getMaxScroll();
      this.renderScrollBar(matrices, tessellator, buffer, maxScroll, scrollbarPosition, int_4);
      this.renderDecorations(matrices, mouseX, mouseY);
      RenderSystem.enableTexture();
      RenderSystem.shadeModel(7424);
      RenderSystem.enableAlphaTest();
      RenderSystem.disableBlend();
   }

   protected void renderScrollBar(MatrixStack matrices, Tessellator tessellator, BufferBuilder buffer, int maxScroll, int scrollbarPositionMinX, int scrollbarPositionMaxX) {
      if (maxScroll > 0) {
         int int_9 = (this.bottom - this.top) * (this.bottom - this.top) / this.getMaxScrollPosition();
         int_9 = MathHelper.clamp(int_9, 32, this.bottom - this.top - 8);
         int int_10 = (int)this.getScroll() * (this.bottom - this.top - int_9) / maxScroll + this.top;
         if (int_10 < this.top) {
            int_10 = this.top;
         }

         Matrix4f matrix = matrices.getLast().getMatrix();
         buffer.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
         buffer.pos(matrix, (float)scrollbarPositionMinX, (float)this.bottom, 0.0F).tex(0.0F, 1.0F).color(0, 0, 0, 255).endVertex();
         buffer.pos(matrix, (float)scrollbarPositionMaxX, (float)this.bottom, 0.0F).tex(1.0F, 1.0F).color(0, 0, 0, 255).endVertex();
         buffer.pos(matrix, (float)scrollbarPositionMaxX, (float)this.top, 0.0F).tex(1.0F, 0.0F).color(0, 0, 0, 255).endVertex();
         buffer.pos(matrix, (float)scrollbarPositionMinX, (float)this.top, 0.0F).tex(0.0F, 0.0F).color(0, 0, 0, 255).endVertex();
         tessellator.draw();
         buffer.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
         buffer.pos(matrix, (float)scrollbarPositionMinX, (float)(int_10 + int_9), 0.0F).tex(0.0F, 1.0F).color(128, 128, 128, 255).endVertex();
         buffer.pos(matrix, (float)scrollbarPositionMaxX, (float)(int_10 + int_9), 0.0F).tex(1.0F, 1.0F).color(128, 128, 128, 255).endVertex();
         buffer.pos(matrix, (float)scrollbarPositionMaxX, (float)int_10, 0.0F).tex(1.0F, 0.0F).color(128, 128, 128, 255).endVertex();
         buffer.pos(matrix, (float)scrollbarPositionMinX, (float)int_10, 0.0F).tex(0.0F, 0.0F).color(128, 128, 128, 255).endVertex();
         tessellator.draw();
         buffer.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
         buffer.pos(scrollbarPositionMinX, int_10 + int_9 - 1, 0.0D).tex(0.0F, 1.0F).color(192, 192, 192, 255).endVertex();
         buffer.pos(scrollbarPositionMaxX - 1, int_10 + int_9 - 1, 0.0D).tex(1.0F, 1.0F).color(192, 192, 192, 255).endVertex();
         buffer.pos(scrollbarPositionMaxX - 1, int_10, 0.0D).tex(1.0F, 0.0F).color(192, 192, 192, 255).endVertex();
         buffer.pos(scrollbarPositionMinX, int_10, 0.0D).tex(0.0F, 0.0F).color(192, 192, 192, 255).endVertex();
         tessellator.draw();
      }

   }

   protected void centerScrollOn(E item) {
      double d = (double)(this.bottom - this.top) / -2.0D;

      for(int i = 0; i < this.getEventListeners().indexOf(item) && i < this.getItemCount(); ++i) {
         d += this.getItem(i).getItemHeight();
      }

      this.capYPosition(d);
   }

   protected void ensureVisible(E item) {
      int rowTop = this.getRowTop(this.getEventListeners().indexOf(item));
      int int_2 = rowTop - this.top - 4 - item.getItemHeight();
      if (int_2 < 0) {
         this.scroll(int_2);
      }

      int int_3 = this.bottom - rowTop - item.getItemHeight() * 2;
      if (int_3 < 0) {
         this.scroll(-int_3);
      }

   }

   protected void scroll(int int_1) {
      this.capYPosition(this.getScroll() + (double)int_1);
      this.yDrag = -2;
   }

   public double getScroll() {
      return this.scroll;
   }

   public void capYPosition(double double_1) {
      this.scroll = MathHelper.clamp(double_1, 0.0D, this.getMaxScroll());
   }

   protected int getMaxScroll() {
      return Math.max(0, this.getMaxScrollPosition() - (this.bottom - this.top - 4));
   }

   public int getScrollBottom() {
      return (int)this.getScroll() - this.height - this.headerHeight;
   }

   protected void updateScrollingState(double double_1, double double_2, int int_1) {
      this.scrolling = int_1 == 0 && double_1 >= (double)this.getScrollbarPosition() && double_1 < (double)(this.getScrollbarPosition() + 6);
   }

   protected int getScrollbarPosition() {
      return this.width / 2 + 124;
   }

   public boolean mouseClicked(double double_1, double double_2, int int_1) {
      this.updateScrollingState(double_1, double_2, int_1);
      if (!this.isMouseOver(double_1, double_2)) {
         return false;
      } else {
         E item = this.getItemAtPosition(double_1, double_2);
         if (item != null) {
            if (item.mouseClicked(double_1, double_2, int_1)) {
               this.setListener(item);
               this.setDragging(true);
               return true;
            }
         } else if (int_1 == 0) {
            this.clickedHeader((int)(double_1 - (double)(this.left + this.width / 2 - this.getItemWidth() / 2)), (int)(double_2 - (double)this.top) + (int)this.getScroll() - 4);
            return true;
         }

         return this.scrolling;
      }
   }

   public boolean mouseReleased(double double_1, double double_2, int int_1) {
      if (this.getFocused() != null) {
         this.getFocused().mouseReleased(double_1, double_2, int_1);
      }

      return false;
   }

   public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
      if (super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)) {
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
            this.capYPosition(this.getScroll() + deltaY * double_6);
         }

         return true;
      } else {
         return false;
      }
   }

   public boolean mouseScrolled(double double_1, double double_2, double double_3) {
      Iterator var7 = this.entries.iterator();

      DynamicEntryListWidget.Entry entry;
      do {
         if (!var7.hasNext()) {
            this.capYPosition(this.getScroll() - double_3 * (double)(this.getMaxScroll() / this.getItemCount()) / 2.0D);
            return true;
         }

         entry = (DynamicEntryListWidget.Entry)var7.next();
      } while(!entry.mouseScrolled(double_1, double_2, double_3));

      return true;
   }

   public boolean keyPressed(int int_1, int int_2, int int_3) {
      if (super.keyPressed(int_1, int_2, int_3)) {
         return true;
      } else if (int_1 == 264) {
         this.moveSelection(1);
         return true;
      } else if (int_1 == 265) {
         this.moveSelection(-1);
         return true;
      } else {
         return false;
      }
   }

   protected void moveSelection(int int_1) {
      if (!this.getEventListeners().isEmpty()) {
         int int_2 = this.getEventListeners().indexOf(this.getSelectedItem());
         int int_3 = MathHelper.clamp(int_2 + int_1, 0, this.getItemCount() - 1);
         E itemListWidget$Item_1 = this.getEventListeners().get(int_3);
         this.selectItem(itemListWidget$Item_1);
         this.ensureVisible(itemListWidget$Item_1);
      }

   }

   public boolean isMouseOver(double double_1, double double_2) {
      return double_2 >= (double)this.top && double_2 <= (double)this.bottom && double_1 >= (double)this.left && double_1 <= (double)this.right;
   }

   protected void renderList(MatrixStack matrices, int startX, int startY, int int_3, int int_4, float float_1) {
      int itemCount = this.getItemCount();
      Tessellator tessellator = Tessellator.getInstance();
      BufferBuilder buffer = tessellator.getBuffer();

      for(int renderIndex = 0; renderIndex < itemCount; ++renderIndex) {
         E item = this.getItem(renderIndex);
         int itemY = startY + this.headerHeight;

         int itemHeight;
         for(itemHeight = 0; itemHeight < this.entries.size() && itemHeight < renderIndex; ++itemHeight) {
            itemY += this.entries.get(itemHeight).getItemHeight();
         }

         itemHeight = item.getItemHeight() - 4;
         int itemWidth = this.getItemWidth();
         if (this.selectionVisible && this.isSelected(renderIndex)) {
            int itemMinX = this.left + this.width / 2 - itemWidth / 2;
            int itemMaxX = itemMinX + itemWidth;
            RenderSystem.disableTexture();
            float float_2 = this.isFocused() ? 1.0F : 0.5F;
            Matrix4f matrix = matrices.getLast().getMatrix();
            RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
            buffer.begin(7, DefaultVertexFormats.POSITION_COLOR);
            buffer.pos(matrix, (float)itemMinX, (float)(itemY + itemHeight + 2), 0.0F).color(float_2, float_2, float_2, 1.0F).endVertex();
            buffer.pos(matrix, (float)itemMaxX, (float)(itemY + itemHeight + 2), 0.0F).color(float_2, float_2, float_2, 1.0F).endVertex();
            buffer.pos(matrix, (float)itemMaxX, (float)(itemY - 2), 0.0F).color(float_2, float_2, float_2, 1.0F).endVertex();
            buffer.pos(matrix, (float)itemMinX, (float)(itemY - 2), 0.0F).color(float_2, float_2, float_2, 1.0F).endVertex();
            tessellator.draw();
            buffer.begin(7, DefaultVertexFormats.POSITION_COLOR);
            buffer.pos(matrix, (float)(itemMinX + 1), (float)(itemY + itemHeight + 1), 0.0F).color(0.0F, 0.0F, 0.0F, 1.0F).endVertex();
            buffer.pos(matrix, (float)(itemMaxX - 1), (float)(itemY + itemHeight + 1), 0.0F).color(0.0F, 0.0F, 0.0F, 1.0F).endVertex();
            buffer.pos(matrix, (float)(itemMaxX - 1), (float)(itemY - 1), 0.0F).color(0.0F, 0.0F, 0.0F, 1.0F).endVertex();
            buffer.pos(matrix, (float)(itemMinX + 1), (float)(itemY - 1), 0.0F).color(0.0F, 0.0F, 0.0F, 1.0F).endVertex();
            tessellator.draw();
            RenderSystem.enableTexture();
         }

         int y = this.getRowTop(renderIndex);
         int x = this.getRowLeft();
         RenderHelper.disableStandardItemLighting();
         this.renderItem(matrices, item, renderIndex, y, x, itemWidth, itemHeight, int_3, int_4, this.isMouseOver(
           int_3, int_4) && Objects.equals(this.getItemAtPosition(int_3, int_4), item), float_1);
      }

   }

   protected void renderItem(MatrixStack matrices, E item, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean isSelected, float delta) {
      item.render(matrices, index, y, x, entryWidth, entryHeight, mouseX, mouseY, isSelected, delta);
   }

   protected int getRowLeft() {
      return this.left + this.width / 2 - this.getItemWidth() / 2 + 2;
   }

   protected int getRowTop(int index) {
      int integer = this.top + 4 - (int)this.getScroll() + this.headerHeight;

      for(int i = 0; i < this.entries.size() && i < index; ++i) {
         integer += this.entries.get(i).getItemHeight();
      }

      return integer;
   }

   protected boolean isFocused() {
      return false;
   }

   protected void renderHoleBackground(MatrixStack matrices, int y1, int y2, int alpha1, int alpha2) {
      Tessellator tessellator = Tessellator.getInstance();
      BufferBuilder buffer = tessellator.getBuffer();
      this.client.getTextureManager().bindTexture(this.backgroundLocation);
      Matrix4f matrix = matrices.getLast().getMatrix();
      RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
      float float_1 = 32.0F;
      buffer.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
      buffer.pos(matrix, (float)this.left, (float)y2, 0.0F).tex(0.0F, (float)y2 / 32.0F).color(64, 64, 64, alpha2).endVertex();
      buffer.pos(matrix, (float)(this.left + this.width), (float)y2, 0.0F).tex((float)this.width / 32.0F, (float)y2 / 32.0F).color(64, 64, 64, alpha2).endVertex();
      buffer.pos(matrix, (float)(this.left + this.width), (float)y1, 0.0F).tex((float)this.width / 32.0F, (float)y1 / 32.0F).color(64, 64, 64, alpha1).endVertex();
      buffer.pos(matrix, (float)this.left, (float)y1, 0.0F).tex(0.0F, (float)y1 / 32.0F).color(64, 64, 64, alpha1).endVertex();
      tessellator.draw();
   }

   protected E remove(int int_1) {
      E itemListWidget$Item_1 = this.entries.get(int_1);
      return this.removeEntry(this.entries.get(int_1)) ? itemListWidget$Item_1 : null;
   }

   protected boolean removeEntry(E itemListWidget$Item_1) {
      boolean boolean_1 = this.entries.remove(itemListWidget$Item_1);
      if (boolean_1 && itemListWidget$Item_1 == this.getSelectedItem()) {
         this.selectItem(null);
      }

      return boolean_1;
   }

   @OnlyIn(Dist.CLIENT)
   class Entries extends AbstractList<E> {
      private final ArrayList<E> items;

      private Entries() {
         this.items = Lists.newArrayList();
      }

      public void clear() {
         this.items.clear();
      }

      public E get(int int_1) {
         return this.items.get(int_1);
      }

      public int size() {
         return this.items.size();
      }

      public E set(int int_1, E itemListWidget$Item_1) {
         E itemListWidget$Item_2 = this.items.set(int_1, itemListWidget$Item_1);
         itemListWidget$Item_1.parent = DynamicEntryListWidget.this;
         return itemListWidget$Item_2;
      }

      public void add(int int_1, E itemListWidget$Item_1) {
         this.items.add(int_1, itemListWidget$Item_1);
         itemListWidget$Item_1.parent = DynamicEntryListWidget.this;
      }

      public E remove(int int_1) {
         return this.items.remove(int_1);
      }

      // $FF: synthetic method
      Entries(Object x1) {
         this();
      }
   }

   @OnlyIn(Dist.CLIENT)
   public abstract static class Entry<E extends DynamicEntryListWidget.Entry<E>> extends AbstractGui implements IGuiEventListener {
      /** @deprecated */
      @Deprecated
      DynamicEntryListWidget<E> parent;

      public abstract void render(MatrixStack var1, int var2, int var3, int var4, int var5, int var6, int var7, int var8, boolean var9, float var10);

      public boolean isMouseOver(double double_1, double double_2) {
         return Objects.equals(this.parent.getItemAtPosition(double_1, double_2), this);
      }

      public DynamicEntryListWidget<E> getParent() {
         return this.parent;
      }

      public void setParent(DynamicEntryListWidget<E> parent) {
         this.parent = parent;
      }

      public abstract int getItemHeight();

      /** @deprecated */
      @Deprecated
      public int getMorePossibleHeight() {
         return -1;
      }
   }

   public static final class SmoothScrollingSettings {
      public static final double CLAMP_EXTENSION = 200.0D;

      private SmoothScrollingSettings() {
      }
   }
}
