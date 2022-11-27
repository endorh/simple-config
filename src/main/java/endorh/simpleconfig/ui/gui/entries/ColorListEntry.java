package endorh.simpleconfig.ui.gui.entries;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.vertex.PoseStack;
import endorh.simpleconfig.api.ui.TextFormatter;
import endorh.simpleconfig.api.ui.math.Color;
import endorh.simpleconfig.api.ui.math.Rectangle;
import endorh.simpleconfig.ui.api.ScissorsHandler;
import endorh.simpleconfig.ui.gui.widget.ColorDisplayWidget;
import endorh.simpleconfig.ui.gui.widget.ColorPickerWidget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.network.chat.Component;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

public class ColorListEntry extends TextFieldListEntry<Integer> {
	protected final ColorDisplayWidget colorDisplayWidget;
	protected boolean alpha;
	protected ColorPickerWidget colorPicker;
	protected int last;
	protected boolean canShowColorPicker = true;
	protected boolean colorPickerVisible = false;
	protected Rectangle colorPickerRectangle = new Rectangle();
	protected Rectangle reportedColorPickerRectangle = new Rectangle();
	protected List<GuiEventListener> widgetsWithColorPicker;
	protected List<GuiEventListener> childWidgetsWithColorPicker;
	
	@Internal public ColorListEntry(Component fieldName, int value) {
		super(fieldName, 0, false);
		alpha = true;
		ColorValue colorValue = getColorValue(String.valueOf(value));
		ColorError error = colorValue.getError();
		if (error != null) throw new IllegalArgumentException("Invalid Color: " + error.name());
		alpha = false;
		// this.textFieldWidget.setText(this.getHexColorString(value));
		colorDisplayWidget = new ColorDisplayWidget(textFieldWidget, 0, 0, 20, value);
		colorDisplayWidget.onClick = () -> setColorPickerVisible(!isColorPickerVisible());
		colorPicker = new ColorPickerWidget(
		  Color.ofTransparent(value), 48, 24, 142, 84, c -> {
			  if (isEditable()) {
				  setDisplayedValue(alpha ? c.getColor() : c.getRGB());
				  last = getDisplayedValue();
			  }
		  });
		setOriginal(value);
		widgets.add(colorDisplayWidget);
		childWidgets.add(colorDisplayWidget);
		widgetsWithColorPicker = new LinkedList<>(widgets);
		widgetsWithColorPicker.add(colorPicker);
		childWidgetsWithColorPicker = new LinkedList<>(childWidgets);
		childWidgetsWithColorPicker.add(colorPicker);
		setTextFormatter(TextFormatter.forColor());
		last = value;
	}
	
	public boolean canShowColorPicker() {
		return canShowColorPicker && isEditable();
	}
	
	public void setCanShowColorPicker(boolean canShowColorPicker) {
		this.canShowColorPicker = canShowColorPicker;
		if (!canShowColorPicker) setColorPickerVisible(false);
	}
	
	public void setColorPickerVisible(boolean visible) {
		if (visible && !canShowColorPicker()) visible = false;
		colorPickerVisible = visible;
		if (visible) {
			if (this.isChildSubEntry()) colorPicker.setInitial(toColor(getDisplayedValue()));
			getScreen().addOverlay(reportedColorPickerRectangle, this, -1);
		}
	}
	
	public Color toColor(int color) {
		return alpha? Color.ofTransparent(color) : Color.ofOpaque(color);
	}
	
	public boolean isColorPickerVisible() {
		return colorPickerVisible;
	}
	
	@Override public void renderChildEntry(
	  PoseStack mStack, int x, int y, int w, int h, int mouseX, int mouseY, float delta
	) {
		if (last != getDisplayedValue()) {
			last = getDisplayedValue();
			colorPicker.setValue(toColor(getDisplayedValue()), true);
		}
		if (!isFocused() || !canShowColorPicker()) setColorPickerVisible(false);
		final Minecraft mc = Minecraft.getInstance();
		final Window window = mc.getWindow();
		final Font font = mc.font;
		colorDisplayWidget.y = y;
		ColorValue value = getColorValue(getText());
		if (!value.hasError()) {
			colorDisplayWidget.setColor(
			  alpha ? value.getColor() : 0xFF000000 | value.getColor());
		}
		// An offset of 3 matches the visual offset with the reset button (2 + 1 (border))
		final int offset = colorDisplayWidget.getWidth() + 3;
		super.renderChildEntry(mStack, font.isBidirectional()? x : x + offset, y, w - offset, h, mouseX, mouseY, delta);
		colorDisplayWidget.x = x;
		colorDisplayWidget.render(mStack, mouseX, mouseY, delta);
		if (isColorPickerVisible()) {
			final int cpw = colorPicker.getWidth(), cph = colorPicker.getHeight();
			final int ww = window.getGuiScaledWidth();
			colorPicker.y = y;
			colorPicker.x = x + w / 2 < ww / 2? x + w + 3 : x - cpw - 3;
			if (colorPicker.x < 4 || colorPicker.x + cpw > ww - 4) { // Wrap
				colorPicker.y = y + h + 3;
				colorPicker.x = x + w / 2 < ww / 2? x : x + w - cpw;
			}
			colorPickerRectangle.setBounds(colorPicker.x, colorPicker.y, cpw, cph);
			reportedColorPickerRectangle.setBounds(
			  ScissorsHandler.INSTANCE.getScissorsAreas().stream()
			    .reduce(colorPickerRectangle, Rectangle::intersection));
		}
	}
	
	@Override public boolean renderOverlay(
	  PoseStack mStack, Rectangle area, int mouseX, int mouseY, float delta
	) {
		if (area != reportedColorPickerRectangle)
			return super.renderOverlay(mStack, area, mouseX, mouseY, delta);
		if (!isColorPickerVisible())
			return false;
		getScreen().removeTooltips(area);
		colorPicker.render(mStack, mouseX, mouseY, delta);
		return true;
	}
	
	@Override public boolean overlayMouseClicked(Rectangle area, double mouseX, double mouseY, int button) {
		if (area != reportedColorPickerRectangle)
			return super.overlayMouseClicked(area, mouseX, mouseY, button);
		return colorPicker.isMouseOver(mouseX, mouseY) &&
		       colorPicker.mouseClicked(mouseX, mouseY, button);
	}
	
	@Override public void overlayMouseClickedOutside(Rectangle area, double mouseX, double mouseY, int button) {
		super.overlayMouseClickedOutside(area, mouseX, mouseY, button);
		if (!colorDisplayWidget.isMouseOver(mouseX, mouseY)) setColorPickerVisible(false);
	}
	
	@Override public boolean isOverlayDragging() {
		return true;
	}
	
	@Override public boolean overlayMouseDragged(
	  Rectangle area, double mouseX, double mouseY, int button, double dragX, double dragY
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
	
	@Override public void updateFocused(boolean isFocused) {
		super.updateFocused(isFocused);
		if (!isFocused)
			setColorPickerVisible(false);
	}
	
	@Override protected void acquireFocus() {
		super.acquireFocus();
		textFieldWidget.setAnchorPos(1);
	}
	
	@Override public void setOriginal(@Nullable Integer original) {
		super.setOriginal(original);
		if (original != null && colorPicker != null)
			colorPicker.setInitial(toColor(original));
	}
	
	@Override public int getExtraScrollHeight() {
		return isColorPickerVisible() ? colorDisplayWidget.getHeight() + colorPickerRectangle.y - entryArea.y : -1;
	}
	
	@Override protected Integer fromString(String s) {
		return getColorValueColor(s);
	}
	
	@Override protected String toString(Integer value) {
		return getHexColorString(value);
	}
	
	@Internal @Override public Optional<Component> getErrorMessage() {
		ColorValue colorValue = getColorValue(getText());
		ColorError error = colorValue.getError();
		if (error != null) {
			return Optional.of(Component.translatable("simpleconfig.config.error.invalid_color", getText()));
		}
		return super.getErrorMessage();
	}
	
	public void withAlpha() {
		if (!alpha) {
			alpha = true;
			colorPicker.allowAlpha(true);
			colorPicker.setWidth(142 + 16);
		}
		final Integer original = getOriginal();
		setOriginal(original);
		setValue(original);
		setDisplayedValue(original);
	}
	
	public void withoutAlpha() {
		if (alpha) {
			alpha = false;
			colorPicker.allowAlpha(false);
			colorPicker.setWidth(142);
		}
		final Integer original = getOriginal();
		setOriginal(original);
		setValue(original);
		setDisplayedValue(original);
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
	
	@Override protected @NotNull List<? extends GuiEventListener> getEntryListeners() {
		if (this.isChildSubEntry())
			return isColorPickerVisible()? childWidgetsWithColorPicker : childWidgets;
		return isColorPickerVisible()? widgetsWithColorPicker : widgets;
	}
}
