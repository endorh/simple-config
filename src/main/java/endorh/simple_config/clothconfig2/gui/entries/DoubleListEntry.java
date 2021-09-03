package endorh.simple_config.clothconfig2.gui.entries;

import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.ApiStatus.Internal;

import java.util.Optional;
import java.util.function.Function;

@OnlyIn(value = Dist.CLIENT)
public class DoubleListEntry extends TextFieldListEntry<Double> implements IRangedEntry<Double> {
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
	private double minimum = Double.MIN_VALUE;
	private double maximum = Double.MAX_VALUE;
	
	@Internal public DoubleListEntry(ITextComponent fieldName, Double value) {
		super(fieldName, value, false);
	}
	
	@Override
	protected String stripAddText(String s) {
		return stripCharacters.apply(s);
	}
	
	@Override
	protected void textFieldPreRender(TextFieldWidget widget) {
		try {
			double i = Double.parseDouble(widget.getText());
			if (i < this.minimum || i > this.maximum) {
				widget.setTextColor(0xFF5555);
			} else {
				widget.setTextColor(0xE0E0E0);
			}
		} catch (NumberFormatException ex) {
			widget.setTextColor(0xFF5555);
		}
	}
	
	@Override public void setMinimum(Double minimum) {
		this.minimum = minimum != null ? minimum : Double.NEGATIVE_INFINITY;
	}
	
	@Override public void setMaximum(Double maximum) {
		this.maximum = maximum != null ? maximum : Double.POSITIVE_INFINITY;
	}
	
	@Override public Double fromString(String s) {
		try {
			return Double.valueOf(s);
		} catch (NumberFormatException e) {
			return 0.0;
		}
	}
	
	@Internal @Override public Optional<ITextComponent> getError() {
		try {
			double i = Double.parseDouble(getText());
			if (i > this.maximum)
				return Optional.of(new TranslationTextComponent("text.cloth-config.error.too_large", this.maximum));
			if (i < this.minimum)
				return Optional.of(new TranslationTextComponent("text.cloth-config.error.too_small", this.minimum));
		} catch (NumberFormatException ex) {
			return Optional.of(
			  new TranslationTextComponent("text.cloth-config.error.not_valid_number_double"));
		}
		return super.getError();
	}
}

