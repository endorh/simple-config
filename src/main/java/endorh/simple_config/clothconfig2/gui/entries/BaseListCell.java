package endorh.simple_config.clothconfig2.gui.entries;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.gui.FocusableGui;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.Optional;
import java.util.function.Supplier;

@OnlyIn(Dist.CLIENT)
public abstract class BaseListCell extends FocusableGui {
   private Supplier<Optional<ITextComponent>> errorSupplier;

   public final int getPreferredTextColor() {
      return this.getConfigError().isPresent() ? 16733525 : 14737632;
   }

   public final Optional<ITextComponent> getConfigError() {
      return this.errorSupplier != null && this.errorSupplier.get().isPresent() ? this.errorSupplier.get()
                                                                                : this.getError();
   }

   public void setErrorSupplier(Supplier<Optional<ITextComponent>> errorSupplier) {
      this.errorSupplier = errorSupplier;
   }

   public abstract Optional<ITextComponent> getError();

   public abstract int getCellHeight();

   public abstract void render(MatrixStack var1, int var2, int var3, int var4, int var5, int var6, int var7, int var8, boolean var9, float var10);

   public void updateSelected(boolean isSelected) {
   }

   public boolean isRequiresRestart() {
      return false;
   }

   public boolean isEdited() {
      return this.getConfigError().isPresent();
   }

   public void onAdd() {
   }

   public void onDelete() {
   }
}
