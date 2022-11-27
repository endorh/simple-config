package endorh.simpleconfig.ui.gui.entries;

import endorh.simpleconfig.api.ui.TextFormatter;
import endorh.simpleconfig.ui.gui.entries.FloatListListEntry.FloatListCell;
import net.minecraft.network.chat.Component;
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
	
	@Internal public FloatListListEntry(Component fieldName, List<Float> value) {
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
			widget.setFormatter(TextFormatter.numeric(false));
		}
		
		@Override protected boolean isValidText(@NotNull String text) {
			return text.chars().allMatch(c -> Character.isDigit(c) || c == 45 || c == 46);
		}
		
		@Override public Float getValue() {
			try {
				return Float.valueOf(widget.getValue());
			} catch (NumberFormatException e) {
				return 0.0f;
			}
		}
		
		@Override public void doSetValue(Float value) {
			widget.setValue(String.valueOf(value));
		}
		
		@Override public Optional<Component> getErrorMessage() {
			try {
				float i = Float.parseFloat(widget.getValue());
				final FloatListListEntry listEntry = getListEntry();
				if (i > listEntry.maximum)
					return Optional.of(Component.translatable("simpleconfig.config.error.too_large", listEntry.maximum));
				if (i < listEntry.minimum)
					return Optional.of(Component.translatable("simpleconfig.config.error.too_small", listEntry.minimum));
			} catch (NumberFormatException ex) {
				return Optional.of(
				  Component.translatable("simpleconfig.config.error.invalid_float", widget.getValue()));
			}
			return Optional.empty();
		}
	}
}

