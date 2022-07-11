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
public class LongListEntry
  extends TextFieldListEntry<Long> implements IRangedEntry<Long> {
	private long minimum = Long.MIN_VALUE;
	private long maximum = Long.MAX_VALUE;
	
	@Internal public LongListEntry(ITextComponent fieldName, Long value) {
		super(fieldName, value, false);
		setTextFormatter(ITextFormatter.numeric(true));
	}
	
	@Override public void setMinimum(Long minimum) {
		this.minimum = minimum != null ? minimum : Long.MIN_VALUE;
	}
	
	@Override public void setMaximum(Long maximum) {
		this.maximum = maximum != null ? maximum : Long.MAX_VALUE;
	}
	
	@Override protected @Nullable Long fromString(String s) {
		try {
			return Long.valueOf(s);
		} catch (NumberFormatException e) {
			return null;
		}
	}
	
	@Internal @Override public Optional<ITextComponent> getErrorMessage() {
		try {
			long i = Long.parseLong(getText());
			if (i > this.maximum)
				return Optional.of(new TranslationTextComponent("text.cloth-config.error.too_large", this.maximum));
			if (i < this.minimum)
				return Optional.of(new TranslationTextComponent("text.cloth-config.error.too_small", this.minimum));
		} catch (NumberFormatException ex) {
			return Optional.of(
			  new TranslationTextComponent("text.cloth-config.error.not_valid_number_long"));
		}
		return super.getErrorMessage();
	}
}

