package endorh.simple_config.clothconfig2.gui.widget;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.button.ImageButton;
import net.minecraft.util.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public class ToggleImageButton extends ImageButton {
	
	protected boolean toggle;
	protected ResourceLocation texture;
	public int u;
	public int v;
	protected int hoverOverlayColor = 0x42FFFFFF;
	protected int borderColor = 0xffe0e0e0;
	protected Consumer<Boolean> onChange;
	
	public ToggleImageButton(
	  boolean value, int x, int y, int width, int height, int u, int v,
	  ResourceLocation texture, @Nullable Consumer<Boolean> onChange
	) {
		super(x, y, width, height, u, v, v, texture, b -> {});
		this.toggle = value;
		this.u = u;
		this.v = v;
		this.texture = texture;
		this.onChange = onChange;
	}
	
	public boolean getValue() {
		return toggle;
	}
	
	public void setValue(boolean value) {
		toggle = value;
		if (onChange != null)
			onChange.accept(toggle);
	}
	
	@Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (isMouseOver(mouseX, mouseY)) {
			if (button == 0) {
				setValue(!toggle);
				return true;
			} else if (button == 1) {
				setValue(Screen.hasShiftDown());
				return true;
			}
		}
		return false;
	}
	
	@Override public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (keyCode == 257 || keyCode == 32) {
			setValue(!toggle);
			return true;
		}
		return false;
	}
	
	@Override public void renderButton(
	  @NotNull MatrixStack mStack, int mouseX, int mouseY, float partialTicks
	) {
		Minecraft.getInstance().getTextureManager().bindTexture(texture);
		blit(mStack, x, y, u, toggle? v + height : v, width, height);
		if (isMouseOver(mouseX, mouseY))
			fill(mStack, x, y, x + width, y + height, hoverOverlayColor);
		if (isFocused()) {
			fill(mStack, x, y, x + width, y + 1, borderColor);
			fill(mStack, x, y + 1, x + 1, y + height - 1, borderColor);
			fill(mStack, x + width - 1, y + 1, x + width, y + height - 1, borderColor);
			fill(mStack, x, y + height - 1, x + width, y + height, borderColor);
		}
	}
}
