package endorh.simple_config.clothconfig2.gui.entries;

import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.ApiStatus;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

@OnlyIn(value = Dist.CLIENT)
public class FloatListEntry extends TextFieldListEntry<Float> {
	private static final Function<String, String> stripCharacters = s -> {
		StringBuilder stringBuilder_1 = new StringBuilder();
		char[] var2 = s.toCharArray();
		int var3 = var2.length;
		for (char c : var2) {
			if (!Character.isDigit(c) && c != '-' && c != '.') continue;
			stringBuilder_1.append(c);
		}
		return stringBuilder_1.toString();
	};
	private float minimum = -3.4028235E38f;
	private float maximum = Float.MAX_VALUE;
	private final Consumer<Float> saveConsumer;
	
	@Deprecated
	@ApiStatus.Internal
	public FloatListEntry(
	  ITextComponent fieldName, Float value, ITextComponent resetButtonKey,
	  Supplier<Float> defaultValue, Consumer<Float> saveConsumer
	) {
		super(fieldName, value, resetButtonKey, defaultValue);
		this.saveConsumer = saveConsumer;
	}
	
	@Deprecated
	@ApiStatus.Internal
	public FloatListEntry(
	  ITextComponent fieldName, Float value, ITextComponent resetButtonKey,
	  Supplier<Float> defaultValue, Consumer<Float> saveConsumer,
	  Supplier<Optional<ITextComponent[]>> tooltipSupplier
	) {
		this(fieldName, value, resetButtonKey, defaultValue, saveConsumer, tooltipSupplier, false);
	}
	
	@Deprecated
	@ApiStatus.Internal
	public FloatListEntry(
	  ITextComponent fieldName, Float value, ITextComponent resetButtonKey,
	  Supplier<Float> defaultValue, Consumer<Float> saveConsumer,
	  Supplier<Optional<ITextComponent[]>> tooltipSupplier, boolean requiresRestart
	) {
		super(fieldName, value, resetButtonKey, defaultValue, tooltipSupplier, requiresRestart);
		this.saveConsumer = saveConsumer;
	}
	
	@Override
	protected String stripAddText(String s) {
		return stripCharacters.apply(s);
	}
	
	@Override
	protected void textFieldPreRender(TextFieldWidget widget) {
		try {
			double i = Float.parseFloat(this.textFieldWidget.getText());
			if (i < (double) this.minimum || i > (double) this.maximum) {
				widget.setTextColor(0xFF5555);
			} else {
				widget.setTextColor(0xE0E0E0);
			}
		} catch (NumberFormatException ex) {
			widget.setTextColor(0xFF5555);
		}
	}
	
	@Override
	protected boolean isMatchDefault(String text) {
		return this.getDefaultValue().isPresent() &&
		       text.equals(this.defaultValue.get().toString());
	}
	
	public FloatListEntry setMinimum(float minimum) {
		this.minimum = minimum;
		return this;
	}
	
	public FloatListEntry setMaximum(float maximum) {
		this.maximum = maximum;
		return this;
	}
	
	@Override
	public void save() {
		if (this.saveConsumer != null) {
			this.saveConsumer.accept(this.getValue());
		}
	}
	
	@Override
	public Float getValue() {
		try {
			return Float.valueOf(this.textFieldWidget.getText());
		} catch (NumberFormatException e) {
			return 0.0f;
		}
	}
	
	@Override
	public Optional<ITextComponent> getError() {
		try {
			float i = Float.parseFloat(this.textFieldWidget.getText());
			if (i > this.maximum) {
				return Optional.of(new TranslationTextComponent("text.cloth-config.error.too_large",
				                                                this.maximum));
			}
			if (i < this.minimum) {
				return Optional.of(new TranslationTextComponent("text.cloth-config.error.too_small",
				                                                this.minimum));
			}
		} catch (NumberFormatException ex) {
			return Optional.of(
			  new TranslationTextComponent("text.cloth-config.error.not_valid_number_float"));
		}
		return super.getError();
	}
}

