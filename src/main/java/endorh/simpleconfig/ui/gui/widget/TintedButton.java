package endorh.simpleconfig.ui.gui.widget;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.util.text.ITextComponent;
import org.jetbrains.annotations.NotNull;

public class TintedButton extends Button {
	
	protected int tintColor = 0x00000000;
	
	public static TintedButton of(
	  int width, int height,  ITextComponent title, IPressable pressedAction
	) {
		return of(width, height, 0, title, pressedAction);
	}
	
	public static TintedButton of(
	  int width, int height, int tint, ITextComponent title, IPressable pressedAction
	) {
		TintedButton button = new TintedButton(0, 0, width, height, title, pressedAction);
		button.setTintColor(tint);
		return button;
	}
	
	public TintedButton(
	  int x, int y, int width, int height, ITextComponent title, IPressable pressedAction
	) {
		super(x, y, width, height, title, pressedAction);
	}
	
	public TintedButton(
	  int x, int y, int width, int height, ITextComponent title, IPressable pressedAction, ITooltip onTooltip
	) {
		super(x, y, width, height, title, pressedAction, onTooltip);
	}
	
	@Override protected void renderBg(
	  @NotNull MatrixStack mStack, @NotNull Minecraft minecraft, int mouseX, int mouseY
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
		this.tintColor = color;
	}
}
