package endorh.simpleconfig.clothconfig2.gui.widget;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.util.text.ITextComponent;

import java.util.function.BiPredicate;

public class MultiFunctionButton extends TintedButton {
	
	protected BiPredicate<Button, Integer> pressAction;
	
	public MultiFunctionButton(
	  int x, int y, int width, int height, ITextComponent title,
	  BiPredicate<Button, Integer> pressAction
	) {
		super(x, y, width, height, title, b -> {});
		this.pressAction = pressAction;
	}
	
	public MultiFunctionButton(
	  int x, int y, int width, int height, ITextComponent title,
	  BiPredicate<Button, Integer> pressAction, ITooltip onTooltip
	) {
		super(x, y, width, height, title, b -> {}, onTooltip);
		this.pressAction = pressAction;
	}
	
	public boolean onPress(int button) {
		return pressAction.test(this, button);
	}
	
	@Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (this.active && this.visible && clicked(mouseX, mouseY) && onPress(button)) {
			playDownSound(Minecraft.getInstance().getSoundHandler());
			return true;
		}
		return false;
	}
	
	@Override public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (this.active && this.visible) {
			if (keyCode != 257 && keyCode != 32 && keyCode != 335) { // !(Enter | Space | NumPadEnter)
				return false;
			} else {
				int button = Screen.hasControlDown()? 2 : Screen.hasShiftDown()? 1 : 0;
				if (onPress(button)) {
					this.playDownSound(Minecraft.getInstance().getSoundHandler());
					return true;
				} else if (button != 0 && onPress(0)) {
					this.playDownSound(Minecraft.getInstance().getSoundHandler());
					return true;
				}
			}
		}
		return false;
	}
}
