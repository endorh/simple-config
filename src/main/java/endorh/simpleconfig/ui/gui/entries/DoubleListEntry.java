package endorh.simpleconfig.ui.gui.entries;

import endorh.simpleconfig.ui.api.ITextFormatter;
import endorh.simpleconfig.ui.hotkey.HotKeyActionTypes;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.ApiStatus.Internal;

import java.util.Optional;
import java.util.stream.Stream;

@OnlyIn(value = Dist.CLIENT)
public class DoubleListEntry extends TextFieldListEntry<Double> implements IRangedEntry<Double> {
	private double minimum = Double.MIN_VALUE;
	private double maximum = Double.MAX_VALUE;
	
	@Internal public DoubleListEntry(ITextComponent fieldName, Double value) {
		super(fieldName, value, false);
		setTextFormatter(ITextFormatter.numeric(false));
		Stream.of(
		  HotKeyActionTypes.DOUBLE_ADD, HotKeyActionTypes.DOUBLE_ADD_CYCLE,
		  HotKeyActionTypes.DOUBLE_MULTIPLY, HotKeyActionTypes.DOUBLE_DIVIDE
		).forEach(hotKeyActionTypes::add);
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
			return null;
		}
	}
	
	@Internal @Override public Optional<ITextComponent> getErrorMessage() {
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
		return super.getErrorMessage();
	}
}