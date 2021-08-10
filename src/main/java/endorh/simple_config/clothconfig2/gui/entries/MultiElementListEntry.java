package endorh.simple_config.clothconfig2.gui.entries;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import endorh.simple_config.clothconfig2.api.AbstractConfigEntry;
import endorh.simple_config.clothconfig2.api.AbstractConfigListEntry;
import endorh.simple_config.clothconfig2.api.Expandable;
import endorh.simple_config.clothconfig2.api.ReferenceProvider;
import endorh.simple_config.clothconfig2.gui.widget.DynamicEntryListWidget;
import endorh.simple_config.clothconfig2.math.Rectangle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SimpleSound;
import net.minecraft.client.gui.IGuiEventListener;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@OnlyIn(Dist.CLIENT)
public class MultiElementListEntry<T> extends TooltipListEntry<T> implements Expandable {
   private static final ResourceLocation CONFIG_TEX = new ResourceLocation("cloth-config2", "textures/gui/cloth_config.png");
   private final T object;
   private final List<AbstractConfigListEntry<?>> entries;
   private final MultiElementListEntry<T>.CategoryLabelWidget widget;
   private final List<IGuiEventListener> children;
   private boolean expanded;

   @Internal
   public MultiElementListEntry(ITextComponent categoryName, T object, List<AbstractConfigListEntry<?>> entries, boolean defaultExpanded) {
      super(categoryName, null);
      this.object = object;
      this.entries = entries;
      this.expanded = defaultExpanded;
      this.widget = new MultiElementListEntry.CategoryLabelWidget();
      this.children = Lists.newArrayList(new IGuiEventListener[]{this.widget});
      this.children.addAll(entries);
      //noinspection unchecked
      this.setReferenceProviderEntries((List<ReferenceProvider<?>>) (List<?>) entries);
   }

   public boolean isRequiresRestart() {
      Iterator<AbstractConfigListEntry<?>> var1 = this.entries.iterator();

      AbstractConfigListEntry<?> entry;
      do {
         if (!var1.hasNext()) {
            return false;
         }
         
         entry = var1.next();
      } while(!entry.isRequiresRestart());

      return true;
   }

   public boolean isEdited() {
      Iterator<AbstractConfigListEntry<?>> var1 = this.entries.iterator();

      AbstractConfigListEntry<?> entry;
      do {
         if (!var1.hasNext()) {
            return false;
         }

         entry = var1.next();
      } while(!entry.isEdited());

      return true;
   }

   public void setRequiresRestart(boolean requiresRestart) {
   }

   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      return super.mouseClicked(mouseX, mouseY, button);
   }

   public ITextComponent getCategoryName() {
      return this.getFieldName();
   }

   public T getValue() {
      return this.object;
   }

   public Optional<T> getDefaultValue() {
      return Optional.empty();
   }

   public void render(MatrixStack matrices, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean isHovered, float delta) {
      super.render(matrices, index, y, x, entryWidth, entryHeight, mouseX, mouseY, isHovered, delta);
      Minecraft.getInstance().getTextureManager().bindTexture(CONFIG_TEX);
      RenderHelper.disableStandardItemLighting();
      RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
      this.blit(matrices, x - 15, y + 5, 24, (this.widget.rectangle.contains(mouseX, mouseY) ? 18 : 0) + (this.expanded ? 9 : 0), 9, 9);
      Minecraft.getInstance().fontRenderer.func_238407_a_(matrices, this.getDisplayedFieldName().func_241878_f(), (float)x, (float)(y + 6), this.widget.rectangle.contains(mouseX, mouseY) ? -1638890 : -1);
   
      for (AbstractConfigListEntry<?> entry : this.entries) {
         entry.setParent((DynamicEntryListWidget) this.getParent());
         entry.setScreen(this.getConfigScreen());
      }

      if (this.expanded) {
         int yy = y + 24;
         for (AbstractConfigListEntry<?> entry : this.entries) {
            entry.render(matrices, -1, yy, x + 14, entryWidth - 14, entry.getItemHeight(), mouseX, mouseY, isHovered, delta);
            yy += entry.getItemHeight();
            yy += Math.max(0, entry.getMorePossibleHeight());
         }
      }
   }

   public Rectangle getEntryArea(int x, int y, int entryWidth, int entryHeight) {
      this.widget.rectangle.x = x - 15;
      this.widget.rectangle.y = y;
      this.widget.rectangle.width = entryWidth + 15;
      this.widget.rectangle.height = 24;
      return new Rectangle(this.getParent().left, y, this.getParent().right - this.getParent().left, 20);
   }

   public int getItemHeight() {
      if (this.expanded) {
         int i = 24;
         for (AbstractConfigListEntry<?> entry : this.entries)
            i += entry.getItemHeight();
         return i;
      }
      return 24;
   }

   public void updateSelected(boolean isSelected) {
      for (AbstractConfigListEntry<?> entry : this.entries)
         entry.updateSelected(this.expanded && isSelected && this.getListener() == entry);
   }

   public int getInitialReferenceOffset() {
      return 24;
   }

   public void lateRender(MatrixStack matrices, int mouseX, int mouseY, float delta) {
      if (this.expanded) {
         for (AbstractConfigListEntry<?> entry : this.entries)
            entry.lateRender(matrices, mouseX, mouseY, delta);
      }
   }

   public int getMorePossibleHeight() {
      if (!this.expanded) {
         return -1;
      } else {
         List<Integer> list = new ArrayList<>();
         int i = 24;
   
         for (AbstractConfigListEntry<?> entry : this.entries) {
            i += entry.getItemHeight();
            if (entry.getMorePossibleHeight() >= 0) {
               list.add(i + entry.getMorePossibleHeight());
            }
         }

         list.add(i);
         return list.stream().max(Integer::compare).orElse(0) - this.getItemHeight();
      }
   }

   public @NotNull List<? extends IGuiEventListener> getEventListeners() {
      return this.expanded ? this.children : Collections.singletonList(this.widget);
   }

   public void save() {
      this.entries.forEach(AbstractConfigEntry::save);
   }

   public Optional<ITextComponent> getError() {
      List<ITextComponent> errors =
        this.entries.stream().map(AbstractConfigEntry::getConfigError).filter(Optional::isPresent).map(Optional::get).collect(Collectors.toList());
      return errors.size() > 1 ? Optional.of(new TranslationTextComponent("text.cloth-config.multi_error")) : errors.stream().findFirst();
   }

   public boolean isExpanded() {
      return this.expanded;
   }

   public void setExpanded(boolean expanded) {
      this.expanded = expanded;
   }

   public class CategoryLabelWidget implements IGuiEventListener {
      private final Rectangle rectangle = new Rectangle();

      public boolean mouseClicked(double double_1, double double_2, int int_1) {
         if (this.rectangle.contains(double_1, double_2)) {
            MultiElementListEntry.this.expanded = !MultiElementListEntry.this.expanded;
            Minecraft.getInstance().getSoundHandler().play(SimpleSound.master(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            return true;
         } else {
            return false;
         }
      }
   }
}
