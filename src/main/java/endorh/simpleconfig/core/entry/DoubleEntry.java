package endorh.simpleconfig.core.entry;

import endorh.simpleconfig.core.IKeyEntry;
import endorh.simpleconfig.core.ISimpleConfigEntryHolder;
import endorh.simpleconfig.ui.api.AbstractConfigListEntry;
import endorh.simpleconfig.ui.api.ConfigEntryBuilder;
import endorh.simpleconfig.ui.impl.builders.DoubleFieldBuilder;
import endorh.simpleconfig.ui.impl.builders.DoubleSliderBuilder;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class DoubleEntry extends AbstractRangedEntry<Double, Number, Double, DoubleEntry>
  implements IKeyEntry<Double> {
	@Internal public DoubleEntry(
	  ISimpleConfigEntryHolder parent, String name, double value
	) {
		super(parent, name, value);
		commentMin = -Double.MAX_VALUE;
		commentMax = Double.MAX_VALUE;
	}
	
	public static class Builder extends AbstractRangedEntry.Builder<Double, Number, Double, DoubleEntry, Builder> {
		public Builder(Double value) {
			super(value, Double.class, "%.2f");
		}
		
		/**
		 * Set min (inclusive)
		 */
		public Builder min(double min) {
			return super.min(min);
		}
		
		/**
		 * Set max (inclusive)
		 */
		public Builder max(double max) {
			return super.max(max);
		}
		
		/**
		 * Set inclusive range
		 */
		public Builder range(double min, double max) {
			return super.range(min, max);
		}
		
		/**
		 * Scale the backing field of this entry by the given scale.<br>
		 * The scale is applied in both directions, when committing the field's value,
		 * the inverse of the scale is applied before saving the changes to the config.
		 *
		 * @param scale The scale by which the config value is <em>multiplied</em>
		 *              before being stored in the backing field
		 */
		public Builder fieldScale(double scale) {
			if (scale == 0D || !Double.isFinite(scale))
				throw new IllegalArgumentException("Scale must be a non-zero finite number");
			return field(d -> d * scale, d -> d / scale, Double.class);
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
		protected DoubleEntry buildEntry(ISimpleConfigEntryHolder parent, String name) {
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
	
}
