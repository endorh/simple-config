package endorh.simple_config.clothconfig2.gui.entries;

import endorh.simple_config.clothconfig2.gui.entries.FloatListListEntry.FloatListCell;
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
public class FloatListListEntry
  extends AbstractTextFieldListListEntry<Float, FloatListCell, FloatListListEntry> {
	private float minimum = Float.NEGATIVE_INFINITY;
	private float maximum = Float.POSITIVE_INFINITY;
	
	@Deprecated
	@ApiStatus.Internal
	public FloatListListEntry(
	  ITextComponent fieldName, List<Float> value, boolean defaultExpanded,
	  Supplier<Optional<ITextComponent[]>> tooltipSupplier, Consumer<List<Float>> saveConsumer,
	  Supplier<List<Float>> defaultValue, ITextComponent resetButtonKey
	) {
		this(
		  fieldName, value, defaultExpanded, tooltipSupplier, saveConsumer, defaultValue,
		  resetButtonKey, false);
	}
	
	@Deprecated
	@ApiStatus.Internal
	public FloatListListEntry(
	  ITextComponent fieldName, List<Float> value, boolean defaultExpanded,
	  Supplier<Optional<ITextComponent[]>> tooltipSupplier, Consumer<List<Float>> saveConsumer,
	  Supplier<List<Float>> defaultValue, ITextComponent resetButtonKey, boolean requiresRestart
	) {
		this(
		  fieldName, value, defaultExpanded, tooltipSupplier, saveConsumer, defaultValue,
		  resetButtonKey, requiresRestart, true, true);
	}
	
	@Deprecated
	@ApiStatus.Internal
	public FloatListListEntry(
	  ITextComponent fieldName, List<Float> value, boolean defaultExpanded,
	  Supplier<Optional<ITextComponent[]>> tooltipSupplier, Consumer<List<Float>> saveConsumer,
	  Supplier<List<Float>> defaultValue, ITextComponent resetButtonKey, boolean requiresRestart,
	  boolean deleteButtonEnabled, boolean insertInFront
	) {
		super(
		  fieldName, value, defaultExpanded, tooltipSupplier, saveConsumer, defaultValue,
		  resetButtonKey, requiresRestart, deleteButtonEnabled, insertInFront, FloatListCell::new);
	}
	
	public FloatListListEntry setMaximum(float maximum) {
		this.maximum = maximum;
		return this;
	}
	
	public FloatListListEntry setMinimum(float minimum) {
		this.minimum = minimum;
		return this;
	}
	
	public static class FloatListCell
	  extends
	  AbstractTextFieldListListEntry.AbstractTextFieldListCell<Float, FloatListCell, FloatListListEntry> {
		public FloatListCell(Float value, FloatListListEntry listListEntry) {
			super(value, listListEntry);
		}
		
		@Override
		@Nullable
		protected Float substituteDefault(@Nullable Float value) {
			if (value == null) {
				return 0.0f;
			}
			return value;
		}
		
		@Override
		protected boolean isValidText(@NotNull String text) {
			return text.chars().allMatch(c -> Character.isDigit(c) || c == 45 || c == 46);
		}
		
		@Override
		public Float getValue() {
			try {
				return Float.valueOf(this.widget.getText());
			} catch (NumberFormatException e) {
				return 0.0f;
			}
		}
		
		@Override public void setValue(Float value) {
			this.widget.setText(String.valueOf(value));
		}
		
		@Override
		public Optional<ITextComponent> getError() {
			try {
				float i = Float.parseFloat(this.widget.getText());
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
				  new TranslationTextComponent("text.cloth-config.error.not_valid_number_float"));
			}
			return Optional.empty();
		}
	}
}

