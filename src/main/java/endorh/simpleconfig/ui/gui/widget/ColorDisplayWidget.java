package endorh.simpleconfig.ui.gui.widget;

import com.mojang.blaze3d.systems.RenderSystem;
import endorh.simpleconfig.api.ui.icon.SimpleConfigIcons;
import net.minecraft.client.GameNarrator;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ColorDisplayWidget extends AbstractWidget {
	protected TextFieldWidgetEx textFieldWidget;
	protected int color;
	protected int size;
	@Nullable public Runnable onClick = null;
	
	public ColorDisplayWidget(TextFieldWidgetEx textFieldWidget, int x, int y, int size, int color) {
		super(x, y, size, size, GameNarrator.NO_TITLE);
		this.textFieldWidget = textFieldWidget;
		this.color = color;
		this.size = size;
	}
	
	@Override public void renderWidget(@NotNull GuiGraphics gg, int mouseX, int mouseY, float delta) {
		int x = getX();
		int y = getY();
		gg.fillGradient(
		  x, y, x + size, y + size,
		  textFieldWidget.isFocused() ? -1 : 0XFFA0A0A0,
		  textFieldWidget.isFocused() ? -1 : 0XFFA0A0A0);
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.enableDepthTest();
		SimpleConfigIcons.ColorPicker.CHESS_BOARD.renderStretch(gg, x + 1, y + 1, size - 2, size - 2);
		gg.fillGradient(x + 1, y + 1, x + size - 1, y + size - 1, color, color);
	}
	
	@Override public void onClick(double mouseX, double mouseY) {
		if (onClick != null)
			onClick.run();
	}

	@Override public boolean isFocused() {
		return false;
	}

	@Override public void setFocused(boolean pFocused) {
	}
	
	public void setColor(int color) {
		this.color = color;
	}

	@Override protected void updateWidgetNarration(@NotNull NarrationElementOutput out) {}
}

