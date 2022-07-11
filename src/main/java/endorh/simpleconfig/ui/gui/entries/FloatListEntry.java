package endorh.simpleconfig.ui.gui.entries;

import endorh.simpleconfig.ui.api.ITextFormatter;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

@OnlyIn(value = Dist.CLIENT)
public class FloatListEntry extends TextFieldListEntry<Float> implements IRangedEntry<Float> {
	private float minimum = Float.MIN_VALUE;
	private float maximum = Float.MAX_VALUE;
	
	@Internal public FloatListEntry(
	  ITextComponent fieldName, Float value
	) {
		super(fieldName, value, false);
		setTextFormatter(ITextFormatter.numeric(false));
	}
	
	@Override public void setMinimum(Float minimum) {
		this.minimum = minimum != null ? minimum : Float.NEGATIVE_INFINITY;
	}
	
	@Override public void setMaximum(Float maximum) {
		this.maximum = maximum != null ? maximum : Float.POSITIVE_INFINITY;
	}
	
	@Override public @Nullable Float fromString(String s) {
		try {
			return Float.valueOf(s);
		} catch (NumberFormatException e) {
			return null;
		}
	}
	
	@Internal @Override public Optional<ITextComponent> getErrorMessage() {
		try {
			float i = Float.parseFloat(getText());
			if (i > this.maximum)
				return Optional.of(new TranslationTextComponent("text.cloth-config.error.too_large", this.maximum));
			if (i < this.minimum)
				return Optional.of(new TranslationTextComponent("text.cloth-config.error.too_small", this.minimum));
		} catch (NumberFormatException ex) {
			return Optional.of(
			  new TranslationTextComponent("text.cloth-config.error.not_valid_number_float"));
		}
		return super.getErrorMessage();
	}
}

