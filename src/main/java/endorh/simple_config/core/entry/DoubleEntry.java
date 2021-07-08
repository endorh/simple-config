package endorh.simple_config.core.entry;

import endorh.simple_config.core.ISimpleConfigEntryHolder;
import endorh.simple_config.core.IStringKeyEntry;
import endorh.simple_config.gui.DoubleSliderBuilder;
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.impl.builders.DoubleFieldBuilder;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class DoubleEntry extends AbstractRangedEntry<Double, Number, Double, DoubleEntry>
  implements IStringKeyEntry<Double> {
	@Internal public DoubleEntry(
	  ISimpleConfigEntryHolder parent, String name, double value
	) {
		super(parent, name, value, Double.class);
	}
	
	public static class Builder extends AbstractRangedEntry.Builder<Double, Number, Double, DoubleEntry, Builder> {
		
		public Builder(Double value) {
			super(value, Double.class, "%.2f");
		}
		
		public Builder min(double min) {
			return super.min(min);
		}
		public Builder max(double max) {
			return super.max(max);
		}
		public Builder range(double min, double max) {
			return super.range(min, max);
		}
		
		@Override
		protected void checkBounds() {
			min = min == null ? Double.NEGATIVE_INFINITY : min;
			max = max == null ? Double.POSITIVE_INFINITY : max;
			if (min.isNaN() || max.isNaN())
				throw new IllegalArgumentException("NaN bound in double config entry");
			if (asSlider && (min.isInfinite() || max.isInfinite()))
				throw new IllegalArgumentException("Infinite bound in double config entry");
			super.checkBounds();
		}
		
		@Override
		public DoubleEntry buildEntry(ISimpleConfigEntryHolder parent, String name) {
			return new DoubleEntry(parent, name, value);
		}
	}
	
	@Nullable
	@Override
	protected Double fromConfig(@Nullable Number value) {
		return value != null? value.doubleValue() : null;
	}
	
	@OnlyIn(Dist.CLIENT)
	@Override
	protected Optional<AbstractConfigListEntry<Double>> buildGUIEntry(
	  ConfigEntryBuilder builder
	) {
		if (!asSlider) {
			final DoubleFieldBuilder valBuilder = builder
			  .startDoubleField(getDisplayName(), get())
			  .setDefaultValue(value)
			  .setMin(min).setMax(max)
			  .setSaveConsumer(saveConsumer())
			  .setTooltipSupplier(this::supplyTooltip)
			  .setErrorSupplier(this::supplyError);
			return Optional.of(decorate(valBuilder).build());
		} else {
			final DoubleSliderBuilder valBuilder =
			  new DoubleSliderBuilder(builder, getDisplayName(), get(), min, max)
			  .setDefaultValue(value)
			  .setSaveConsumer(saveConsumer())
			  .setTooltipSupplier(this::supplyTooltip)
			  .setErrorSupplier(this::supplyError)
			  .setTextGetter(sliderTextSupplier);
			return Optional.of(decorate(valBuilder).build());
		}
	}
	
	@Override public ITextComponent getKeySerializationError(String key) {
		return new TranslationTextComponent("text.cloth-config.error.not_valid_number_double");
	}
	
	@Override public Optional<Double> deserializeStringKey(String key) {
		try {
			return Optional.of(Double.parseDouble(key));
		} catch (NumberFormatException e) {
			return Optional.empty();
		}
	}
}
