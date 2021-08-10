package endorh.simple_config.clothconfig2.api;

import com.mojang.blaze3d.matrix.MatrixStack;
import endorh.simple_config.clothconfig2.gui.ClothConfigScreen;
import endorh.simple_config.clothconfig2.math.Rectangle;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public abstract class AbstractConfigListEntry<T> extends AbstractConfigEntry<T> {
   private final ITextComponent fieldName;
   private boolean editable = true;
   private boolean requiresRestart;

   public AbstractConfigListEntry(ITextComponent fieldName, boolean requiresRestart) {
      this.fieldName = fieldName;
      this.requiresRestart = requiresRestart;
   }

   public boolean isRequiresRestart() {
      return this.requiresRestart;
   }

   public void setRequiresRestart(boolean requiresRestart) {
      this.requiresRestart = requiresRestart;
   }

   public boolean isEditable() {
      return this.getConfigScreen().isEditable() && this.editable;
   }

   public void setEditable(boolean editable) {
      this.editable = editable;
   }

   public final int getPreferredTextColor() {
      return this.getConfigError().isPresent() ? 16733525 : 16777215;
   }

   public Rectangle getEntryArea(int x, int y, int entryWidth, int entryHeight) {
      return new Rectangle(this.getParent().left, y, this.getParent().right - this.getParent().left, this.getItemHeight() - 4);
   }

   public boolean isMouseInside(int mouseX, int mouseY, int x, int y, int entryWidth, int entryHeight) {
      return this.getParent().isMouseOver(mouseX, mouseY) && this.getEntryArea(x, y, entryWidth, entryHeight).contains(mouseX, mouseY);
   }

   public void render(MatrixStack matrices, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean isHovered, float delta) {
      if (this.isMouseInside(mouseX, mouseY, x, y, entryWidth, entryHeight)) {
         Rectangle area = this.getEntryArea(x, y, entryWidth, entryHeight);
         if (this.getParent() instanceof ClothConfigScreen.ListWidget) {
            ((ClothConfigScreen.ListWidget)this.getParent()).thisTimeTarget = area;
         }
      }

   }

   public ITextComponent getFieldName() {
      return this.fieldName;
   }
}
