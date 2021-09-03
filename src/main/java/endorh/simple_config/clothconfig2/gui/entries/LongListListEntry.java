package endorh.simple_config.clothconfig2.gui.entries;

import endorh.simple_config.clothconfig2.gui.entries.LongListListEntry.LongListCell;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

@OnlyIn(value = Dist.CLIENT)
public class LongListListEntry extends AbstractTextFieldListListEntry<Long, LongListCell, LongListListEntry>
  implements IRangedEntry<Long> {
	private long minimum = Long.MIN_VALUE;
	private long maximum = Long.MAX_VALUE;
	
	@Internal public LongListListEntry(ITextComponent fieldName, List<Long> value) {
		super(fieldName, value, LongListCell::new);
	}
	
	@Override public void setMinimum(Long minimum) {
		this.minimum = minimum != null ? minimum : Long.MIN_VALUE;
	}
	
	@Override public void setMaximum(Long maximum) {
		this.maximum = maximum != null ? maximum : Long.MAX_VALUE;
	}
	
	public static class LongListCell
	  extends
	  AbstractTextFieldListListEntry.AbstractTextFieldListCell<Long, LongListCell, LongListListEntry> {
		public LongListCell(LongListListEntry listListEntry) {
			super(listListEntry);
		}
		
		@Override protected boolean isValidText(@NotNull String text) {
			return text.chars().allMatch(c -> Character.isDigit(c) || c == 45);
		}
		
		@Override public Long getValue() {
			try {
				return Long.valueOf(widget.getText());
			} catch (NumberFormatException e) {
				return 0L;
			}
		}
		
		@Override public void doSetValue(Long value) {
			widget.setText(String.valueOf(value));
		}
		
		@Override public Optional<ITextComponent> getError() {
			try {
				long l = Long.parseLong(widget.getText());
				final LongListListEntry listEntry = getListEntry();
				if (l > listEntry.maximum)
					return Optional.of(new TranslationTextComponent(
					  "text.cloth-config.error.too_large", listEntry.maximum));
				if (l < listEntry.minimum)
					return Optional.of(new TranslationTextComponent(
					  "text.cloth-config.error.too_small", listEntry.minimum));
			} catch (NumberFormatException ex) {
				return Optional.of(
				  new TranslationTextComponent("text.cloth-config.error.not_valid_number_long"));
			}
			return Optional.empty();
		}
	}
}

