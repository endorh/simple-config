package endorh.simpleconfig.ui.gui.entries;

import endorh.simpleconfig.api.ui.TextFormatter;
import endorh.simpleconfig.ui.gui.entries.LongListListEntry.LongListCell;
import net.minecraft.network.chat.Component;
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
	
	@Internal public LongListListEntry(Component fieldName, List<Long> value) {
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
			widget.setFormatter(TextFormatter.numeric(true));
		}
		
		@Override protected boolean isValidText(@NotNull String text) {
			return text.chars().allMatch(c -> Character.isDigit(c) || c == 45);
		}
		
		@Override public Long getValue() {
			try {
				return Long.valueOf(widget.getValue());
			} catch (NumberFormatException e) {
				return 0L;
			}
		}
		
		@Override public void doSetValue(Long value) {
			widget.setValue(String.valueOf(value));
		}
		
		@Override public Optional<Component> getErrorMessage() {
			try {
				long l = Long.parseLong(widget.getValue());
				final LongListListEntry listEntry = getListEntry();
				if (l > listEntry.maximum)
					return Optional.of(Component.translatable("simpleconfig.config.error.too_large", listEntry.maximum));
				if (l < listEntry.minimum)
					return Optional.of(Component.translatable("simpleconfig.config.error.too_small", listEntry.minimum));
			} catch (NumberFormatException ex) {
				return Optional.of(
				  Component.translatable("simpleconfig.config.error.invalid_integer", widget.getValue()));
			}
			return Optional.empty();
		}
	}
}

