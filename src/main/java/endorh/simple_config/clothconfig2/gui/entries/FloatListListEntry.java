package endorh.simple_config.clothconfig2.gui.entries;

import endorh.simple_config.clothconfig2.gui.entries.FloatListListEntry.FloatListCell;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

@OnlyIn(value = Dist.CLIENT)
public class FloatListListEntry
  extends AbstractTextFieldListListEntry<Float, FloatListCell, FloatListListEntry>
  implements IRangedEntry<Float> {
	private float minimum = Float.NEGATIVE_INFINITY;
	private float maximum = Float.POSITIVE_INFINITY;
	
	@Internal public FloatListListEntry(ITextComponent fieldName, List<Float> value) {
		super(fieldName, value, FloatListCell::new);
	}
	
	@Override public void setMinimum(Float minimum) {
		this.minimum = minimum != null ? minimum : Float.NEGATIVE_INFINITY;
	}
	
	@Override public void setMaximum(Float maximum) {
		this.maximum = maximum != null ? maximum : Float.POSITIVE_INFINITY;
	}
	
	public static class FloatListCell
	  extends AbstractTextFieldListListEntry.AbstractTextFieldListCell<Float, FloatListCell, FloatListListEntry> {
		public FloatListCell(FloatListListEntry listListEntry) {
			super(listListEntry);
		}
		
		@Override protected boolean isValidText(@NotNull String text) {
			return text.chars().allMatch(c -> Character.isDigit(c) || c == 45 || c == 46);
		}
		
		@Override public Float getValue() {
			try {
				return Float.valueOf(widget.getText());
			} catch (NumberFormatException e) {
				return 0.0f;
			}
		}
		
		@Override public void doSetValue(Float value) {
			widget.setText(String.valueOf(value));
		}
		
		@Override public Optional<ITextComponent> getError() {
			try {
				float i = Float.parseFloat(widget.getText());
				final FloatListListEntry listEntry = getListEntry();
				if (i > listEntry.maximum)
					return Optional.of(new TranslationTextComponent(
					  "text.cloth-config.error.too_large", listEntry.maximum));
				if (i < listEntry.minimum)
					return Optional.of(new TranslationTextComponent(
					  "text.cloth-config.error.too_small", listEntry.minimum));
			} catch (NumberFormatException ex) {
				return Optional.of(
				  new TranslationTextComponent("text.cloth-config.error.not_valid_number_float"));
			}
			return Optional.empty();
		}
	}
}

