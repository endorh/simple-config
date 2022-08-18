package endorh.simpleconfig.core.entry;

import endorh.simpleconfig.api.ISimpleConfigEntryHolder;
import endorh.simpleconfig.api.entry.FloatEntryBuilder;
import endorh.simpleconfig.core.BackingField.BackingFieldBinding;
import endorh.simpleconfig.core.BackingField.BackingFieldBuilder;
import endorh.simpleconfig.core.IKeyEntry;
import endorh.simpleconfig.ui.api.ConfigFieldBuilder;
import endorh.simpleconfig.ui.impl.builders.FieldBuilder;
import endorh.simpleconfig.ui.impl.builders.FloatFieldBuilder;
import endorh.simpleconfig.ui.impl.builders.FloatSliderBuilder;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class FloatEntry extends AbstractRangedEntry<Float, Number, Float>
  implements IKeyEntry<Float> {
	@Internal public FloatEntry(
	  ISimpleConfigEntryHolder parent, String name, float value
	) {
		super(parent, name, value);
		commentMin = -Float.MAX_VALUE;
		commentMax = Float.MAX_VALUE;
	}
	
	public static class Builder
	  extends AbstractRangedEntry.Builder<Float, Number, Float, FloatEntry, FloatEntryBuilder, Builder>
	  implements FloatEntryBuilder {
		public Builder(Float value) {
			super(value, Float.class, "%.2f");
		}
		
		@Override @Contract(pure=true) public FloatEntryBuilder min(float min) {
			return super.min(min);
		}
		
		@Override @Contract(pure=true) public FloatEntryBuilder max(float max) {
			return super.max(max);
		}
		
		@Override @Contract(pure=true) public FloatEntryBuilder range(float min, float max) {
			return super.range(min, max);
		}
		
		@Override @Contract(pure=true) public FloatEntryBuilder fieldScale(float scale) {
			if (scale == 0F || !Float.isFinite(scale))
				throw new IllegalArgumentException("Scale must be a non-zero finite number");
			return field(f -> f * scale, f -> f / scale, Float.class);
		}
		
		@Override @Contract(pure=true) public FloatEntryBuilder fieldScale(String name, float scale) {
			return addField(BackingFieldBinding.withName(
			  name, BackingFieldBuilder.of(f -> f * scale, Float.class)));
		}
		
		@Override @Contract(pure=true) public FloatEntryBuilder addFieldScale(String suffix, float scale) {
			return addField(suffix, f -> f * scale, Float.class);
		}
		
		@Override @Contract(pure=true) public FloatEntryBuilder add_field_scale(String suffix, float scale) {
			return add_field(suffix, f -> f * scale, Float.class);
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
	@Override public Optional<FieldBuilder<Float, ?, ?>> buildGUIEntry(
	  ConfigFieldBuilder builder
	) {
		if (!asSlider) {
			final FloatFieldBuilder valBuilder = builder
			  .startFloatField(getDisplayName(), get())
			  .setMin(min).setMax(max);
			return Optional.of(decorate(valBuilder));
		} else {
			final FloatSliderBuilder valBuilder =
			  new FloatSliderBuilder(builder, getDisplayName(), get(), min, max)
				 .setTextGetter(sliderTextSupplier);
			return Optional.of(decorate(valBuilder));
		}
	}
	
}
