package endorh.simpleconfig.ui.gui.widget;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.function.BiPredicate;

public class MultiFunctionButton extends TintedButton {
	
	protected BiPredicate<Button, Integer> pressAction;
	
	public MultiFunctionButton(
	  int x, int y, int width, int height, Component title,
	  BiPredicate<Button, Integer> pressAction
	) {
		super(x, y, width, height, title, b -> {});
		this.pressAction = pressAction;
	}
	
	public MultiFunctionButton(
	  int x, int y, int width, int height, Component title,
	  BiPredicate<Button, Integer> pressAction, CreateNarration createNarration
	) {
		super(x, y, width, height, title, b -> {}, createNarration);
		this.pressAction = pressAction;
	}
	
	public boolean onPress(int button) {
		return pressAction.test(this, button);
	}
	
	@Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (active && visible && clicked(mouseX, mouseY) && onPress(button)) {
			playDownSound(Minecraft.getInstance().getSoundManager());
			return true;
		}
		return false;
	}
	
	@Override public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (active && visible) {
			if (keyCode != 257 && keyCode != 32 && keyCode != 335) { // !(Enter | Space | NumPadEnter)
				return false;
			} else {
				int button = Screen.hasControlDown()? 2 : Screen.hasShiftDown()? 1 : 0;
				if (onPress(button)) {
					playDownSound(Minecraft.getInstance().getSoundManager());
					return true;
				} else if (button != 0 && onPress(0)) {
					playDownSound(Minecraft.getInstance().getSoundManager());
					return true;
				}
			}
		}
		return false;
	}
}
