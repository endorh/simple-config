package endorh.simple_config.clothconfig2.gui.widget;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.gui.chat.NarratorChatListener;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.Widget;
import org.jetbrains.annotations.NotNull;

public class ColorDisplayWidget extends Widget {
	protected TextFieldWidget textFieldWidget;
	protected int color;
	protected int size;
	
	public ColorDisplayWidget(TextFieldWidget textFieldWidget, int x, int y, int size, int color) {
		super(x, y, size, size, NarratorChatListener.EMPTY);
		this.textFieldWidget = textFieldWidget;
		this.color = color;
		this.size = size;
	}
	
	public void renderButton(@NotNull MatrixStack matrices, int mouseX, int mouseY, float delta) {
		this.fillGradient(
		  matrices, this.x, this.y, this.x + this.size, this.y + this.size,
		  this.textFieldWidget.isFocused() ? -1 : -6250336,
		  this.textFieldWidget.isFocused() ? -1 : -6250336);
		this.fillGradient(
		  matrices, this.x + 1, this.y + 1, this.x + this.size - 1, this.y + this.size - 1, -1, -1);
		this.fillGradient(
		  matrices, this.x + 1, this.y + 1, this.x + this.size - 1, this.y + this.size - 1,
		  this.color, this.color);
	}
	
	public void onClick(double mouseX, double mouseY) {
	}
	
	public void onRelease(double mouseX, double mouseY) {
	}
	
	public void setColor(int color) {
		this.color = color;
	}
}

