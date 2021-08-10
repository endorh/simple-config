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
public class DoubleListEntry extends TextFieldListEntry<Double> {
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
	private double minimum = -1.7976931348623157E308;
	private double maximum = Double.MAX_VALUE;
	private final Consumer<Double> saveConsumer;
	
	@Deprecated
	@ApiStatus.Internal
	public DoubleListEntry(
	  ITextComponent fieldName, Double value, ITextComponent resetButtonKey,
	  Supplier<Double> defaultValue, Consumer<Double> saveConsumer
	) {
		super(fieldName, value, resetButtonKey, defaultValue);
		this.saveConsumer = saveConsumer;
	}
	
	@Deprecated
	@ApiStatus.Internal
	public DoubleListEntry(
	  ITextComponent fieldName, Double value, ITextComponent resetButtonKey,
	  Supplier<Double> defaultValue, Consumer<Double> saveConsumer,
	  Supplier<Optional<ITextComponent[]>> tooltipSupplier
	) {
		this(fieldName, value, resetButtonKey, defaultValue, saveConsumer, tooltipSupplier, false);
	}
	
	@Deprecated
	@ApiStatus.Internal
	public DoubleListEntry(
	  ITextComponent fieldName, Double value, ITextComponent resetButtonKey,
	  Supplier<Double> defaultValue, Consumer<Double> saveConsumer,
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
			double i = Double.parseDouble(this.textFieldWidget.getText());
			if (i < this.minimum || i > this.maximum) {
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
	
	@Override
	public void save() {
		if (this.saveConsumer != null) {
			this.saveConsumer.accept(this.getValue());
		}
	}
	
	public DoubleListEntry setMinimum(double minimum) {
		this.minimum = minimum;
		return this;
	}
	
	public DoubleListEntry setMaximum(double maximum) {
		this.maximum = maximum;
		return this;
	}
	
	@Override
	public Double getValue() {
		try {
			return Double.valueOf(this.textFieldWidget.getText());
		} catch (NumberFormatException e) {
			return 0.0;
		}
	}
	
	@Override
	public Optional<ITextComponent> getError() {
		try {
			double i = Double.parseDouble(this.textFieldWidget.getText());
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
			  new TranslationTextComponent("text.cloth-config.error.not_valid_number_double"));
		}
		return super.getError();
	}
}

