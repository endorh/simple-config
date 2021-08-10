package endorh.simple_config.clothconfig2.gui.entries;

import endorh.simple_config.clothconfig2.gui.entries.LongListListEntry.LongListCell;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

@OnlyIn(value = Dist.CLIENT)
public class LongListListEntry extends
                               AbstractTextFieldListListEntry<Long, LongListCell, LongListListEntry> {
	private long minimum = Long.MIN_VALUE;
	private long maximum = Long.MAX_VALUE;
	
	@Deprecated
	@ApiStatus.Internal
	public LongListListEntry(
	  ITextComponent fieldName, List<Long> value, boolean defaultExpanded,
	  Supplier<Optional<ITextComponent[]>> tooltipSupplier, Consumer<List<Long>> saveConsumer,
	  Supplier<List<Long>> defaultValue, ITextComponent resetButtonKey
	) {
		this(
		  fieldName, value, defaultExpanded, tooltipSupplier, saveConsumer, defaultValue,
		  resetButtonKey, false);
	}
	
	@Deprecated
	@ApiStatus.Internal
	public LongListListEntry(
	  ITextComponent fieldName, List<Long> value, boolean defaultExpanded,
	  Supplier<Optional<ITextComponent[]>> tooltipSupplier, Consumer<List<Long>> saveConsumer,
	  Supplier<List<Long>> defaultValue, ITextComponent resetButtonKey, boolean requiresRestart
	) {
		this(
		  fieldName, value, defaultExpanded, tooltipSupplier, saveConsumer, defaultValue,
		  resetButtonKey, requiresRestart, true, true);
	}
	
	@Deprecated
	@ApiStatus.Internal
	public LongListListEntry(
	  ITextComponent fieldName, List<Long> value, boolean defaultExpanded,
	  Supplier<Optional<ITextComponent[]>> tooltipSupplier, Consumer<List<Long>> saveConsumer,
	  Supplier<List<Long>> defaultValue, ITextComponent resetButtonKey, boolean requiresRestart,
	  boolean deleteButtonEnabled, boolean insertInFront
	) {
		super(
		  fieldName, value, defaultExpanded, tooltipSupplier, saveConsumer, defaultValue,
		  resetButtonKey, requiresRestart, deleteButtonEnabled, insertInFront, LongListCell::new);
	}
	
	public LongListListEntry setMaximum(long maximum) {
		this.maximum = maximum;
		return this;
	}
	
	public LongListListEntry setMinimum(long minimum) {
		this.minimum = minimum;
		return this;
	}
	
	@Override
	public LongListListEntry self() {
		return this;
	}
	
	public static class LongListCell
	  extends
	  AbstractTextFieldListListEntry.AbstractTextFieldListCell<Long, LongListCell, LongListListEntry> {
		public LongListCell(Long value, LongListListEntry listListEntry) {
			super(value, listListEntry);
		}
		
		@Override
		@Nullable
		protected Long substituteDefault(@Nullable Long value) {
			if (value == null) {
				return 0L;
			}
			return value;
		}
		
		@Override
		protected boolean isValidText(@NotNull String text) {
			return text.chars().allMatch(c -> Character.isDigit(c) || c == 45);
		}
		
		@Override
		public Long getValue() {
			try {
				return Long.valueOf(this.widget.getText());
			} catch (NumberFormatException e) {
				return 0L;
			}
		}
		
		@Override
		public Optional<ITextComponent> getError() {
			try {
				long l = Long.parseLong(this.widget.getText());
				if (l > this.listListEntry.maximum) {
					return Optional.of(new TranslationTextComponent("text.cloth-config.error.too_large",
					                                                this.listListEntry.maximum));
				}
				if (l < this.listListEntry.minimum) {
					return Optional.of(new TranslationTextComponent("text.cloth-config.error.too_small",
					                                                this.listListEntry.minimum));
				}
			} catch (NumberFormatException ex) {
				return Optional.of(
				  new TranslationTextComponent("text.cloth-config.error.not_valid_number_long"));
			}
			return Optional.empty();
		}
	}
}

