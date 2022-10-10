package endorh.simpleconfig.core.entry;

import endorh.simpleconfig.api.ConfigEntryHolder;
import endorh.simpleconfig.api.entry.DoubleEntryBuilder;
import endorh.simpleconfig.core.BackingField.BackingFieldBinding;
import endorh.simpleconfig.core.BackingField.BackingFieldBuilder;
import endorh.simpleconfig.core.IKeyEntry;
import endorh.simpleconfig.ui.api.ConfigFieldBuilder;
import endorh.simpleconfig.ui.impl.builders.DoubleFieldBuilder;
import endorh.simpleconfig.ui.impl.builders.DoubleSliderBuilder;
import endorh.simpleconfig.ui.impl.builders.FieldBuilder;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class DoubleEntry extends AbstractRangedEntry<Double, Number, Double>
  implements IKeyEntry<Double> {
	@Internal public DoubleEntry(
	  ConfigEntryHolder parent, String name, double value
	) {
		super(parent, name, value);
		commentMin = -Double.MAX_VALUE;
		commentMax = Double.MAX_VALUE;
	}
	
	public static class Builder
	  extends AbstractRangedEntry.Builder<Double, Number, Double, DoubleEntry, DoubleEntryBuilder, Builder>
	  implements DoubleEntryBuilder {
		public Builder(Double value) {
			super(value, Double.class, "%.2f");
		}
		
		@Override @Contract(pure=true) public @NotNull DoubleEntryBuilder min(double min) {
			return super.min(min);
		}
		
		@Override @Contract(pure=true) public @NotNull DoubleEntryBuilder max(double max) {
			return super.max(max);
		}
		
		@Override @Contract(pure=true) public @NotNull DoubleEntryBuilder range(double min, double max) {
			return super.range(min, max);
		}
		
		@Override @Contract(pure=true) public @NotNull DoubleEntryBuilder fieldScale(double scale) {
			if (scale == 0D || !Double.isFinite(scale))
				throw new IllegalArgumentException("Scale must be a non-zero finite number");
			return field(d -> d * scale, d -> d / scale, Double.class);
		}
		
		@Override @Contract(pure=true) public @NotNull DoubleEntryBuilder fieldScale(String name, double scale) {
			if (scale == 0D || !Double.isFinite(scale))
				throw new IllegalArgumentException("Scale must be a non-zero finite number");
			return addField(BackingFieldBinding.withName(
			  name, BackingFieldBuilder.of(f -> f * scale, Double.class)));
		}
		
		@Override @Contract(pure=true) public @NotNull DoubleEntryBuilder addFieldScale(String suffix, double scale) {
			if (scale == 0D || !Double.isFinite(scale))
				throw new IllegalArgumentException("Scale must be a non-zero finite number");
			return addField(suffix, f -> f * scale, Double.class);
		}
		
		@Override @Contract(pure=true) public @NotNull DoubleEntryBuilder add_field_scale(String suffix, double scale) {
			if (scale == 0D || !Double.isFinite(scale))
				throw new IllegalArgumentException("Scale must be a non-zero finite number");
			return add_field(suffix, f -> f * scale, Double.class);
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
		protected DoubleEntry buildEntry(ConfigEntryHolder parent, String name) {
			return new DoubleEntry(parent, name, value);
		}
		
		@Override protected Builder createCopy() {
			return new Builder(value);
		}
	}
	
	@Nullable
	@Override public Double fromConfig(@Nullable Number value) {
		return value != null ? value.doubleValue() : null;
	}
	
	@OnlyIn(Dist.CLIENT)
	@Override public Optional<FieldBuilder<Double, ?, ?>> buildGUIEntry(
	  ConfigFieldBuilder builder
	) {
		if (!asSlider) {
			final DoubleFieldBuilder valBuilder = builder
			  .startDoubleField(getDisplayName(), get())
			  .setMin(min).setMax(max);
			return Optional.of(decorate(valBuilder));
		} else {
			final DoubleSliderBuilder valBuilder =
			  new DoubleSliderBuilder(builder, getDisplayName(), get(), min, max)
				 .setTextGetter(sliderTextSupplier);
			return Optional.of(decorate(valBuilder));
		}
	}
	
}
