package dnj.simple_config.core.entry;

import dnj.simple_config.core.ISimpleConfigEntryHolder;
import dnj.simple_config.gui.FloatSliderBuilder;
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.impl.builders.FloatFieldBuilder;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class FloatEntry extends RangedEntry<Float, Number, Float, FloatEntry> {
	public FloatEntry(Float value, Float min, Float max) {
		super(value,
		      min == null ? Float.NEGATIVE_INFINITY : min,
		      max == null ? Float.POSITIVE_INFINITY : max, "%.2f", Float.class);
	}
	
	@Override protected void checkBounds() {
		if (min.isNaN() || max.isNaN())
			throw new IllegalArgumentException("NaN bound in float config entry " + getPath());
		if (asSlider && (min.isInfinite() || max.isInfinite()))
			throw new IllegalArgumentException("Infinite bound in slider float config entry " + getPath());
	}
	
	public FloatEntry min(float min) {
		return super.min(min);
	}
	public FloatEntry max(float max) {
		return super.max(max);
	}
	
	@Nullable
	@Override
	protected Float fromConfig(@Nullable Number value) {
		return value != null? value.floatValue() : null;
	}
	
	@OnlyIn(Dist.CLIENT)
	@Override protected Optional<AbstractConfigListEntry<Float>> buildGUIEntry(
	  ConfigEntryBuilder builder, ISimpleConfigEntryHolder c
	) {
		checkBounds();
		if (!asSlider) {
			final FloatFieldBuilder valBuilder = builder
			  .startFloatField(getDisplayName(), c.get(name))
			  .setDefaultValue(value)
			  .setMin(min).setMax(max)
			  .setSaveConsumer(saveConsumer(c))
			  .setTooltipSupplier(this::supplyTooltip)
			  .setErrorSupplier(this::supplyError);
			return Optional.of(decorate(valBuilder).build());
		} else {
			final FloatSliderBuilder valBuilder =
			  new FloatSliderBuilder(
			    builder, getDisplayName(), c.get(name),
			    min != null? min : Float.NEGATIVE_INFINITY,
			    max != null? max : Float.POSITIVE_INFINITY)
			  .setDefaultValue(value)
			  .setSaveConsumer(saveConsumer(c))
			  .setTooltipSupplier(this::supplyTooltip)
			  .setErrorSupplier(this::supplyError)
			  .setTextGetter(sliderTextSupplier);
			return Optional.of(decorate(valBuilder).build());
		}
	}
}
