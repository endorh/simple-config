package endorh.simpleconfig.core.entry;

import endorh.simpleconfig.core.IKeyEntry;
import endorh.simpleconfig.core.ISimpleConfigEntryHolder;
import endorh.simpleconfig.ui.api.AbstractConfigListEntry;
import endorh.simpleconfig.ui.api.ConfigEntryBuilder;
import endorh.simpleconfig.ui.impl.builders.FloatFieldBuilder;
import endorh.simpleconfig.ui.impl.builders.FloatSliderBuilder;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class FloatEntry extends AbstractRangedEntry<Float, Number, Float, FloatEntry>
  implements IKeyEntry<Float> {
	@Internal public FloatEntry(
	  ISimpleConfigEntryHolder parent, String name, float value
	) {
		super(parent, name, value);
		commentMin = -Float.MAX_VALUE;
		commentMax = Float.MAX_VALUE;
	}
	
	public static class Builder extends AbstractRangedEntry.Builder<Float, Number, Float, FloatEntry, Builder> {
		public Builder(Float value) {
			super(value, Float.class, "%.2f");
		}
		
		/**
		 * Set min (inclusive)
		 */
		public Builder min(float min) {
			return super.min(min);
		}
		
		/**
		 * Set max (inclusive)
		 */
		public Builder max(float max) {
			return super.max(max);
		}
		
		/**
		 * Set inclusive range
		 */
		public Builder range(float min, float max) {
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
		public Builder fieldScale(float scale) {
			if (scale == 0F || !Float.isFinite(scale))
				throw new IllegalArgumentException("Scale must be a non-zero finite number");
			return field(f -> f * scale, f -> f / scale, Float.class);
		}
		
		@Override
		protected void checkBounds() {
			min = min == null ? Float.NEGATIVE_INFINITY : min;
			max = max == null ? Float.POSITIVE_INFINITY : max;
			if (min.isNaN() || max.isNaN())
				throw new IllegalArgumentException("NaN bound in float config entry");
			if (asSlider && (min.isInfinite() || max.isInfinite()))
				throw new IllegalArgumentException("Infinite bound in float config entry");
			super.checkBounds();
		}
		
		@Override
		protected FloatEntry buildEntry(ISimpleConfigEntryHolder parent, String name) {
			return new FloatEntry(parent, name, value);
		}
		
		@Override protected Builder createCopy() {
			return new Builder(value);
		}
	}
	
	@Nullable
	@Override public Float fromConfig(@Nullable Number value) {
		return value != null ? value.floatValue() : null;
	}
	
	@OnlyIn(Dist.CLIENT)
	@Override
	public Optional<AbstractConfigListEntry<Float>> buildGUIEntry(
	  ConfigEntryBuilder builder
	) {
		if (!asSlider) {
			final FloatFieldBuilder valBuilder = builder
			  .startFloatField(getDisplayName(), get())
			  .setMin(min).setMax(max);
			return Optional.of(decorate(valBuilder).build());
		} else {
			final FloatSliderBuilder valBuilder =
			  new FloatSliderBuilder(builder, getDisplayName(), get(), min, max)
				 .setTextGetter(sliderTextSupplier);
			return Optional.of(decorate(valBuilder).build());
		}
	}
	
}
