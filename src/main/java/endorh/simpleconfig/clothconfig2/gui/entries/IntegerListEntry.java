package endorh.simpleconfig.clothconfig2.gui.entries;

import endorh.simpleconfig.clothconfig2.api.ITextFormatter;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

@OnlyIn(value = Dist.CLIENT)
public class IntegerListEntry
  extends TextFieldListEntry<Integer> implements IRangedEntry<Integer> {
	private int minimum = Integer.MIN_VALUE;
	private int maximum = Integer.MAX_VALUE;
	
	@Internal public IntegerListEntry(ITextComponent fieldName, Integer value) {
		super(fieldName, value, false);
		setTextFormatter(ITextFormatter.numeric(true));
	}
	
	@Override public void setMinimum(Integer minimum) {
		this.minimum = minimum != null ? minimum : Integer.MIN_VALUE;
	}
	
	@Override public void setMaximum(Integer maximum) {
		this.maximum = maximum != null ? maximum : Integer.MAX_VALUE;
	}
	
	@Override
	public @Nullable Integer fromString(String s) {
		try {
			return Integer.valueOf(s);
		} catch (NumberFormatException e) {
			return null;
		}
	}
	
	@Internal @Override public Optional<ITextComponent> getErrorMessage() {
		try {
			int i = Integer.parseInt(getText());
			if (i > this.maximum)
				return Optional.of(new TranslationTextComponent("text.cloth-config.error.too_large", this.maximum));
			if (i < this.minimum)
				return Optional.of(new TranslationTextComponent("text.cloth-config.error.too_small", this.minimum));
		} catch (NumberFormatException ex) {
			return Optional.of(
			  new TranslationTextComponent("text.cloth-config.error.not_valid_number_int"));
		}
		return super.getErrorMessage();
	}
}

