package endorh.simpleconfig.ui.gui.widget;

import endorh.simpleconfig.SimpleConfigMod;
import endorh.simpleconfig.api.ui.icon.Icon;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.function.Consumer;

public class ToggleImageButton extends ImageButton {
	
	private boolean toggle;
	protected Icon icon;
	protected @Nullable Icon tintedIcon = null;
	protected int tint = 0;
	protected int hoverOverlayColor = 0x42FFFFFF;
	protected int borderColor = 0xffe0e0e0;
	protected Consumer<Boolean> onChange;
	
	public static ToggleImageButton of(
	  boolean value, int size, Icon icon
	) {
		return of(value, size, icon, null);
	}
	
	public static ToggleImageButton of(
	  boolean value, int size, Icon icon, @Nullable Consumer<Boolean> listener
	) {
		return new ToggleImageButton(value, 0, 0, size, size, icon, listener);
	}
	
	public ToggleImageButton(
	  boolean value, int x, int y, int width, int height, Icon icon,
	  @Nullable Consumer<Boolean> onChange
	) {
		super(x, y, width, height, icon.getU(), icon.getV(), icon.h, icon.getTexture(), b -> {});
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
	
	public Consumer<Boolean> getListener() {
		return onChange;
	}
	
	public void setListener(Consumer<Boolean> listener) {
		this.onChange = listener;
	}
	
	@Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (isMouseOver(mouseX, mouseY)) {
			if (button == 0) {
				setValue(!isToggle());
				Minecraft.getInstance().getSoundManager().play(
				  SimpleSoundInstance.forUI(SimpleConfigMod.UI_TAP, 1F));
				return true;
			} else if (button == 1) {
				setValue(Screen.hasShiftDown());
				Minecraft.getInstance().getSoundManager().play(
				  SimpleSoundInstance.forUI(SimpleConfigMod.UI_TAP, 1F));
				return true;
			}
		}
		return false;
	}
	
	@Override public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_SPACE || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
			setValue(!isToggle());
			return true;
		}
		return false;
	}
	
	@Override public void renderWidget(
      @NotNull GuiGraphics gg, int mouseX, int mouseY, float partialTicks
	) {
		Icon icon = tintedIcon == null? this.icon : tintedIcon;
		int x = getX();
		int y = getY();
		icon.renderStretch(gg, x, y, width, height, isToggle() ? 1 : 0);
		if (isMouseOver(mouseX, mouseY))
			gg.fill(x, y, x + width, y + height, hoverOverlayColor);
		if (isFocused()) {
			gg.fill(x, y, x + width, y + 1, borderColor);
			gg.fill(x, y + 1, x + 1, y + height - 1, borderColor);
			gg.fill(x + width - 1, y + 1, x + width, y + height - 1, borderColor);
			gg.fill(x, y + height - 1, x + width, y + height, borderColor);
		}
	}
	
	public boolean isToggle() {
		return toggle;
	}
	
	public void setToggle(boolean toggle) {
		this.toggle = toggle;
	}
	
	public Icon getIcon() {
		return icon;
	}
	
	public void setIcon(Icon icon) {
		this.icon = icon;
		tintedIcon = tint == 0? null : icon.withTint(tint);
	}
	
	public int getTint() {
		return tint;
	}
	
	public void setTint(int tint) {
		this.tint = tint;
		tintedIcon = tint == 0? null : icon.withTint(tint);
	}
	
	public int getHoverOverlayColor() {
		return hoverOverlayColor;
	}
	
	public void setHoverOverlayColor(int hoverOverlayColor) {
		this.hoverOverlayColor = hoverOverlayColor;
	}
	
	public int getBorderColor() {
		return borderColor;
	}
	
	public void setBorderColor(int borderColor) {
		this.borderColor = borderColor;
	}
}
