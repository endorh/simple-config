package endorh.simpleconfig.ui.gui.widget;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

public class TintedButton extends Button {
	
	protected int tintColor = 0x00000000;
	
	public static TintedButton of(
	  Component title, OnPress pressedAction
	) {
		return of(80, 20, title, pressedAction);
	}
	
	public static TintedButton of(
	  Component title, int tint, OnPress pressedAction
	) {
		return of(80, 20, title, tint, pressedAction);
	}
	
	public static TintedButton of(
	  int width, int height,  Component title, OnPress pressedAction
	) {
		return of(width, height, title, 0, pressedAction);
	}
	
	public static TintedButton of(
	  int width, int height, Component title, int tint, OnPress pressedAction
	) {
		TintedButton button = new TintedButton(0, 0, width, height, title, pressedAction);
		button.setTintColor(tint);
		return button;
	}
	
	public TintedButton(
	  int x, int y, int width, int height, Component title, OnPress pressedAction
	) {
		super(x, y, width, height, title, pressedAction);
	}
	
	public TintedButton(
	  int x, int y, int width, int height, Component title, OnPress pressedAction, OnTooltip onTooltip
	) {
		super(x, y, width, height, title, pressedAction, onTooltip);
	}
	
	@Override protected void renderBg(
	  @NotNull PoseStack mStack, @NotNull Minecraft minecraft, int mouseX, int mouseY
	) {
		super.renderBg(mStack, minecraft, mouseX, mouseY);
		// The 2-patch button texture blit implementation floors width to even numbers
		if (tintColor != 0) {
			fill(mStack, x, y, x + width / 2 * 2, y + height,
			     active ? tintColor : tintColor & 0xFFFFFF | (tintColor >> 24 & 0xFF) / 4 << 24);
		}
	}
	
	public int getTintColor() {
		return tintColor;
	}
	
	public void setTintColor(int color) {
		tintColor = color;
	}
}
