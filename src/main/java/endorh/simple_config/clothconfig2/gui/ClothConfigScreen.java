package endorh.simple_config.clothconfig2.gui;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import endorh.simple_config.clothconfig2.api.*;
import endorh.simple_config.clothconfig2.gui.widget.DynamicElementListWidget;
import endorh.simple_config.clothconfig2.impl.EasingMethod;
import endorh.simple_config.clothconfig2.math.Point;
import endorh.simple_config.clothconfig2.math.Rectangle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.chat.NarratorChatListener;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Tuple;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.ApiStatus.ScheduledForRemoval;

import java.util.*;
import java.util.stream.Collectors;

@OnlyIn(Dist.CLIENT)
public class ClothConfigScreen extends AbstractTabbedConfigScreen {
   private final ScrollingContainer tabsScroller = new ScrollingContainer() {
      public Rectangle getBounds() {
         return new Rectangle(0, 0, 1, ClothConfigScreen.this.width - 40);
      }

      public int getMaxScrollHeight() {
         return (int)ClothConfigScreen.this.getTabsMaximumScrolled();
      }

      public void updatePosition(float delta) {
         super.updatePosition(delta);
         this.scrollAmount = this.clamp(this.scrollAmount, 0.0D);
      }
   };
   public ClothConfigScreen.ListWidget<AbstractConfigEntry<AbstractConfigEntry<?>>> listWidget;
   private final LinkedHashMap<ITextComponent, List<AbstractConfigEntry<?>>> categorizedEntries = Maps.newLinkedHashMap();
   private final List<Tuple<ITextComponent, Integer>> tabs;
   private Widget quitButton;
   private Widget saveButton;
   private Widget buttonLeftTab;
   private Widget buttonRightTab;
   private Rectangle tabsBounds;
   private Rectangle tabsLeftBounds;
   private Rectangle tabsRightBounds;
   private double tabsMaximumScrolled = -1.0D;
   private final List<ClothConfigTabButton> tabButtons = Lists.newArrayList();
   private final Map<ITextComponent, ConfigCategory> categoryMap;

   @Internal
   public ClothConfigScreen(Screen parent, ITextComponent title, Map<ITextComponent, ConfigCategory> categoryMap, ResourceLocation backgroundLocation) {
      super(parent, title, backgroundLocation);
      categoryMap.forEach((categoryName, category) -> {
         List<AbstractConfigEntry<?>> entries = Lists.newArrayList();
         Iterator var4 = category.getEntries().iterator();

         while(var4.hasNext()) {
            Object object = var4.next();
            AbstractConfigListEntry entry;
            if (object instanceof Tuple) {
               entry = (AbstractConfigListEntry)((Tuple)object).getB();
            } else {
               entry = (AbstractConfigListEntry)object;
            }

            entry.setScreen(this);
            entries.add(entry);
         }

         this.categorizedEntries.put(categoryName, entries);
         if (category.getBackground() != null) {
            this.registerCategoryBackground(categoryName, category.getBackground());
         }

      });
      this.tabs = (List)this.categorizedEntries.keySet().stream().map((s) -> {
         return new Tuple(s, Minecraft.getInstance().fontRenderer.getStringPropertyWidth(s) + 8);
      }).collect(Collectors.toList());
      this.categoryMap = categoryMap;
   }

   public ITextComponent getSelectedCategory() {
      return (ITextComponent)((Tuple)this.tabs.get(this.selectedCategoryIndex)).getA();
   }

   public Map<ITextComponent, List<AbstractConfigEntry<?>>> getCategorizedEntries() {
      return this.categorizedEntries;
   }

   public boolean isEdited() {
      return super.isEdited();
   }

   /** @deprecated */
   @Deprecated
   public void setEdited(boolean edited) {
      super.setEdited(edited);
   }

   /** @deprecated */
   @Deprecated
   public void setEdited(boolean edited, boolean requiresRestart) {
      super.setEdited(edited, requiresRestart);
   }

   public void saveAll(boolean openOtherScreens) {
      super.saveAll(openOtherScreens);
   }

   protected void init() {
      super.init();
      this.tabButtons.clear();
      this.children.add(this.listWidget = new ClothConfigScreen.ListWidget(this, this.minecraft, this.width, this.height, this.isShowingTabs() ? 70 : 30, this.height - 32, this.getBackgroundLocation()));
      if (this.categorizedEntries.size() > this.selectedCategoryIndex) {
         this.listWidget.getEventListeners().addAll((List)Lists.newArrayList(this.categorizedEntries.values()).get(this.selectedCategoryIndex));
      }

      int buttonWidths = Math.min(200, (this.width - 50 - 12) / 3);
      this.addButton(this.quitButton = new Button(this.width / 2 - buttonWidths - 3, this.height - 26, buttonWidths, 20, this.isEdited() ? new TranslationTextComponent("text.cloth-config.cancel_discard") : new TranslationTextComponent("gui.cancel"), (widget) -> {
         this.quit();
      }));
      this.addButton(this.saveButton = new Button(this.width / 2 + 3, this.height - 26, buttonWidths, 20, NarratorChatListener.EMPTY, (button) -> {
         this.saveAll(true);
      }) {
         public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
            boolean hasErrors = false;
            Iterator var6 = Lists.newArrayList(ClothConfigScreen.this.categorizedEntries.values()).iterator();

            while(var6.hasNext()) {
               List<AbstractConfigEntry<?>> entries = (List)var6.next();
               Iterator var8 = entries.iterator();

               while(var8.hasNext()) {
                  AbstractConfigEntry<?> entry = (AbstractConfigEntry)var8.next();
                  if (entry.getConfigError().isPresent()) {
                     hasErrors = true;
                     break;
                  }
               }

               if (hasErrors) {
                  break;
               }
            }

            this.active = ClothConfigScreen.this.isEdited() && !hasErrors;
            this.setMessage(hasErrors ? new TranslationTextComponent("text.cloth-config.error_cannot_save") : new TranslationTextComponent("text.cloth-config.save_and_done"));
            super.render(matrices, mouseX, mouseY, delta);
         }
      });
      this.saveButton.active = this.isEdited();
      if (this.isShowingTabs()) {
         this.tabsBounds = new Rectangle(0, 41, this.width, 24);
         this.tabsLeftBounds = new Rectangle(0, 41, 18, 24);
         this.tabsRightBounds = new Rectangle(this.width - 18, 41, 18, 24);
         this.children.add(this.buttonLeftTab = new Button(4, 44, 12, 18, NarratorChatListener.EMPTY, (button) -> {
            this.tabsScroller.scrollTo(0.0D, true);
         }) {
            public void renderButton(MatrixStack matrices, int mouseX, int mouseY, float delta) {
               ClothConfigScreen.this.minecraft.getTextureManager().bindTexture(AbstractConfigScreen.CONFIG_TEX);
               RenderSystem.color4f(1.0F, 1.0F, 1.0F, this.alpha);
               int int_3 = this.getYImage(this.isHovered());
               RenderSystem.enableBlend();
               RenderSystem.blendFuncSeparate(770, 771, 0, 1);
               RenderSystem.blendFunc(770, 771);
               this.blit(matrices, this.x, this.y, 12, 18 * int_3, this.width, this.height);
            }
         });
         int j = 0;

         for(Iterator var3 = this.tabs.iterator(); var3.hasNext(); ++j) {
            Tuple<ITextComponent, Integer> tab = (Tuple)var3.next();
            this.tabButtons.add(new ClothConfigTabButton(this, j, -100, 43, tab.getB(), 20,
                                                         tab.getA(), this.categoryMap.get(tab.getA())
                                                           .getDescription()));
         }

         this.children.addAll(this.tabButtons);
         this.children.add(this.buttonRightTab = new Button(this.width - 16, 44, 12, 18, NarratorChatListener.EMPTY, (button) -> {
            this.tabsScroller.scrollTo(this.tabsScroller.getMaxScroll(), true);
         }) {
            public void renderButton(MatrixStack matrices, int mouseX, int mouseY, float delta) {
               ClothConfigScreen.this.minecraft.getTextureManager().bindTexture(AbstractConfigScreen.CONFIG_TEX);
               RenderSystem.color4f(1.0F, 1.0F, 1.0F, this.alpha);
               int int_3 = this.getYImage(this.isHovered());
               RenderSystem.enableBlend();
               RenderSystem.blendFuncSeparate(770, 771, 0, 1);
               RenderSystem.blendFunc(770, 771);
               this.blit(matrices, this.x, this.y, 0, 18 * int_3, this.width, this.height);
            }
         });
      } else {
         this.tabsBounds = this.tabsLeftBounds = this.tabsRightBounds = new Rectangle();
      }

      Optional.ofNullable(this.afterInitConsumer).ifPresent((consumer) -> {
         consumer.accept(this);
      });
   }

   public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
      if (this.tabsBounds.contains(mouseX, mouseY) && !this.tabsLeftBounds.contains(mouseX, mouseY) && !this.tabsRightBounds.contains(mouseX, mouseY) && amount != 0.0D) {
         this.tabsScroller.offset(-amount * 16.0D, true);
         return true;
      } else {
         return super.mouseScrolled(mouseX, mouseY, amount);
      }
   }

   public double getTabsMaximumScrolled() {
      if (this.tabsMaximumScrolled == -1.0D) {
         int[] i = new int[]{0};

         Tuple pair;
         for(Iterator var2 = this.tabs.iterator(); var2.hasNext(); i[0] += (Integer)pair.getB() + 2) {
            pair = (Tuple)var2.next();
         }

         this.tabsMaximumScrolled = i[0];
      }

      return this.tabsMaximumScrolled + 6.0D;
   }

   public void resetTabsMaximumScrolled() {
      this.tabsMaximumScrolled = -1.0D;
   }

   public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
      Iterator var6;
      if (this.isShowingTabs()) {
         this.tabsScroller.updatePosition(delta * 3.0F);
         int xx = 24 - (int)this.tabsScroller.scrollAmount;

         ClothConfigTabButton tabButton;
         for(var6 = this.tabButtons.iterator(); var6.hasNext(); xx += tabButton.getWidth() + 2) {
            tabButton = (ClothConfigTabButton)var6.next();
            tabButton.x = xx;
         }

         this.buttonLeftTab.active = this.tabsScroller.scrollAmount > 0.0D;
         this.buttonRightTab.active = this.tabsScroller.scrollAmount < this.getTabsMaximumScrolled() - (double)this.width + 40.0D;
      }

      if (this.isTransparentBackground()) {
         this.fillGradient(matrices, 0, 0, this.width, this.height, -1072689136, -804253680);
      } else {
         this.renderDirtBackground(0);
      }

      this.listWidget.render(matrices, mouseX, mouseY, delta);
      ScissorsHandler.INSTANCE.scissor(new Rectangle(this.listWidget.left, this.listWidget.top, this.listWidget.width, this.listWidget.bottom - this.listWidget.top));
      Iterator var10 = this.listWidget.getEventListeners().iterator();

      while(var10.hasNext()) {
         AbstractConfigEntry child = (AbstractConfigEntry)var10.next();
         child.lateRender(matrices, mouseX, mouseY, delta);
      }

      ScissorsHandler.INSTANCE.removeLastScissor();
      if (this.isShowingTabs()) {
         drawCenteredString(matrices, this.minecraft.fontRenderer, this.title, this.width / 2, 18, -1);
         Rectangle onlyInnerTabBounds = new Rectangle(this.tabsBounds.x + 20, this.tabsBounds.y, this.tabsBounds.width - 40, this.tabsBounds.height);
         ScissorsHandler.INSTANCE.scissor(onlyInnerTabBounds);
         if (this.isTransparentBackground()) {
            this.fillGradient(matrices, onlyInnerTabBounds.x, onlyInnerTabBounds.y, onlyInnerTabBounds.getMaxX(), onlyInnerTabBounds.getMaxY(), 1744830464, 1744830464);
         } else {
            this.overlayBackground(matrices, onlyInnerTabBounds, 32, 32, 32, 255, 255);
         }

         this.tabButtons.forEach((widget) -> {
            widget.render(matrices, mouseX, mouseY, delta);
         });
         this.drawTabsShades(matrices, 0, this.isTransparentBackground() ? 120 : 255);
         ScissorsHandler.INSTANCE.removeLastScissor();
         this.buttonLeftTab.render(matrices, mouseX, mouseY, delta);
         this.buttonRightTab.render(matrices, mouseX, mouseY, delta);
      } else {
         drawCenteredString(matrices, this.minecraft.fontRenderer, this.title, this.width / 2, 12, -1);
      }

      int var10004;
      if (this.isEditable()) {
         List<ITextComponent> errors = Lists.newArrayList();
         var6 = Lists.newArrayList(this.categorizedEntries.values()).iterator();

         while(var6.hasNext()) {
            List<AbstractConfigEntry<?>> entries = (List)var6.next();
            Iterator var8 = entries.iterator();

            while(var8.hasNext()) {
               AbstractConfigEntry<?> entry = (AbstractConfigEntry)var8.next();
               if (entry.getConfigError().isPresent()) {
                  errors.add(entry.getConfigError().get());
               }
            }
         }

         if (errors.size() > 0) {
            this.minecraft.getTextureManager().bindTexture(CONFIG_TEX);
            RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
            String text = "§c" + (errors.size() == 1 ? errors.get(0).copyRaw().getString() : I18n.format("text.cloth-config.multi_error"));
            int stringWidth;
            if (this.isTransparentBackground()) {
               stringWidth = this.minecraft.fontRenderer.getStringWidth(text);
               var10004 = 20 + stringWidth;
               Objects.requireNonNull(this.minecraft.fontRenderer);
               this.fillGradient(matrices, 8, 9, var10004, 14 + 9, 1744830464, 1744830464);
            }

            this.blit(matrices, 10, 10, 0, 54, 3, 11);
            drawString(matrices, this.minecraft.fontRenderer, text, 18, 12, -1);
            if (errors.size() > 1) {
               stringWidth = this.minecraft.fontRenderer.getStringWidth(text);
               if (mouseX >= 10 && mouseY >= 10 && mouseX <= 18 + stringWidth) {
                  Objects.requireNonNull(this.minecraft.fontRenderer);
                  if (mouseY <= 14 + 9) {
                     this.addTooltip(Tooltip.of(new Point(mouseX, mouseY),
                                                errors.toArray(new ITextComponent[0])));
                  }
               }
            }
         }
      } else if (!this.isEditable()) {
         this.minecraft.getTextureManager().bindTexture(CONFIG_TEX);
         RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
         String text = "§c" + I18n.format("text.cloth-config.not_editable");
         if (this.isTransparentBackground()) {
            int stringWidth = this.minecraft.fontRenderer.getStringWidth(text);
            var10004 = 20 + stringWidth;
            Objects.requireNonNull(this.minecraft.fontRenderer);
            this.fillGradient(matrices, 8, 9, var10004, 14 + 9, 1744830464, 1744830464);
         }

         this.blit(matrices, 10, 10, 0, 54, 3, 11);
         drawString(matrices, this.minecraft.fontRenderer, text, 18, 12, -1);
      }

      super.render(matrices, mouseX, mouseY, delta);
   }

   /** @deprecated */
   @Deprecated
   @ScheduledForRemoval
   public void queueTooltip(QueuedTooltip queuedTooltip) {
      super.addTooltip(queuedTooltip);
   }

   private void drawTabsShades(MatrixStack matrices, int lightColor, int darkColor) {
      this.drawTabsShades(matrices.getLast().getMatrix(), lightColor, darkColor);
   }

   private void drawTabsShades(Matrix4f matrix, int lightColor, int darkColor) {
      RenderSystem.enableBlend();
      RenderSystem.blendFuncSeparate(770, 771, 0, 1);
      RenderSystem.disableAlphaTest();
      RenderSystem.shadeModel(7425);
      RenderSystem.disableTexture();
      Tessellator tessellator = Tessellator.getInstance();
      BufferBuilder buffer = tessellator.getBuffer();
      buffer.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
      buffer.pos(matrix, (float)(this.tabsBounds.getMinX() + 20), (float)(this.tabsBounds.getMinY() + 4), 0.0F).tex(0.0F, 1.0F).color(0, 0, 0, lightColor).endVertex();
      buffer.pos(matrix, (float)(this.tabsBounds.getMaxX() - 20), (float)(this.tabsBounds.getMinY() + 4), 0.0F).tex(1.0F, 1.0F).color(0, 0, 0, lightColor).endVertex();
      buffer.pos(matrix, (float)(this.tabsBounds.getMaxX() - 20), (float)this.tabsBounds.getMinY(), 0.0F).tex(1.0F, 0.0F).color(0, 0, 0, darkColor).endVertex();
      buffer.pos(matrix, (float)(this.tabsBounds.getMinX() + 20), (float)this.tabsBounds.getMinY(), 0.0F).tex(0.0F, 0.0F).color(0, 0, 0, darkColor).endVertex();
      tessellator.draw();
      buffer.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
      buffer.pos(matrix, (float)(this.tabsBounds.getMinX() + 20), (float)this.tabsBounds.getMaxY(), 0.0F).tex(0.0F, 1.0F).color(0, 0, 0, darkColor).endVertex();
      buffer.pos(matrix, (float)(this.tabsBounds.getMaxX() - 20), (float)this.tabsBounds.getMaxY(), 0.0F).tex(1.0F, 1.0F).color(0, 0, 0, darkColor).endVertex();
      buffer.pos(matrix, (float)(this.tabsBounds.getMaxX() - 20), (float)(this.tabsBounds.getMaxY() - 4), 0.0F).tex(1.0F, 0.0F).color(0, 0, 0, lightColor).endVertex();
      buffer.pos(matrix, (float)(this.tabsBounds.getMinX() + 20), (float)(this.tabsBounds.getMaxY() - 4), 0.0F).tex(0.0F, 0.0F).color(0, 0, 0, lightColor).endVertex();
      tessellator.draw();
      RenderSystem.enableTexture();
      RenderSystem.shadeModel(7424);
      RenderSystem.enableAlphaTest();
      RenderSystem.disableBlend();
   }

   public void save() {
      super.save();
   }

   public boolean isEditable() {
      return super.isEditable();
   }

   public static class ListWidget<R extends DynamicElementListWidget.ElementEntry<R>> extends DynamicElementListWidget<R> {
      private final AbstractConfigScreen screen;
      private boolean hasCurrent;
      private double currentX;
      private double currentY;
      private double currentWidth;
      private double currentHeight;
      public Rectangle target;
      public Rectangle thisTimeTarget;
      public long lastTouch;
      public long start;
      public long duration;

      public ListWidget(AbstractConfigScreen screen, Minecraft client, int width, int height, int top, int bottom, ResourceLocation backgroundLocation) {
         super(client, width, height, top, bottom, backgroundLocation);
         this.setRenderSelection(false);
         this.screen = screen;
      }

      public int getItemWidth() {
         return this.width - 80;
      }

      protected int getScrollbarPosition() {
         return this.left + this.width - 36;
      }

      protected void renderItem(MatrixStack matrices, R item, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean isSelected, float delta) {
         if (item instanceof AbstractConfigEntry) {
            ((AbstractConfigEntry)item).updateSelected(this.getFocused() == item);
         }

         super.renderItem(matrices, item, index, y, x, entryWidth, entryHeight, mouseX, mouseY, isSelected, delta);
      }

      protected void renderList(MatrixStack matrices, int startX, int startY, int int_3, int int_4, float delta) {
         this.thisTimeTarget = null;
         long timePast;
         if (this.hasCurrent) {
            timePast = System.currentTimeMillis() - this.lastTouch;
            int alpha = timePast <= 200L ? 255 : MathHelper.ceil(255.0D - (double)(Math.min((float)(timePast - 200L), 500.0F) / 500.0F) * 255.0D);
            alpha = alpha * 36 / 255 << 24;
            this.fillGradient(matrices, this.currentX, this.currentY, this.currentX + this.currentWidth, this.currentY + this.currentHeight, 16777215 | alpha, 16777215 | alpha);
         }

         super.renderList(matrices, startX, startY, int_3, int_4, delta);
         if (this.thisTimeTarget != null && this.isMouseOver(int_3, int_4)) {
            this.lastTouch = System.currentTimeMillis();
         }

         if (this.thisTimeTarget != null && !this.thisTimeTarget.equals(this.target)) {
            if (!this.hasCurrent) {
               this.currentX = this.thisTimeTarget.x;
               this.currentY = this.thisTimeTarget.y;
               this.currentWidth = this.thisTimeTarget.width;
               this.currentHeight = this.thisTimeTarget.height;
               this.hasCurrent = true;
            }

            this.target = this.thisTimeTarget.clone();
            this.start = this.lastTouch;
            this.duration = 40L;
         } else if (this.hasCurrent && this.target != null) {
            timePast = System.currentTimeMillis() - this.start;
            this.currentX = (int)ScrollingContainer.ease(this.currentX, this.target.x, Math.min((double)timePast / (double)this.duration * (double)delta * 3.0D, 1.0D), EasingMethod.EasingMethodImpl.LINEAR);
            this.currentY = (int)ScrollingContainer.ease(this.currentY, this.target.y, Math.min((double)timePast / (double)this.duration * (double)delta * 3.0D, 1.0D), EasingMethod.EasingMethodImpl.LINEAR);
            this.currentWidth = (int)ScrollingContainer.ease(this.currentWidth, this.target.width, Math.min((double)timePast / (double)this.duration * (double)delta * 3.0D, 1.0D), EasingMethod.EasingMethodImpl.LINEAR);
            this.currentHeight = (int)ScrollingContainer.ease(this.currentHeight,
                                                              this.target.height, Math.min((double)timePast / (double)this.duration * (double)delta * 3.0D, 1.0D), EasingMethod.EasingMethodImpl.LINEAR);
         }

      }

      protected void fillGradient(MatrixStack matrices, double xStart, double yStart, double xEnd, double yEnd, int colorStart, int colorEnd) {
         RenderSystem.disableTexture();
         RenderSystem.enableBlend();
         RenderSystem.disableAlphaTest();
         RenderSystem.defaultBlendFunc();
         RenderSystem.shadeModel(7425);
         Tessellator tessellator = Tessellator.getInstance();
         BufferBuilder bufferBuilder = tessellator.getBuffer();
         bufferBuilder.begin(7, DefaultVertexFormats.POSITION_COLOR);
         fillGradient(matrices.getLast().getMatrix(), bufferBuilder, xStart, yStart, xEnd, yEnd, this.getBlitOffset(), colorStart, colorEnd);
         tessellator.draw();
         RenderSystem.shadeModel(7424);
         RenderSystem.disableBlend();
         RenderSystem.enableAlphaTest();
         RenderSystem.enableTexture();
      }

      protected static void fillGradient(Matrix4f matrix4f, BufferBuilder bufferBuilder, double xStart, double yStart, double xEnd, double yEnd, int i, int j, int k) {
         float f = (float)(j >> 24 & 255) / 255.0F;
         float g = (float)(j >> 16 & 255) / 255.0F;
         float h = (float)(j >> 8 & 255) / 255.0F;
         float l = (float)(j & 255) / 255.0F;
         float m = (float)(k >> 24 & 255) / 255.0F;
         float n = (float)(k >> 16 & 255) / 255.0F;
         float o = (float)(k >> 8 & 255) / 255.0F;
         float p = (float)(k & 255) / 255.0F;
         bufferBuilder.pos(matrix4f, (float)xEnd, (float)yStart, (float)i).color(g, h, l, f).endVertex();
         bufferBuilder.pos(matrix4f, (float)xStart, (float)yStart, (float)i).color(g, h, l, f).endVertex();
         bufferBuilder.pos(matrix4f, (float)xStart, (float)yEnd, (float)i).color(n, o, p, m).endVertex();
         bufferBuilder.pos(matrix4f, (float)xEnd, (float)yEnd, (float)i).color(n, o, p, m).endVertex();
      }

      public boolean mouseClicked(double mouseX, double mouseY, int button) {
         this.updateScrollingState(mouseX, mouseY, button);
         if (!this.isMouseOver(mouseX, mouseY)) {
            return false;
         } else {
            Iterator var6 = this.getEventListeners().iterator();

            DynamicElementListWidget.ElementEntry entry;
            do {
               if (!var6.hasNext()) {
                  if (button == 0) {
                     this.clickedHeader((int)(mouseX - (double)(this.left + this.width / 2 - this.getItemWidth() / 2)), (int)(mouseY - (double)this.top) + (int)this.getScroll() - 4);
                     return true;
                  }

                  return this.scrolling;
               }

               entry = (DynamicElementListWidget.ElementEntry)var6.next();
            } while(!entry.mouseClicked(mouseX, mouseY, button));

            this.setListener(entry);
            this.setDragging(true);
            return true;
         }
      }

      protected void renderBackBackground(MatrixStack matrices, BufferBuilder buffer, Tessellator tessellator) {
         if (!this.screen.isTransparentBackground()) {
            super.renderBackBackground(matrices, buffer, tessellator);
         } else {
            this.fillGradient(matrices, this.left, this.top, this.right, this.bottom, 1744830464, 1744830464);
         }

      }

      protected void renderHoleBackground(MatrixStack matrices, int y1, int y2, int alpha1, int alpha2) {
         if (!this.screen.isTransparentBackground()) {
            super.renderHoleBackground(matrices, y1, y2, alpha1, alpha2);
         }

      }
   }
}
