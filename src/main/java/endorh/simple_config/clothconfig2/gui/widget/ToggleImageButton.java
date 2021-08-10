package endorh.simple_config.clothconfig2.gui.widget;

import com.mojang.blaze3d.matrix.MatrixStack;
import endorh.simple_config.clothconfig2.gui.Icon;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.button.ImageButton;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public class ToggleImageButton extends ImageButton {
	
	private boolean toggle;
	protected Icon icon;
	protected int hoverOverlayColor = 0x42FFFFFF;
	protected int borderColor = 0xffe0e0e0;
	protected Consumer<Boolean> onChange;
	
	public ToggleImageButton(
	  boolean value, int x, int y, int width, int height, Icon icon,
	  @Nullable Consumer<Boolean> onChange
	) {
		super(x, y, width, height, icon.u, icon.v, icon.h, icon.location, b -> {});
		this.toggle = value;
		this.icon = icon;
		this.onChange = onChange;
	}
	
	public boolean getValue() {
		return isToggle();
	}
	
	public void setValue(boolean value) {
		setToggle(value);
		if (onChange != null)
			onChange.accept(isToggle());
	}
	
	@Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (isMouseOver(mouseX, mouseY)) {
			if (button == 0) {
				setValue(!isToggle());
				return true;
			} else if (button == 1) {
				setValue(Screen.hasShiftDown());
				return true;
			}
		}
		return false;
	}
	
	@Override public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (keyCode == 257 || keyCode == 32 || keyCode == 335) { // Enter | Space | NumPadEnter
			setValue(!isToggle());
			return true;
		}
		return false;
	}
	
	@Override public void renderButton(
	  @NotNull MatrixStack mStack, int mouseX, int mouseY, float partialTicks
	) {
		icon.renderStretch(mStack, x, y, width, height, isToggle() ? 1 : 0);
		if (isMouseOver(mouseX, mouseY))
			fill(mStack, x, y, x + width, y + height, hoverOverlayColor);
		if (isFocused()) {
			fill(mStack, x, y, x + width, y + 1, borderColor);
			fill(mStack, x, y + 1, x + 1, y + height - 1, borderColor);
			fill(mStack, x + width - 1, y + 1, x + width, y + height - 1, borderColor);
			fill(mStack, x, y + height - 1, x + width, y + height, borderColor);
		}
	}
	
	public boolean isToggle() {
		return toggle;
	}
	
	public void setToggle(boolean toggle) {
		this.toggle = toggle;
	}
}
