package endorh.simpleconfig.clothconfig2.gui.widget;

import com.mojang.blaze3d.matrix.MatrixStack;
import endorh.simpleconfig.clothconfig2.gui.Icon;
import endorh.simpleconfig.clothconfig2.gui.SimpleConfigIcons;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.util.IReorderingProcessor;
import net.minecraft.util.text.ITextComponent;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Consumer;

public class CheckboxButton extends ToggleImageButton {
	
	protected @Nullable ITextComponent label;
	protected int textColor = 0xffe0e0e0;
	protected int realWidth;
	
	public CheckboxButton(
	  boolean value, int x, int y, int w, @Nullable ITextComponent label,
	  @Nullable Consumer<Boolean> onChange
	) {
		this(value, x, y, w, 18, SimpleConfigIcons.CHECKBOX, label, onChange);
	}
	
	public CheckboxButton(
	  boolean value, int x, int y, int w, int h, Icon icon,
	  @Nullable ITextComponent label, @Nullable Consumer<Boolean> onChange
	) {
		super(value, x, y, w, h, icon, onChange);
		this.label = label;
		this.realWidth = width;
	}
	
	@Override protected int getYImage(boolean isHovered) {
		return isToggle()? 1 : 0;
	}
	
	@Override public void renderButton(
	  @NotNull MatrixStack mStack, int mouseX, int mouseY, float delta
	) {
		final int w = width;
		width = 18;
		super.renderButton(mStack, mouseX, mouseY, delta);
		width = w;
		if (label != null) {
			final FontRenderer font = Minecraft.getInstance().font;
			final List<IReorderingProcessor> lines =
			  font.split(label, width - 24);
			if (!lines.isEmpty())
				font.drawShadow(mStack, lines.get(0), x + 22, y + 6, textColor);
		}
	}
	
	@Override public boolean isMouseOver(double mouseX, double mouseY) {
		return mouseX >= x && mouseX < x + realWidth && mouseY >= y && mouseY < y + height;
	}
	
	@Override public void setWidth(int width) {
		super.setWidth(width);
		realWidth = width;
	}
	
	@Override public void setHeight(int value) {
		super.setHeight(18);
	}
	
	@Nullable public ITextComponent getLabel() {
		return label;
	}
	
	public void setLabel(@Nullable ITextComponent label) {
		this.label = label;
	}
	
	public int getTextColor() {
		return textColor;
	}
	
	public void setTextColor(int textColor) {
		this.textColor = textColor;
	}
}
