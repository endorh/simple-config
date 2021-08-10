package endorh.simple_config.clothconfig2.gui.entries;

import endorh.simple_config.clothconfig2.gui.entries.DoubleListListEntry.DoubleListCell;
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
public class DoubleListListEntry
  extends AbstractTextFieldListListEntry<Double, DoubleListCell, DoubleListListEntry> {
	private double minimum = Double.NEGATIVE_INFINITY;
	private double maximum = Double.POSITIVE_INFINITY;
	
	@Deprecated
	@ApiStatus.Internal
	public DoubleListListEntry(
	  ITextComponent fieldName, List<Double> value, boolean defaultExpanded,
	  Supplier<Optional<ITextComponent[]>> tooltipSupplier, Consumer<List<Double>> saveConsumer,
	  Supplier<List<Double>> defaultValue, ITextComponent resetButtonKey
	) {
		this(
		  fieldName, value, defaultExpanded, tooltipSupplier, saveConsumer, defaultValue,
		  resetButtonKey, false);
	}
	
	@Deprecated
	@ApiStatus.Internal
	public DoubleListListEntry(
	  ITextComponent fieldName, List<Double> value, boolean defaultExpanded,
	  Supplier<Optional<ITextComponent[]>> tooltipSupplier, Consumer<List<Double>> saveConsumer,
	  Supplier<List<Double>> defaultValue, ITextComponent resetButtonKey, boolean requiresRestart
	) {
		this(
		  fieldName, value, defaultExpanded, tooltipSupplier, saveConsumer, defaultValue,
		  resetButtonKey, requiresRestart, true, true);
	}
	
	@Deprecated
	@ApiStatus.Internal
	public DoubleListListEntry(
	  ITextComponent fieldName, List<Double> value, boolean defaultExpanded,
	  Supplier<Optional<ITextComponent[]>> tooltipSupplier, Consumer<List<Double>> saveConsumer,
	  Supplier<List<Double>> defaultValue, ITextComponent resetButtonKey, boolean requiresRestart,
	  boolean deleteButtonEnabled, boolean insertInFront
	) {
		super(
		  fieldName, value, defaultExpanded, tooltipSupplier, saveConsumer, defaultValue,
		  resetButtonKey, requiresRestart, deleteButtonEnabled, insertInFront, DoubleListCell::new);
	}
	
	public DoubleListListEntry setMaximum(Double maximum) {
		this.maximum = maximum;
		return this;
	}
	
	public DoubleListListEntry setMinimum(Double minimum) {
		this.minimum = minimum;
		return this;
	}
	
	public static class DoubleListCell
	  extends
	  AbstractTextFieldListListEntry.AbstractTextFieldListCell<Double, DoubleListCell, DoubleListListEntry> {
		public DoubleListCell(Double value, DoubleListListEntry listListEntry) {
			super(value, listListEntry);
		}
		
		@Override
		@Nullable
		protected Double substituteDefault(@Nullable Double value) {
			if (value == null) {
				return 0.0;
			}
			return value;
		}
		
		@Override
		protected boolean isValidText(@NotNull String text) {
			return text.chars().allMatch(c -> Character.isDigit(c) || c == 45 || c == 46);
		}
		
		@Override
		public Double getValue() {
			try {
				return Double.valueOf(this.widget.getText());
			} catch (NumberFormatException e) {
				return 0.0;
			}
		}
		
		@Override public void setValue(Double value) {
			this.widget.setText(String.valueOf(value));
		}
		
		@Override
		public Optional<ITextComponent> getError() {
			try {
				double i = Double.parseDouble(this.widget.getText());
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
				  new TranslationTextComponent("text.cloth-config.error.not_valid_number_double"));
			}
			return Optional.empty();
		}
	}
}

