package endorh.simple_config.clothconfig2.gui.entries;

import endorh.simple_config.clothconfig2.gui.entries.IntegerListListEntry.IntegerListCell;
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
public class IntegerListListEntry
  extends AbstractTextFieldListListEntry<Integer, IntegerListCell, IntegerListListEntry> {
	private int minimum = Integer.MIN_VALUE;
	private int maximum = Integer.MAX_VALUE;
	
	@Deprecated
	@ApiStatus.Internal
	public IntegerListListEntry(
	  ITextComponent fieldName, List<Integer> value, boolean defaultExpanded,
	  Supplier<Optional<ITextComponent[]>> tooltipSupplier, Consumer<List<Integer>> saveConsumer,
	  Supplier<List<Integer>> defaultValue, ITextComponent resetButtonKey
	) {
		this(
		  fieldName, value, defaultExpanded, tooltipSupplier, saveConsumer, defaultValue,
		  resetButtonKey, false);
	}
	
	@Deprecated
	@ApiStatus.Internal
	public IntegerListListEntry(
	  ITextComponent fieldName, List<Integer> value, boolean defaultExpanded,
	  Supplier<Optional<ITextComponent[]>> tooltipSupplier, Consumer<List<Integer>> saveConsumer,
	  Supplier<List<Integer>> defaultValue, ITextComponent resetButtonKey, boolean requiresRestart
	) {
		this(
		  fieldName, value, defaultExpanded, tooltipSupplier, saveConsumer, defaultValue,
		  resetButtonKey, requiresRestart, true, true);
	}
	
	@Deprecated
	@ApiStatus.Internal
	public IntegerListListEntry(
	  ITextComponent fieldName, List<Integer> value, boolean defaultExpanded,
	  Supplier<Optional<ITextComponent[]>> tooltipSupplier, Consumer<List<Integer>> saveConsumer,
	  Supplier<List<Integer>> defaultValue, ITextComponent resetButtonKey, boolean requiresRestart,
	  boolean deleteButtonEnabled, boolean insertInFront
	) {
		super(
		  fieldName, value, defaultExpanded, tooltipSupplier, saveConsumer, defaultValue,
		  resetButtonKey, requiresRestart, deleteButtonEnabled, insertInFront, IntegerListCell::new);
	}
	
	public IntegerListListEntry setMaximum(int maximum) {
		this.maximum = maximum;
		return this;
	}
	
	public IntegerListListEntry setMinimum(int minimum) {
		this.minimum = minimum;
		return this;
	}
	
	@Override
	public IntegerListListEntry self() {
		return this;
	}
	
	public static class IntegerListCell extends
	                                    AbstractTextFieldListListEntry.AbstractTextFieldListCell<Integer, IntegerListCell, IntegerListListEntry> {
		public IntegerListCell(Integer value, IntegerListListEntry listListEntry) {
			super(value, listListEntry);
		}
		
		@Override
		@Nullable
		protected Integer substituteDefault(@Nullable Integer value) {
			if (value == null) {
				return 0;
			}
			return value;
		}
		
		@Override
		protected boolean isValidText(@NotNull String text) {
			return text.chars().allMatch(c -> Character.isDigit(c) || c == 45);
		}
		
		@Override
		public Integer getValue() {
			try {
				return Integer.valueOf(this.widget.getText());
			} catch (NumberFormatException e) {
				return 0;
			}
		}
		
		@Override
		public Optional<ITextComponent> getError() {
			try {
				int i = Integer.parseInt(this.widget.getText());
				if (i > this.listListEntry.maximum) {
					return Optional.of(new TranslationTextComponent("text.cloth-config.error.too_large",
					                                                this.listListEntry.maximum));
				}
				if (i < this.listListEntry.minimum) {
					return Optional.of(new TranslationTextComponent("text.cloth-config.error.too_small",
					                                                this.listListEntry.minimum));
				}
			} catch (NumberFormatException ex) {
				return Optional.of(
				  new TranslationTextComponent("text.cloth-config.error.not_valid_number_int"));
			}
			return Optional.empty();
		}
	}
}

