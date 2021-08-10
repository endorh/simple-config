package endorh.simple_config.clothconfig2.gui;

import com.mojang.blaze3d.matrix.MatrixStack;
import endorh.simple_config.clothconfig2.api.Tooltip;
import endorh.simple_config.clothconfig2.math.Point;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.widget.button.AbstractButton;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.ITextProperties;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.function.Supplier;

@OnlyIn(value = Dist.CLIENT)
public class ClothConfigTabButton
  extends AbstractButton {
	private final int index;
	private final ClothConfigScreen screen;
	@Nullable
	private final Supplier<Optional<ITextProperties[]>> descriptionSupplier;
	
	public ClothConfigTabButton(
	  ClothConfigScreen screen, int index, int int_1, int int_2, int int_3, int int_4,
	  ITextComponent string_1, @Nullable Supplier<Optional<ITextProperties[]>> descriptionSupplier
	) {
		super(int_1, int_2, int_3, int_4, string_1);
		this.index = index;
		this.screen = screen;
		this.descriptionSupplier = descriptionSupplier;
	}
	
	public ClothConfigTabButton(
	  ClothConfigScreen screen, int index, int int_1, int int_2, int int_3, int int_4,
	  ITextComponent string_1
	) {
		this(screen, index, int_1, int_2, int_3, int_4, string_1, null);
	}
	
	public void onPress() {
		if (this.index != -1) {
			this.screen.selectedCategoryIndex = this.index;
		}
		this.screen.init(Minecraft.getInstance(), this.screen.width, this.screen.height);
	}
	
	public void render(@NotNull MatrixStack matrices, int int_1, int int_2, float float_1) {
		Optional<ITextProperties[]> tooltip;
		this.active = this.index != this.screen.selectedCategoryIndex;
		super.render(matrices, int_1, int_2, float_1);
		if (this.isMouseOver(int_1, int_2) && (tooltip = this.getTooltip()).isPresent() &&
		    tooltip.get().length > 0) {
			this.screen.addTooltip(Tooltip.of(new Point(int_1, int_2), tooltip.get()));
		}
	}
	
	protected boolean clicked(double double_1, double double_2) {
		return this.visible && this.active && this.isMouseOver(double_1, double_2);
	}
	
	public boolean isMouseOver(double double_1, double double_2) {
		return this.visible && double_1 >= (double) this.x && double_2 >= (double) this.y &&
		       double_1 < (double) (this.x + this.width) &&
		       double_2 < (double) (this.y + this.height) && double_1 >= 20.0 &&
		       double_1 < (double) (this.screen.width - 20);
	}
	
	public Optional<ITextProperties[]> getTooltip() {
		if (this.descriptionSupplier != null) {
			return this.descriptionSupplier.get();
		}
		return Optional.empty();
	}
}

