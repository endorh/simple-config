package endorh.simple_config.core.entry;

import endorh.simple_config.clothconfig2.api.AbstractConfigListEntry;
import endorh.simple_config.clothconfig2.api.ConfigEntryBuilder;
import endorh.simple_config.clothconfig2.impl.builders.FloatFieldBuilder;
import endorh.simple_config.clothconfig2.impl.builders.FloatSliderBuilder;
import endorh.simple_config.core.IKeyEntry;
import endorh.simple_config.core.ISimpleConfigEntryHolder;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class FloatEntry extends AbstractRangedEntry<Float, Number, Float, FloatEntry>
  implements IKeyEntry<Number, Float> {
	protected float fieldScale = 1F;
	
	@Internal public FloatEntry(
	  ISimpleConfigEntryHolder parent, String name, float value
	) {
		super(parent, name, value);
		commentMin = Float.MIN_VALUE;
		commentMax = Float.MAX_VALUE;
	}
	
	public static class Builder extends
	                            AbstractRangedEntry.Builder<Float, Number, Float, FloatEntry, Builder> {
		protected float fieldScale = 1F;
		
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
			final Builder copy = copy();
			copy.fieldScale = scale;
			return copy;
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
			final FloatEntry entry = new FloatEntry(parent, name, value);
			entry.fieldScale = fieldScale;
			return entry;
		}
		
		@Override protected Builder createCopy() {
			final Builder copy = new Builder(value);
			copy.fieldScale = fieldScale;
			return copy;
		}
	}
	
	@Nullable
	@Override public Float fromConfig(@Nullable Number value) {
		return value != null ? value.floatValue() : null;
	}
	
	@Override protected void setBackingField(Float value) throws IllegalAccessException {
		super.setBackingField(value * fieldScale);
	}
	
	@Override protected Float getFromBackingField() throws IllegalAccessException {
		return super.getFromBackingField() / fieldScale;
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
	
	@Override public Optional<Number> deserializeStringKey(@NotNull String key) {
		try {
			return Optional.of(Float.parseFloat(key));
		} catch (NumberFormatException e) {
			return Optional.empty();
		}
	}
}
