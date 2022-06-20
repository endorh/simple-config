package endorh.simpleconfig.clothconfig2.gui.entries;

import com.mojang.blaze3d.matrix.MatrixStack;
import endorh.simpleconfig.clothconfig2.api.ScissorsHandler;
import endorh.simpleconfig.clothconfig2.gui.IOverlayCapableScreen.IOverlayRenderer;
import endorh.simpleconfig.clothconfig2.gui.widget.ColorDisplayWidget;
import endorh.simpleconfig.clothconfig2.gui.widget.ColorPickerWidget;
import endorh.simpleconfig.clothconfig2.math.Color;
import endorh.simpleconfig.clothconfig2.math.Rectangle;
import net.minecraft.client.MainWindow;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.IGuiEventListener;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class ColorListEntry extends TextFieldListEntry<Integer> implements IOverlayRenderer {
	protected final ColorDisplayWidget colorDisplayWidget;
	protected boolean alpha;
	protected ColorPickerWidget colorPicker;
	protected int last;
	protected boolean colorPickerVisible = false;
	protected Rectangle colorPickerRectangle = new Rectangle();
	protected Rectangle reportedColorPickerRectangle = new Rectangle();
	protected List<IGuiEventListener> widgetsWithColorPicker;
	protected List<IGuiEventListener> childWidgetsWithColorPicker;
	
	@Internal public ColorListEntry(ITextComponent fieldName, int value) {
		super(fieldName, 0, false);
		alpha = true;
		ColorValue colorValue = getColorValue(String.valueOf(value));
		if (colorValue.hasError())
			throw new IllegalArgumentException("Invalid Color: " + colorValue.getError().name());
		alpha = false;
		// this.textFieldWidget.setText(this.getHexColorString(value));
		colorDisplayWidget = new ColorDisplayWidget(
		  textFieldWidget, 0, 0, 20, this.value.get());
		colorDisplayWidget.onClick = () -> setColorPickerVisible(!isColorPickerVisible());
		colorPicker = new ColorPickerWidget(
		  Color.ofTransparent(value), 48, 24, 142, 84, c -> {
			  setValue(alpha ? c.getColor() : c.getRGB());
			  last = getValue();
		  });
		setOriginal(value);
		widgets.add(colorDisplayWidget);
		childWidgets.add(colorDisplayWidget);
		widgetsWithColorPicker = new LinkedList<>(widgets);
		widgetsWithColorPicker.add(colorPicker);
		childWidgetsWithColorPicker = new LinkedList<>(childWidgets);
		childWidgetsWithColorPicker.add(colorPicker);
		last = value;
	}
	
	public void setColorPickerVisible(boolean visible) {
		colorPickerVisible = visible;
		if (visible)
			getConfigScreen().claimRectangle(reportedColorPickerRectangle, this, -1);
	}
	
	public boolean isColorPickerVisible() {
		return colorPickerVisible;
	}
	
	@Override public void renderEntry(
	  MatrixStack mStack, int index, int y, int x, int entryWidth, int entryHeight, int mouseX,
	  int mouseY, boolean isHovered, float delta
	) {
		super.renderEntry(mStack, index, y, x, entryWidth, entryHeight, mouseX, mouseY, isHovered, delta);
	}
	
	@Override public void renderChildEntry(
	  MatrixStack mStack, int x, int y, int w, int h, int mouseX, int mouseY, float delta
	) {
		if (last != getValue()) {
			last = getValue();
			colorPicker.setValue(alpha? Color.ofTransparent(getValue()) : Color.ofOpaque(getValue()), true);
		}
		final MainWindow window = Minecraft.getInstance().getWindow();
		colorDisplayWidget.y = y;
		ColorValue value = getColorValue(getText());
		if (!value.hasError()) {
			colorDisplayWidget.setColor(
			  alpha ? value.getColor() : 0xFF000000 | value.getColor());
		}
		final int offset = colorDisplayWidget.getWidth() + 4;
		super.renderChildEntry(mStack, x + offset, y, w - offset, h, mouseX, mouseY, delta);
		final TextFieldWidget textField = this.textFieldWidget;
		colorDisplayWidget.x = textField.x - offset;
		colorDisplayWidget.render(mStack, mouseX, mouseY, delta);
		if (isColorPickerVisible()) {
			colorPicker.y = colorPickerRectangle.y = y;
			colorPicker.x = colorPickerRectangle.x =
			  colorDisplayWidget.x < window.getGuiScaledWidth() - (textField.x + textField.getWidth())
			  ? textField.x + textField.getWidth() + 4
			  : colorDisplayWidget.x - colorPicker.getWidth() - 2;
			colorPickerRectangle.width = colorPicker.getWidth();
			colorPickerRectangle.height = colorPicker.getHeight();
			reportedColorPickerRectangle.setBounds(
			  ScissorsHandler.INSTANCE.getScissorsAreas().stream()
			    .reduce(colorPickerRectangle, Rectangle::intersection));
		}
	}
	
	@Override public boolean renderOverlay(
	  MatrixStack mStack, Rectangle area, int mouseX, int mouseY, float delta
	) {
		if (!isColorPickerVisible())
			return false;
		getConfigScreen().removeTooltips(area);
		colorPicker.render(mStack, mouseX, mouseY, delta);
		return true;
	}
	
	@Override public boolean overlayMouseClicked(double mouseX, double mouseY, int button) {
		return colorPicker.isMouseOver(mouseX, mouseY) &&
		       colorPicker.mouseClicked(mouseX, mouseY, button);
	}
	
	@Override public boolean isOverlayDragging() {
		return true;
	}
	
	@Override public boolean overlayMouseDragged(
	  double mouseX, double mouseY, int button, double dragX, double dragY
	) {
		colorPicker.onDrag(mouseX, mouseY, dragX, dragY);
		return true;
	}
	
	@Override public boolean overlayEscape() {
		if (isColorPickerVisible()) {
			setColorPickerVisible(false);
			return true;
		}
		return false;
	}
	
	@Override
	protected void textFieldPreRender(TextFieldWidget widget) {
		if (!hasErrors())
			widget.setTextColor(0xE0E0E0);
		else widget.setTextColor(0xFF5555);
	}
	
	@Override public void updateSelected(boolean isSelected) {
		super.updateSelected(isSelected);
		if (!isSelected)
			setColorPickerVisible(false);
	}
	
	@Override public void setOriginal(@Nullable Integer original) {
		super.setOriginal(original);
		colorPicker.setInitial(alpha? Color.ofTransparent(getValue()) : Color.ofOpaque(getValue()));
	}
	
	@Override public int getExtraScrollHeight() {
		return isColorPickerVisible() ? colorDisplayWidget.getHeight() - 24 : -1;
	}
	
	@Override protected Integer fromString(String s) {
		return getColorValueColor(s);
	}
	
	@Override protected String toString(Integer value) {
		return getHexColorString(value);
	}
	
	@Internal @Override public Optional<ITextComponent> getErrorMessage() {
		ColorValue colorValue = getColorValue(getText());
		if (colorValue.hasError()) {
			return Optional.of(new TranslationTextComponent(
			  "text.cloth-config.error.color." +
			  colorValue.getError().name().toLowerCase(Locale.ROOT)));
		}
		return super.getErrorMessage();
	}
	
	public void withAlpha() {
		if (!alpha) {
			alpha = true;
			colorPicker.allowAlpha(true);
			colorPicker.setWidth(142 + 16);
		}
		setOriginal(original);
		setValue(original);
	}
	
	public void withoutAlpha() {
		if (alpha) {
			alpha = false;
			colorPicker.allowAlpha(false);
			colorPicker.setWidth(142);
		}
		setOriginal(original);
		setValue(original);
	}
	
	protected String stripHexStarter(String hex) {
		if (hex.startsWith("#"))
			return hex.substring(1);
		return hex;
	}
	
	protected boolean isValidColorString(String str) {
		return !getColorValue(str).hasError();
	}
	
	protected int getColorValueColor(String str) {
		return getColorValue(str).getColor();
	}
	
	protected ColorValue getColorValue(String str) {
		try {
			int color;
			if (str.startsWith("#")) {
				String stripHexStarter = stripHexStarter(str);
				if (stripHexStarter.length() > 8)
					return ColorError.INVALID_COLOR.toValue();
				if (!alpha && stripHexStarter.length() > 6)
					return ColorError.NO_ALPHA_ALLOWED.toValue();
				color = (int) Long.parseLong(stripHexStarter, 16);
			} else color = (int) Long.parseLong(str);
			int a = color >> 24 & 0xFF;
			if (!alpha && a > 0)
				return ColorError.NO_ALPHA_ALLOWED.toValue();
			return new ColorValue(color);
		} catch (NumberFormatException e) {
			return ColorError.INVALID_COLOR.toValue();
		}
	}
	
	protected String getHexColorString(int color) {
		return "#" + StringUtils.leftPad(Integer.toHexString(color), alpha ? 8 : 6, '0');
	}
	
	protected static class ColorValue {
		private int color = -1;
		@Nullable private ColorError error = null;
		
		public ColorValue(int color) {
			this.color = color;
		}
		
		public ColorValue(@Nullable ColorError error) {
			this.error = error;
		}
		
		public int getColor() {
			return color;
		}
		
		@Nullable
		public ColorError getError() {
			return error;
		}
		
		public boolean hasError() {
			return getError() != null;
		}
	}
	
	protected enum ColorError {
		NO_ALPHA_ALLOWED,
		INVALID_ALPHA,
		INVALID_RED,
		INVALID_GREEN,
		INVALID_BLUE,
		INVALID_COLOR;
		
		private final ColorValue value = new ColorValue(this);
		
		public ColorValue toValue() {
			return value;
		}
	}
	
	@Override public @NotNull List<? extends IGuiEventListener> children() {
		if (isChild())
			return isColorPickerVisible()? childWidgetsWithColorPicker : childWidgets;
		return isColorPickerVisible()? widgetsWithColorPicker : widgets;
	}
}

