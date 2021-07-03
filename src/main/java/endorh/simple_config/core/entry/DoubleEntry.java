package endorh.simple_config.core.entry;

import endorh.simple_config.core.ISimpleConfigEntryHolder;
import endorh.simple_config.gui.DoubleSliderBuilder;
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.impl.builders.DoubleFieldBuilder;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class DoubleEntry extends RangedEntry<Double, Number, Double, DoubleEntry> {
	public DoubleEntry(double value, Double min, Double max) {
		super(value,
		      min == null ? Double.NEGATIVE_INFINITY : min,
		      max == null ? Double.POSITIVE_INFINITY : max, "%.2f", Double.class);
	}
	
	@Override protected void checkBounds() {
		if (min.isNaN() || max.isNaN())
			throw new IllegalArgumentException("NaN bound in double config entry " + getPath());
		if (asSlider && (min.isInfinite() || max.isInfinite()))
			throw new IllegalArgumentException("Infinite bound in double config entry " + getPath());
	}
	
	public DoubleEntry min(double min) {
		return super.min(min);
	}
	public DoubleEntry max(double max) {
		return super.max(max);
	}
	
	@Nullable
	@Override
	protected Double fromConfig(@Nullable Number value) {
		return value != null? value.doubleValue() : null;
	}
	
	@OnlyIn(Dist.CLIENT)
	@Override
	protected Optional<AbstractConfigListEntry<Double>> buildGUIEntry(
	  ConfigEntryBuilder builder, ISimpleConfigEntryHolder c
	) {
		checkBounds();
		if (!asSlider) {
			final DoubleFieldBuilder valBuilder = builder
			  .startDoubleField(getDisplayName(), c.get(name))
			  .setDefaultValue(value)
			  .setMin(min).setMax(max)
			  .setSaveConsumer(saveConsumer(c))
			  .setTooltipSupplier(this::supplyTooltip)
			  .setErrorSupplier(this::supplyError);
			return Optional.of(decorate(valBuilder).build());
		} else {
			final DoubleSliderBuilder valBuilder =
			  new DoubleSliderBuilder(builder, getDisplayName(), c.get(name),
			                          min != null? min : Double.NEGATIVE_INFINITY,
			                          max != null? max : Double.POSITIVE_INFINITY)
			  .setDefaultValue(value)
			  .setSaveConsumer(saveConsumer(c))
			  .setTooltipSupplier(this::supplyTooltip)
			  .setErrorSupplier(this::supplyError)
			  .setTextGetter(sliderTextSupplier);
			return Optional.of(decorate(valBuilder).build());
		}
	}
}
