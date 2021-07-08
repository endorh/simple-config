package endorh.simple_config.core.entry;

import endorh.simple_config.core.ISimpleConfigEntryHolder;
import endorh.simple_config.gui.FloatSliderBuilder;
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.impl.builders.FloatFieldBuilder;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class FloatEntry extends AbstractRangedEntry<Float, Number, Float, FloatEntry> {
	@Internal public FloatEntry(
	  ISimpleConfigEntryHolder parent, String name, float value
	) {
		super(parent, name, value, Float.class);
	}
	
	public static class Builder extends AbstractRangedEntry.Builder<Float, Number, Float, FloatEntry, Builder> {
		
		public Builder(Float value) {
			super(value, Float.class, "%.2f");
		}
		
		public Builder min(float min) {
			return super.min(min);
		}
		public Builder max(float max) {
			return super.max(max);
		}
		public Builder range(float min, float max) {
			return super.range(min, max);
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
		public FloatEntry buildEntry(ISimpleConfigEntryHolder parent, String name) {
			return new FloatEntry(parent, name, value);
		}
	}
	
	@Nullable
	@Override
	protected Float fromConfig(@Nullable Number value) {
		return value != null? value.floatValue() : null;
	}
	
	@OnlyIn(Dist.CLIENT)
	@Override
	protected Optional<AbstractConfigListEntry<Float>> buildGUIEntry(
	  ConfigEntryBuilder builder
	) {
		if (!asSlider) {
			final FloatFieldBuilder valBuilder = builder
			  .startFloatField(getDisplayName(), get())
			  .setDefaultValue(value)
			  .setMin(min).setMax(max)
			  .setSaveConsumer(saveConsumer())
			  .setTooltipSupplier(this::supplyTooltip)
			  .setErrorSupplier(this::supplyError);
			return Optional.of(decorate(valBuilder).build());
		} else {
			final FloatSliderBuilder valBuilder =
			  new FloatSliderBuilder(builder, getDisplayName(), get(), min, max)
				 .setDefaultValue(value)
				 .setSaveConsumer(saveConsumer())
				 .setTooltipSupplier(this::supplyTooltip)
				 .setErrorSupplier(this::supplyError)
				 .setTextGetter(sliderTextSupplier);
			return Optional.of(decorate(valBuilder).build());
		}
	}
}
