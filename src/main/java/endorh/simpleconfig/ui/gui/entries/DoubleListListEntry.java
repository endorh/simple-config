package endorh.simpleconfig.ui.gui.entries;

import endorh.simpleconfig.ui.gui.entries.DoubleListListEntry.DoubleListCell;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

@OnlyIn(value = Dist.CLIENT)
public class DoubleListListEntry
  extends AbstractTextFieldListListEntry<Double, DoubleListCell, DoubleListListEntry>
  implements IRangedEntry<Double> {
	private double minimum = Double.NEGATIVE_INFINITY;
	private double maximum = Double.POSITIVE_INFINITY;
	
	@Internal public DoubleListListEntry(
	  ITextComponent fieldName, List<Double> value
	) {
		super(fieldName, value, DoubleListCell::new);
	}
	
	@Override public void setMinimum(Double minimum) {
		this.minimum = minimum != null ? minimum : Double.NEGATIVE_INFINITY;
	}
	
	@Override public void setMaximum(Double maximum) {
		this.maximum = maximum != null ? maximum : Double.POSITIVE_INFINITY;
	}
	
	public static class DoubleListCell
	  extends AbstractTextFieldListListEntry.AbstractTextFieldListCell<Double, DoubleListCell, DoubleListListEntry> {
		
		public DoubleListCell(DoubleListListEntry listListEntry) {
			super(listListEntry);
		}
		
		@Override protected boolean isValidText(@NotNull String text) {
			return text.chars().allMatch(c -> Character.isDigit(c) || c == 45 || c == 46);
		}
		
		@Override public Double getValue() {
			try {
				return Double.valueOf(widget.getText());
			} catch (NumberFormatException e) {
				return 0.0;
			}
		}
		
		@Override public void doSetValue(Double value) {
			widget.setText(String.valueOf(value));
		}
		
		@Override public Optional<ITextComponent> getErrorMessage() {
			try {
				double i = Double.parseDouble(widget.getText());
				final DoubleListListEntry listEntry = getListEntry();
				if (i > listEntry.maximum)
					return Optional.of(new TranslationTextComponent(
					  "text.cloth-config.error.too_large", listEntry.maximum));
				if (i < listEntry.minimum)
					return Optional.of(new TranslationTextComponent(
					  "text.cloth-config.error.too_small", listEntry.minimum));
			} catch (NumberFormatException ex) {
				return Optional.of(
				  new TranslationTextComponent("text.cloth-config.error.not_valid_number_double"));
			}
			return Optional.empty();
		}
	}
}

