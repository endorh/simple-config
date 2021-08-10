package endorh.simple_config.clothconfig2.gui;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import endorh.simple_config.clothconfig2.ClothConfigInitializer;
import endorh.simple_config.clothconfig2.api.*;
import endorh.simple_config.clothconfig2.math.Rectangle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SimpleSound;
import net.minecraft.client.gui.IGuiEventListener;
import net.minecraft.client.gui.chat.NarratorChatListener;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.IRenderTypeBuffer.Impl;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.IReorderingProcessor;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.Tuple;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.text.*;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class GlobalizedClothConfigScreen extends AbstractConfigScreen implements ReferenceBuildingConfigScreen, Expandable {
   public ClothConfigScreen.ListWidget<AbstractConfigEntry<AbstractConfigEntry<?>>> listWidget;
   private Widget cancelButton;
   private Widget exitButton;
   private final LinkedHashMap<ITextComponent, List<AbstractConfigEntry<?>>> categorizedEntries = Maps.newLinkedHashMap();
   private final ScrollingContainer sideScroller = new ScrollingContainer() {
      public Rectangle getBounds() {
         return new Rectangle(4, 4, GlobalizedClothConfigScreen.this.getSideSliderPosition() - 14 - 4, GlobalizedClothConfigScreen.this.height - 8);
      }

      public int getMaxScrollHeight() {
         int i = 0;

         float var10000;
         GlobalizedClothConfigScreen.Reference reference;
         for(Iterator var2 = GlobalizedClothConfigScreen.this.references.iterator(); var2.hasNext(); i = (int)(var10000 + 9.0F * reference.getScale())) {
            reference = (GlobalizedClothConfigScreen.Reference)var2.next();
            if (i != 0) {
               i = (int)((float)i + 3.0F * reference.getScale());
            }

            var10000 = (float)i;
            Objects.requireNonNull(GlobalizedClothConfigScreen.this.font);
         }

         return i;
      }
   };
   private GlobalizedClothConfigScreen.Reference lastHoveredReference = null;
   private final ScrollingContainer sideSlider = new ScrollingContainer() {
      private final Rectangle empty = new Rectangle();

      public Rectangle getBounds() {
         return this.empty;
      }

      public int getMaxScrollHeight() {
         return 1;
      }
   };
   private final List<GlobalizedClothConfigScreen.Reference> references = Lists.newArrayList();
   private final LazyResettable<Integer> sideExpandLimit = new LazyResettable(() -> {
      int max = 0;
   
      for (Reference reference : this.references) {
         ITextComponent referenceText = reference.getText();
         int width = this.font.getStringPropertyWidth(
           (new StringTextComponent(StringUtils.repeat("  ", reference.getIndent()) + "- ")).append(
             referenceText));
         if (width > max) {
            max = width;
         }
      }

      return Math.min(max + 8, this.width / 4);
   });
   private boolean requestingReferenceRebuilding = false;

   @Internal
   public GlobalizedClothConfigScreen(Screen parent, ITextComponent title, Map<ITextComponent, ConfigCategory> categoryMap, ResourceLocation backgroundLocation) {
      super(parent, title, backgroundLocation);
      categoryMap.forEach((categoryName, category) -> {
         List<AbstractConfigEntry<?>> entries = Lists.newArrayList();
   
         for (Object object : category.getEntries()) {
            AbstractConfigListEntry entry;
            if (object instanceof Tuple) {
               entry = (AbstractConfigListEntry) ((Tuple) object).getB();
            } else {
               entry = (AbstractConfigListEntry) object;
            }
      
            entry.setScreen(this);
            entries.add(entry);
         }

         this.categorizedEntries.put(categoryName, entries);
      });
      this.sideSlider.scrollTo(0.0D, false);
   }

   public void requestReferenceRebuilding() {
      this.requestingReferenceRebuilding = true;
   }

   public Map<ITextComponent, List<AbstractConfigEntry<?>>> getCategorizedEntries() {
      return this.categorizedEntries;
   }

   protected void init() {
      super.init();
      this.sideExpandLimit.reset();
      this.references.clear();
      this.buildReferences();
      this.children.add(this.listWidget = new ClothConfigScreen.ListWidget(this, this.minecraft, this.width - 14, this.height, 30, this.height - 32, this.getBackgroundLocation()));
      this.listWidget.setLeftPos(14);
      this.categorizedEntries.forEach((category, entries) -> {
         if (!this.listWidget.getEventListeners().isEmpty()) {
            this.listWidget.getEventListeners().add((AbstractConfigEntry) new GlobalizedClothConfigScreen.EmptyEntry(5));
         }

         this.listWidget.getEventListeners().add((AbstractConfigEntry) new GlobalizedClothConfigScreen.EmptyEntry(4));
         this.listWidget.getEventListeners().add((AbstractConfigEntry) new GlobalizedClothConfigScreen.CategoryTextEntry(category, category.deepCopy().mergeStyle(TextFormatting.BOLD)));
         this.listWidget.getEventListeners().add((AbstractConfigEntry) new GlobalizedClothConfigScreen.EmptyEntry(4));
         this.listWidget.getEventListeners().addAll((List) entries);
      });
      int buttonWidths = Math.min(200, (this.width - 50 - 12) / 3);
      this.addButton(this.cancelButton = new Button(0, this.height - 26, buttonWidths, 20, this.isEdited() ? new TranslationTextComponent("text.cloth-config.cancel_discard") : new TranslationTextComponent("gui.cancel"), (widget) -> {
         this.quit();
      }));
      this.addButton(this.exitButton = new Button(0, this.height - 26, buttonWidths, 20, NarratorChatListener.EMPTY, (button) -> {
         this.saveAll(true);
      }) {
         public void render(@NotNull MatrixStack matrices, int mouseX, int mouseY, float delta) {
            boolean hasErrors = false;
            Iterator var6 = GlobalizedClothConfigScreen.this.categorizedEntries.values().iterator();

            label35:
            while(var6.hasNext()) {
               List<AbstractConfigEntry<?>> entries = (List)var6.next();
   
               for (AbstractConfigEntry<?> entry : entries) {
                  if (entry.getConfigError().isPresent()) {
                     hasErrors = true;
                     break label35;
                  }
               }
            }

            this.active = GlobalizedClothConfigScreen.this.isEdited() && !hasErrors;
            this.setMessage(hasErrors ? new TranslationTextComponent("text.cloth-config.error_cannot_save") : new TranslationTextComponent("text.cloth-config.save_and_done"));
            super.render(matrices, mouseX, mouseY, delta);
         }
      });
      Optional.ofNullable(this.afterInitConsumer).ifPresent((consumer) -> {
         consumer.accept(this);
      });
   }

   private void buildReferences() {
      this.categorizedEntries.forEach((categoryText, entries) -> {
         this.references.add(new GlobalizedClothConfigScreen.CategoryReference(categoryText));
   
         for (AbstractConfigEntry<?> entry : entries) {
            this.buildReferenceFor(entry, 1);
         }

      });
   }

   private void buildReferenceFor(AbstractConfigEntry<?> entry, int layer) {
      List<ReferenceProvider<?>> referencableEntries = entry.getReferenceProviderEntries();
      if (referencableEntries != null) {
         this.references.add(new GlobalizedClothConfigScreen.ConfigEntryReference(entry, layer));
   
         for (ReferenceProvider<?> referencableEntry : referencableEntries) {
            this.buildReferenceFor(referencableEntry.provideReferenceEntry(), layer + 1);
         }
      }

   }

   public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
      this.lastHoveredReference = null;
      if (this.requestingReferenceRebuilding) {
         this.references.clear();
         this.buildReferences();
         this.requestingReferenceRebuilding = false;
      }

      int sliderPosition = this.getSideSliderPosition();
      ScissorsHandler.INSTANCE.scissor(new Rectangle(sliderPosition, 0, this.width - sliderPosition, this.height));
      if (this.isTransparentBackground()) {
         this.fillGradient(matrices, 14, 0, this.width, this.height, -1072689136, -804253680);
      } else {
         this.renderDirtBackground(0);
         this.overlayBackground(matrices, new Rectangle(14, 0, this.width, this.height), 64, 64, 64, 255, 255);
      }

      this.listWidget.width = this.width - sliderPosition;
      this.listWidget.setLeftPos(sliderPosition);
      this.listWidget.render(matrices, mouseX, mouseY, delta);
      ScissorsHandler.INSTANCE.scissor(new Rectangle(this.listWidget.left, this.listWidget.top, this.listWidget.width, this.listWidget.bottom - this.listWidget.top));
   
      for (AbstractConfigEntry<AbstractConfigEntry<?>> child : this.listWidget.getEventListeners()) {
         child.lateRender(matrices, mouseX, mouseY, delta);
      }

      ScissorsHandler.INSTANCE.removeLastScissor();
      this.font.func_238407_a_(matrices, this.title.func_241878_f(), (float)sliderPosition + (float)(this.width - sliderPosition) / 2.0F - (float)this.font.getStringPropertyWidth(this.title) / 2.0F, 12.0F, -1);
      ScissorsHandler.INSTANCE.removeLastScissor();
      this.cancelButton.x = sliderPosition + (this.width - sliderPosition) / 2 - this.cancelButton.getWidth() - 3;
      this.exitButton.x = sliderPosition + (this.width - sliderPosition) / 2 + 3;
      super.render(matrices, mouseX, mouseY, delta);
      this.sideSlider.updatePosition(delta);
      this.sideScroller.updatePosition(delta);
      if (this.isTransparentBackground()) {
         this.fillGradient(matrices, 0, 0, sliderPosition, this.height, -1240461296, -972025840);
         this.fillGradient(matrices, 0, 0, sliderPosition - 14, this.height, 1744830464, 1744830464);
      } else {
         Tessellator tessellator = Tessellator.getInstance();
         BufferBuilder buffer = tessellator.getBuffer();
         this.minecraft.getTextureManager().bindTexture(this.getBackgroundLocation());
         RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
         float f = 32.0F;
         buffer.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
         buffer.pos(sliderPosition - 14, this.height, 0.0D).tex(0.0F, (float)this.height / 32.0F).color(68, 68, 68, 255).endVertex();
         buffer.pos(sliderPosition, this.height, 0.0D).tex(0.4375F, (float)this.height / 32.0F).color(68, 68, 68, 255).endVertex();
         buffer.pos(sliderPosition, 0.0D, 0.0D).tex(0.4375F, 0.0F).color(68, 68, 68, 255).endVertex();
         buffer.pos(sliderPosition - 14, 0.0D, 0.0D).tex(0.0F, 0.0F).color(68, 68, 68, 255).endVertex();
         tessellator.draw();
         buffer.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
         buffer.pos(0.0D, this.height, 0.0D).tex(0.0F, (float)(this.height + (int)this.sideScroller.scrollAmount) / 32.0F).color(32, 32, 32, 255).endVertex();
         buffer.pos(sliderPosition - 14, this.height, 0.0D).tex((float)(sliderPosition - 14) / 32.0F, (float)(this.height + (int)this.sideScroller.scrollAmount) / 32.0F).color(32, 32, 32, 255).endVertex();
         buffer.pos(sliderPosition - 14, 0.0D, 0.0D).tex((float)(sliderPosition - 14) / 32.0F, (float)((int)this.sideScroller.scrollAmount) / 32.0F).color(32, 32, 32, 255).endVertex();
         buffer.pos(0.0D, 0.0D, 0.0D).tex(0.0F, (float)((int)this.sideScroller.scrollAmount) / 32.0F).color(32, 32, 32, 255).endVertex();
         tessellator.draw();
      }

      Matrix4f matrix = matrices.getLast().getMatrix();
      RenderSystem.disableTexture();
      RenderSystem.enableBlend();
      RenderSystem.disableAlphaTest();
      RenderSystem.defaultBlendFunc();
      RenderSystem.shadeModel(7425);
      Tessellator tessellator = Tessellator.getInstance();
      BufferBuilder buffer = tessellator.getBuffer();
      int scrollOffset = this.isTransparentBackground() ? 120 : 160;
      buffer.begin(7, DefaultVertexFormats.POSITION_COLOR);
      buffer.pos(matrix, (float)(sliderPosition + 4), 0.0F, 100.0F).color(0, 0, 0, 0).endVertex();
      buffer.pos(matrix, (float)sliderPosition, 0.0F, 100.0F).color(0, 0, 0, scrollOffset).endVertex();
      buffer.pos(matrix, (float)sliderPosition, (float)this.height, 100.0F).color(0, 0, 0, scrollOffset).endVertex();
      buffer.pos(matrix, (float)(sliderPosition + 4), (float)this.height, 100.0F).color(0, 0, 0, 0).endVertex();
      tessellator.draw();
      scrollOffset /= 2;
      buffer.begin(7, DefaultVertexFormats.POSITION_COLOR);
      buffer.pos(matrix, (float)(sliderPosition - 14), 0.0F, 100.0F).color(0, 0, 0, scrollOffset).endVertex();
      buffer.pos(matrix, (float)(sliderPosition - 14 - 4), 0.0F, 100.0F).color(0, 0, 0, 0).endVertex();
      buffer.pos(matrix, (float)(sliderPosition - 14 - 4), (float)this.height, 100.0F).color(0, 0, 0, 0).endVertex();
      buffer.pos(matrix, (float)(sliderPosition - 14), (float)this.height, 100.0F).color(0, 0, 0, scrollOffset).endVertex();
      tessellator.draw();
      RenderSystem.shadeModel(7424);
      RenderSystem.disableBlend();
      RenderSystem.enableAlphaTest();
      RenderSystem.enableTexture();
      Rectangle slideArrowBounds = new Rectangle(sliderPosition - 14, 0, 14, this.height);
      RenderSystem.enableAlphaTest();
      Impl immediate = IRenderTypeBuffer.getImpl(Tessellator.getInstance().getBuffer());
      this.font.renderStringAtPos(">", (float)(sliderPosition - 7) - (float)this.font.getStringWidth(">") / 2.0F, (float)(this.height / 2), (slideArrowBounds.contains(mouseX, mouseY) ? 16777120 : 16777215) | MathHelper.clamp(MathHelper.ceil((1.0D - this.sideSlider.scrollAmount) * 255.0D), 0, 255) << 24, false, matrices.getLast().getMatrix(), immediate, false, 0, 15728880);
      this.font.renderStringAtPos("<", (float)(sliderPosition - 7) - (float)this.font.getStringWidth("<") / 2.0F, (float)(this.height / 2), (slideArrowBounds.contains(mouseX, mouseY) ? 16777120 : 16777215) | MathHelper.clamp(MathHelper.ceil(this.sideSlider.scrollAmount * 255.0D), 0, 255) << 24, false, matrices.getLast().getMatrix(), immediate, false, 0, 15728880);
      immediate.finish();
      Rectangle scrollerBounds = this.sideScroller.getBounds();
      if (!scrollerBounds.isEmpty()) {
         ScissorsHandler.INSTANCE.scissor(new Rectangle(0, 0, sliderPosition - 14, this.height));
         scrollOffset = (int)((double)scrollerBounds.y - this.sideScroller.scrollAmount);

         GlobalizedClothConfigScreen.Reference reference;
         float var10000;
         for(Iterator var10 = this.references.iterator(); var10.hasNext(); scrollOffset = (int)(var10000 + (float)(9 + 3) * reference.getScale())) {
            reference = (GlobalizedClothConfigScreen.Reference)var10.next();
            matrices.push();
            matrices.scale(reference.getScale(), reference.getScale(), reference.getScale());
            IFormattableTextComponent text = (new StringTextComponent(StringUtils.repeat("  ", reference.getIndent()) + "- ")).append(reference.getText());
            if (this.lastHoveredReference == null) {
               int var10002 = scrollerBounds.x;
               int var10003 = (int)((float)scrollOffset - 4.0F * reference.getScale());
               int var10004 = (int)((float)this.font.getStringPropertyWidth(text) * reference.getScale());
               Objects.requireNonNull(this.font);
               if ((new Rectangle(var10002, var10003, var10004, (int)((float)(9 + 4) * reference.getScale()))).contains(mouseX, mouseY)) {
                  this.lastHoveredReference = reference;
               }
            }

            this.font.func_238422_b_(matrices, text.func_241878_f(), (float)scrollerBounds.x, (float)scrollOffset, this.lastHoveredReference == reference ? 16769544 : 16777215);
            matrices.pop();
            var10000 = (float)scrollOffset;
            Objects.requireNonNull(this.font);
         }

         ScissorsHandler.INSTANCE.removeLastScissor();
         this.sideScroller.renderScrollBar();
      }

   }

   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      Rectangle slideBounds = new Rectangle(0, 0, this.getSideSliderPosition() - 14, this.height);
      if (button == 0 && slideBounds.contains(mouseX, mouseY) && this.lastHoveredReference != null) {
         this.minecraft.getSoundHandler().play(SimpleSound.master(SoundEvents.UI_BUTTON_CLICK, 1.0F));
         this.lastHoveredReference.go();
         return true;
      } else {
         Rectangle slideArrowBounds = new Rectangle(this.getSideSliderPosition() - 14, 0, 14, this.height);
         if (button == 0 && slideArrowBounds.contains(mouseX, mouseY)) {
            this.setExpanded(!this.isExpanded());
            this.minecraft.getSoundHandler().play(SimpleSound.master(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            return true;
         } else {
            return super.mouseClicked(mouseX, mouseY, button);
         }
      }
   }

   public boolean isExpanded() {
      return this.sideSlider.scrollTarget == 1.0D;
   }

   public void setExpanded(boolean expanded) {
      this.sideSlider.scrollTo(expanded ? 1.0D : 0.0D, true, 2000L);
   }

   public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
      Rectangle slideBounds = new Rectangle(0, 0, this.getSideSliderPosition() - 14, this.height);
      if (slideBounds.contains(mouseX, mouseY)) {
         this.sideScroller.offset(ClothConfigInitializer.getScrollStep() * -amount, true);
         return true;
      } else {
         return super.mouseScrolled(mouseX, mouseY, amount);
      }
   }

   private int getSideSliderPosition() {
      return (int)(this.sideSlider.scrollAmount * (double) this.sideExpandLimit.get() + 14.0D);
   }

   private class ConfigEntryReference implements GlobalizedClothConfigScreen.Reference {
      private final AbstractConfigEntry<?> entry;
      private final int layer;

      public ConfigEntryReference(AbstractConfigEntry<?> entry, int layer) {
         this.entry = entry;
         this.layer = layer;
      }

      public int getIndent() {
         return this.layer;
      }

      public ITextComponent getText() {
         return this.entry.getFieldName();
      }

      public float getScale() {
         return 1.0F;
      }

      public void go() {
         int[] i = new int[]{0};

         AbstractConfigEntry child;
         int i1;
         for(Iterator var2 = GlobalizedClothConfigScreen.this.listWidget.getEventListeners().iterator(); var2.hasNext(); i[0] = i1 + child.getItemHeight()) {
            child = (AbstractConfigEntry)var2.next();
            i1 = i[0];
            if (this.goChild(i, null, child)) {
               return;
            }
         }

      }

      private boolean goChild(int[] i, Integer expandedParent, AbstractConfigEntry<?> root) {
         if (root == this.entry) {
            GlobalizedClothConfigScreen.this.listWidget.scrollTo(expandedParent == null ? (double)i[0] : (double)expandedParent, true);
            return true;
         } else {
            int j = i[0];
            i[0] += root.getInitialReferenceOffset();
            boolean expanded = root instanceof Expandable && ((Expandable)root).isExpanded();
            if (root instanceof Expandable) {
               ((Expandable)root).setExpanded(true);
            }

            List<? extends IGuiEventListener> children = root.getEventListeners();
            if (root instanceof Expandable) {
               ((Expandable)root).setExpanded(expanded);
            }

            Iterator var7 = children.iterator();

            while(true) {
               IGuiEventListener child;
               do {
                  if (!var7.hasNext()) {
                     return false;
                  }

                  child = (IGuiEventListener)var7.next();
               } while(!(child instanceof ReferenceProvider));

               int i1 = i[0];
               if (this.goChild(i, expandedParent != null ? expandedParent : (root instanceof Expandable && !expanded ? j : null), ((ReferenceProvider)child).provideReferenceEntry())) {
                  return true;
               }

               i[0] = i1 + ((ReferenceProvider)child).provideReferenceEntry().getItemHeight();
            }
         }
      }
   }

   private class CategoryReference implements GlobalizedClothConfigScreen.Reference {
      private final ITextComponent category;

      public CategoryReference(ITextComponent category) {
         this.category = category;
      }

      public ITextComponent getText() {
         return this.category;
      }

      public float getScale() {
         return 1.0F;
      }

      public void go() {
         int i = 0;
   
         for (AbstractConfigEntry<?> child : GlobalizedClothConfigScreen.this.listWidget.getEventListeners()) {
            if (child instanceof CategoryTextEntry &&
                ((CategoryTextEntry) child).category == this.category) {
               GlobalizedClothConfigScreen.this.listWidget.scrollTo(i, true);
               return;
            }
            i += child.getItemHeight();
         }

      }
   }

   private interface Reference {
      default int getIndent() {
         return 0;
      }

      ITextComponent getText();

      float getScale();

      void go();
   }

   private static class CategoryTextEntry extends AbstractConfigListEntry<Object> {
      private final ITextComponent category;
      private final ITextComponent text;

      public CategoryTextEntry(ITextComponent category, ITextComponent text) {
         super(new StringTextComponent(UUID.randomUUID().toString()), false);
         this.category = category;
         this.text = text;
      }

      public int getItemHeight() {
         List<IReorderingProcessor> strings = Minecraft.getInstance().fontRenderer.trimStringToWidth(this.text, this.getParent().getItemWidth());
         return strings.isEmpty() ? 0 : 4 + strings.size() * 10;
      }

      public Object getValue() {
         return null;
      }

      public Optional<Object> getDefaultValue() {
         return Optional.empty();
      }

      public void save() {
      }

      public boolean isMouseInside(int mouseX, int mouseY, int x, int y, int entryWidth, int entryHeight) {
         return false;
      }

      public void render(MatrixStack matrices, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean isHovered, float delta) {
         super.render(matrices, index, y, x, entryWidth, entryHeight, mouseX, mouseY, isHovered, delta);
         int yy = y + 2;
         List<IReorderingProcessor> texts = Minecraft.getInstance().fontRenderer.trimStringToWidth(this.text, this.getParent().getItemWidth());
   
         for (IReorderingProcessor text : texts) {
            Minecraft.getInstance().fontRenderer.func_238407_a_(
              matrices, text, (float) (x - 4 + entryWidth / 2 - Minecraft.getInstance().fontRenderer.func_243245_a(text) / 2),
              (float) yy, -1);
            yy += 10;
         }
      }

      public @NotNull List<? extends IGuiEventListener> getEventListeners() {
         return Collections.emptyList();
      }
   }

   private static class EmptyEntry extends AbstractConfigListEntry<Object> {
      private final int height;

      public EmptyEntry(int height) {
         super(new StringTextComponent(UUID.randomUUID().toString()), false);
         this.height = height;
      }

      public int getItemHeight() {
         return this.height;
      }

      public Object getValue() {
         return null;
      }

      public Optional<Object> getDefaultValue() {
         return Optional.empty();
      }

      public boolean isMouseInside(int mouseX, int mouseY, int x, int y, int entryWidth, int entryHeight) {
         return false;
      }

      public void save() {
      }

      public void render(MatrixStack matrices, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean isHovered, float delta) {
      }

      public @NotNull List<? extends IGuiEventListener> getEventListeners() {
         return Collections.emptyList();
      }
   }
}
