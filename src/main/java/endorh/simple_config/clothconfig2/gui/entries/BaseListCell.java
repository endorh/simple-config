package endorh.simple_config.clothconfig2.gui.entries;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.gui.FocusableGui;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.Optional;
import java.util.function.Supplier;

@OnlyIn(value = Dist.CLIENT)
public abstract class BaseListCell
  extends FocusableGui {
	private Supplier<Optional<ITextComponent>> errorSupplier;
	
	public final int getPreferredTextColor() {
		return this.getConfigError().isPresent() ? 0xFF5555 : 0xE0E0E0;
	}
	
	public final Optional<ITextComponent> getConfigError() {
		if (this.errorSupplier != null && this.errorSupplier.get().isPresent()) {
			return this.errorSupplier.get();
		}
		return this.getError();
	}
	
	public void setErrorSupplier(Supplier<Optional<ITextComponent>> errorSupplier) {
		this.errorSupplier = errorSupplier;
	}
	
	public abstract Optional<ITextComponent> getError();
	
	public abstract int getCellHeight();
	
	public abstract void render(
	  MatrixStack var1, int var2, int var3, int var4, int var5, int var6, int var7, int var8,
	  boolean var9, float var10
	);
	
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

