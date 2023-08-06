package endorh.simpleconfig.ui.gui.widget;

import com.mojang.blaze3d.vertex.PoseStack;
import endorh.simpleconfig.api.ui.icon.Icon;
import endorh.simpleconfig.api.ui.icon.SimpleConfigIcons;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Consumer;

public class CheckboxButton extends ToggleImageButton {
	
	protected @Nullable Component label;
	protected int textColor = 0xffe0e0e0;
	protected int realWidth;
	
	public static CheckboxButton of(boolean value, @Nullable Component label) {
		return new CheckboxButton(value, 0, 0, 18, label, null);
	}
	
	public CheckboxButton(
	  boolean value, int x, int y, int w, @Nullable Component label,
	  @Nullable Consumer<Boolean> onChange
	) {
		this(value, x, y, w, 18, SimpleConfigIcons.Widgets.CHECKBOX, label, onChange);
	}
	
	public CheckboxButton(
	  boolean value, int x, int y, int w, int h, Icon icon,
	  @Nullable Component label, @Nullable Consumer<Boolean> onChange
	) {
		super(value, x, y, w, h, icon, onChange);
		this.label = label;
		this.realWidth = width;
	}
	
	@Override protected int getYImage(boolean isHovered) {
		return isToggle()? 1 : 0;
	}
	
	@Override public void renderButton(
	  @NotNull PoseStack mStack, int mouseX, int mouseY, float delta
	) {
		final int w = width;
		width = 18;
		super.renderButton(mStack, mouseX, mouseY, delta);
		width = w;
		if (label != null) {
			final Font font = Minecraft.getInstance().font;
			final List<FormattedCharSequence> lines =
			  font.split(label, width - 24);
			if (!lines.isEmpty())
				font.drawShadow(mStack, lines.get(0), getX() + 22, getY() + 6, textColor);
		}
	}
	
	@Override public boolean isMouseOver(double mouseX, double mouseY) {
		return mouseX >= getX() && mouseX < getX() + realWidth && mouseY >= getY() && mouseY < getY() + height;
	}
	
	@Override public void setWidth(int width) {
		super.setWidth(width);
		realWidth = width;
	}
	
	@Override public void setHeight(int value) {
		super.setHeight(18);
	}
	
	@Nullable public Component getLabel() {
		return label;
	}
	
	public void setLabel(@Nullable Component label) {
		this.label = label;
	}
	
	public int getTextColor() {
		return textColor;
	}
	
	public void setTextColor(int textColor) {
		this.textColor = textColor;
	}
}
