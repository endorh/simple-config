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
public class ClothConfigTabButton extends AbstractButton {
	protected final int index;
	protected final ClothConfigScreen screen;
	@Nullable protected final Supplier<Optional<ITextProperties[]>> descriptionSupplier;
	protected int tintColor = 0x00000000;
	
	public ClothConfigTabButton(
	  ClothConfigScreen screen, int index, int x, int y, int w, int h,
	  ITextComponent title, @Nullable Supplier<Optional<ITextProperties[]>> descriptionSupplier
	) {
		super(x, y, w, h, title);
		this.index = index;
		this.screen = screen;
		this.descriptionSupplier = descriptionSupplier;
	}
	
	public ClothConfigTabButton(
	  ClothConfigScreen screen, int index, int x, int y, int w, int h, ITextComponent title
	) {
		this(screen, index, x, y, w, h, title, null);
	}
	
	public void onPress() {
		if (this.index != -1) this.screen.selectedCategoryIndex = this.index;
		this.screen.init(Minecraft.getInstance(), this.screen.width, this.screen.height);
	}
	
	public void render(@NotNull MatrixStack mStack, int mouseX, int mouseY, float delta) {
		Optional<ITextProperties[]> tooltip;
		this.active = this.index != this.screen.selectedCategoryIndex;
		super.render(mStack, mouseX, mouseY, delta);
		if (this.isMouseOver(mouseX, mouseY) && (tooltip = this.getTooltip()).isPresent()
		    && tooltip.get().length > 0) {
			this.screen.addTooltip(Tooltip.of(new Point(mouseX, mouseY), tooltip.get()));
		}
	}
	
	@Override
	protected void renderBg(@NotNull MatrixStack mStack, @NotNull Minecraft minecraft, int mouseX, int mouseY) {
		super.renderBg(mStack, minecraft, mouseX, mouseY);
		if (tintColor != 0)
			fill(mStack, x, y, x + width - 1, y + height, tintColor);
	}
	
	protected boolean clicked(double mouseX, double mouseY) {
		return this.visible && this.active && this.isMouseOver(mouseX, mouseY);
	}
	
	public boolean isMouseOver(double mouseX, double mouseY) {
		return this.visible && mouseX >= (double) this.x && mouseY >= (double) this.y &&
		       mouseX < (double) (this.x + this.width) &&
		       mouseY < (double) (this.y + this.height) && mouseX >= 20.0 &&
		       mouseX < (double) (this.screen.width - 20);
	}
	
	public Optional<ITextProperties[]> getTooltip() {
		return this.descriptionSupplier != null
		       ? this.descriptionSupplier.get() : Optional.empty();
	}
	
	public int getTintColor() {
		return tintColor;
	}
	
	public void setTintColor(int color) {
		this.tintColor = color;
	}
}

