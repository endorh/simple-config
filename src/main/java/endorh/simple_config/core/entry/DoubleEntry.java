package endorh.simple_config.core.entry;

import endorh.simple_config.clothconfig2.api.AbstractConfigListEntry;
import endorh.simple_config.clothconfig2.api.ConfigEntryBuilder;
import endorh.simple_config.clothconfig2.impl.builders.DoubleFieldBuilder;
import endorh.simple_config.clothconfig2.impl.builders.DoubleSliderBuilder;
import endorh.simple_config.core.IKeyEntry;
import endorh.simple_config.core.ISimpleConfigEntryHolder;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class DoubleEntry extends AbstractRangedEntry<Double, Number, Double, DoubleEntry>
  implements IKeyEntry<Number, Double> {
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
		
		@Override protected Builder createCopy() {
			return new Builder(value);
		}
	}
	
	@Nullable
	@Override public Double fromConfig(@Nullable Number value) {
		return value != null? value.doubleValue() : null;
	}
	
	@OnlyIn(Dist.CLIENT)
	@Override
	public Optional<AbstractConfigListEntry<Double>> buildGUIEntry(
	  ConfigEntryBuilder builder
	) {
		if (!asSlider) {
			final DoubleFieldBuilder valBuilder = builder
			  .startDoubleField(getDisplayName(), get())
			  .setMin(min).setMax(max);
			return Optional.of(decorate(valBuilder).build());
		} else {
			final DoubleSliderBuilder valBuilder =
			  new DoubleSliderBuilder(builder, getDisplayName(), get(), min, max)
			  .setTextGetter(sliderTextSupplier);
			return Optional.of(decorate(valBuilder).build());
		}
	}
	
	@Override public Optional<Number> deserializeStringKey(@NotNull String key) {
		try {
			return Optional.of(Double.parseDouble(key));
		} catch (NumberFormatException e) {
			return Optional.empty();
		}
	}
}
