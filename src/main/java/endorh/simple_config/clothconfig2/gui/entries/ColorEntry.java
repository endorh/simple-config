package endorh.simple_config.clothconfig2.gui.entries;

import com.mojang.blaze3d.matrix.MatrixStack;
import endorh.simple_config.clothconfig2.gui.widget.ColorDisplayWidget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class ColorEntry
  extends TextFieldListEntry<Integer> {
	private final ColorDisplayWidget colorDisplayWidget;
	private final Consumer<Integer> saveConsumer;
	private boolean alpha;
	
	@Deprecated
	@ApiStatus.Internal
	public ColorEntry(
	  ITextComponent fieldName, int value, ITextComponent resetButtonKey,
	  Supplier<Integer> defaultValue, Consumer<Integer> saveConsumer,
	  Supplier<Optional<ITextComponent[]>> tooltipSupplier, boolean requiresRestart
	) {
		super(fieldName, 0, resetButtonKey, defaultValue, tooltipSupplier, requiresRestart);
		this.alpha = true;
		ColorValue colorValue = this.getColorValue(String.valueOf(value));
		if (colorValue.hasError()) {
			throw new IllegalArgumentException("Invalid Color: " + colorValue.getError().name());
		}
		this.alpha = false;
		this.saveConsumer = saveConsumer;
		this.original = value;
		this.textFieldWidget.setText(this.getHexColorString(value));
		this.colorDisplayWidget = new ColorDisplayWidget(this.textFieldWidget, 0, 0, 20,
		                                                 this.getColorValueColor(
		                                                   this.textFieldWidget.getText()));
	}
	
	@Override protected void onReset(Button resetButton) {
		this.textFieldWidget.setText(this.getHexColorString(defaultValue.get()));
	}
	
	@Override
	protected boolean isChanged(Integer original, String s) {
		ColorValue colorValue = this.getColorValue(s);
		return colorValue.hasError() || this.original != colorValue.color;
	}
	
	@Override
	public void render(
	  MatrixStack matrices, int index, int y, int x, int entryWidth, int entryHeight, int mouseX,
	  int mouseY, boolean isHovered, float delta
	) {
		super.render(
		  matrices, index, y, x, entryWidth, entryHeight, mouseX, mouseY, isHovered, delta);
		this.colorDisplayWidget.y = y;
		ColorValue value = this.getColorValue(this.textFieldWidget.getText());
		if (!value.hasError()) {
			this.colorDisplayWidget.setColor(
			  this.alpha ? value.getColor() : 0xFF000000 | value.getColor());
		}
		this.colorDisplayWidget.x =
		  Minecraft.getInstance().fontRenderer.getBidiFlag() ? x + this.resetButton.getWidth() +
		                                                       this.textFieldWidget.getWidth()
		                                                     : this.textFieldWidget.x - 23;
		this.colorDisplayWidget.render(matrices, mouseX, mouseY, delta);
	}
	
	@Override
	protected void textFieldPreRender(TextFieldWidget widget) {
		if (!this.getConfigError().isPresent()) {
			widget.setTextColor(0xE0E0E0);
		} else {
			widget.setTextColor(0xFF5555);
		}
	}
	
	@Override
	public void save() {
		if (this.saveConsumer != null) {
			this.saveConsumer.accept(this.getValue());
		}
	}
	
	@Override
	protected boolean isMatchDefault(String text) {
		if (!this.getDefaultValue().isPresent()) {
			return false;
		}
		ColorValue colorValue = this.getColorValue(text);
		return !colorValue.hasError() && colorValue.color == this.getDefaultValue().get();
	}
	
	@Override
	public boolean isEdited() {
		ColorValue colorValue = this.getColorValue(this.textFieldWidget.getText());
		return colorValue.hasError() || colorValue.color != this.original;
	}
	
	@Override
	public Integer getValue() {
		return this.getColorValueColor(this.textFieldWidget.getText());
	}
	
	@Override public void setValue(Integer color) {
		this.textFieldWidget.setText(this.getHexColorString(color));
	}
	
	@Override
	public Optional<ITextComponent> getError() {
		ColorValue colorValue = this.getColorValue(this.textFieldWidget.getText());
		if (colorValue.hasError()) {
			return Optional.of(new TranslationTextComponent(
			  "text.cloth-config.error.color." +
			  colorValue.getError().name().toLowerCase(Locale.ROOT)));
		}
		return super.getError();
	}
	
	public void withAlpha() {
		if (!this.alpha) {
			this.alpha = true;
			this.textFieldWidget.setText(this.getHexColorString(this.original));
		}
	}
	
	public void withoutAlpha() {
		if (this.alpha) {
			this.alpha = false;
			this.textFieldWidget.setText(this.getHexColorString(this.original));
		}
	}
	
	protected String stripHexStarter(String hex) {
		if (hex.startsWith("#")) {
			return hex.substring(1);
		}
		return hex;
	}
	
	protected boolean isValidColorString(String str) {
		return !this.getColorValue(str).hasError();
	}
	
	protected int getColorValueColor(String str) {
		return this.getColorValue(str).getColor();
	}
	
	protected ColorValue getColorValue(String str) {
		try {
			int color;
			if (str.startsWith("#")) {
				String stripHexStarter = this.stripHexStarter(str);
				if (stripHexStarter.length() > 8) {
					return ColorError.INVALID_COLOR.toValue();
				}
				if (!this.alpha && stripHexStarter.length() > 6) {
					return ColorError.NO_ALPHA_ALLOWED.toValue();
				}
				color = (int) Long.parseLong(stripHexStarter, 16);
			} else {
				color = (int) Long.parseLong(str);
			}
			int a = color >> 24 & 0xFF;
			if (!this.alpha && a > 0) {
				return ColorError.NO_ALPHA_ALLOWED.toValue();
			}
			return new ColorValue(color);
		} catch (NumberFormatException e) {
			return ColorError.INVALID_COLOR.toValue();
		}
	}
	
	protected String getHexColorString(int color) {
		return "#" + StringUtils.leftPad(Integer.toHexString(color), this.alpha ? 8 : 6, '0');
	}
	
	protected static class ColorValue {
		private int color = -1;
		@Nullable
		private ColorError error = null;
		
		public ColorValue(int color) {
			this.color = color;
		}
		
		public ColorValue(@Nullable ColorError error) {
			this.error = error;
		}
		
		public int getColor() {
			return this.color;
		}
		
		@Nullable
		public ColorError getError() {
			return this.error;
		}
		
		public boolean hasError() {
			return this.getError() != null;
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
			return this.value;
		}
	}
}

