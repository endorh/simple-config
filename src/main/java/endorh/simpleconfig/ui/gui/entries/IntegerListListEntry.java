package endorh.simpleconfig.ui.gui.entries;

import endorh.simpleconfig.ui.api.ITextFormatter;
import endorh.simpleconfig.ui.gui.entries.IntegerListListEntry.IntegerListCell;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

@OnlyIn(value = Dist.CLIENT)
public class IntegerListListEntry
  extends AbstractTextFieldListListEntry<Integer, IntegerListCell, IntegerListListEntry>
  implements IRangedEntry<Integer> {
	private int minimum = Integer.MIN_VALUE;
	private int maximum = Integer.MAX_VALUE;
	
	@Internal public IntegerListListEntry(ITextComponent fieldName, List<Integer> value) {
		super(fieldName, value, IntegerListCell::new);
	}
	
	@Override public void setMinimum(Integer minimum) {
		this.minimum = minimum != null ? minimum : Integer.MIN_VALUE;
	}
	
	@Override public void setMaximum(Integer maximum) {
		this.maximum = maximum != null ? maximum : Integer.MAX_VALUE;
	}
	
	public static class IntegerListCell
	  extends AbstractTextFieldListListEntry.AbstractTextFieldListCell<Integer, IntegerListCell, IntegerListListEntry> {
		public IntegerListCell(IntegerListListEntry listListEntry) {
			super(listListEntry);
			widget.setFormatter(ITextFormatter.numeric(true));
		}
		
		@Override protected boolean isValidText(@NotNull String text) {
			return text.chars().allMatch(c -> Character.isDigit(c) || c == 45);
		}
		
		@Override public Integer getValue() {
			try {
				return Integer.valueOf(widget.getText());
			} catch (NumberFormatException e) {
				return 0;
			}
		}
		
		@Override public void doSetValue(Integer value) {
			widget.setText(String.valueOf(value));
		}
		
		@Override public Optional<ITextComponent> getErrorMessage() {
			try {
				int i = Integer.parseInt(widget.getText());
				final IntegerListListEntry listEntry = getListEntry();
				if (i > listEntry.maximum)
					return Optional.of(new TranslationTextComponent(
					  "simpleconfig.config.error.too_large", listEntry.maximum));
				if (i < listEntry.minimum)
					return Optional.of(new TranslationTextComponent(
					  "simpleconfig.config.error.too_small", listEntry.minimum));
			} catch (NumberFormatException ex) {
				return Optional.of(
				  new TranslationTextComponent("simpleconfig.config.error.invalid_integer", widget.getText()));
			}
			return Optional.empty();
		}
	}
}

