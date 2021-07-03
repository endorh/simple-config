package dnj.simple_config.core.entry;

import dnj.simple_config.core.ISimpleConfigEntryHolder;
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.impl.builders.LongFieldBuilder;
import me.shedaniel.clothconfig2.impl.builders.LongSliderBuilder;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class LongEntry extends RangedEntry<Long, Number, Long, LongEntry> {
	public LongEntry(long value, Long min, Long max) {
		super(value,
		      min == null ? Long.MIN_VALUE : min,
		      max == null ? Long.MAX_VALUE : max, Long.class);
	}
	
	public LongEntry min(long min) {
		return super.min(min);
	}
	public LongEntry max(long max) {
		return super.max(max);
	}
	
	@Nullable
	@Override
	protected Long fromConfig(@Nullable Number value) {
		return value != null? value.longValue() : null;
	}
	
	@OnlyIn(Dist.CLIENT)
	@Override
	protected Optional<AbstractConfigListEntry<Long>> buildGUIEntry(
	  ConfigEntryBuilder builder, ISimpleConfigEntryHolder c
	) {
		if (!asSlider) {
			final LongFieldBuilder valBuilder = builder
			  .startLongField(getDisplayName(), c.get(name))
			  .setDefaultValue(value)
			  .setMin(min).setMax(max)
			  .setSaveConsumer(saveConsumer(c))
			  .setTooltipSupplier(this::supplyTooltip)
			  .setErrorSupplier(this::supplyError);
			return Optional.of(decorate(valBuilder).build());
		} else {
			final LongSliderBuilder valBuilder = builder
			  .startLongSlider(getDisplayName(), c.get(name), min, max)
			  .setDefaultValue(value)
			  .setSaveConsumer(saveConsumer(c))
			  .setTooltipSupplier(this::supplyTooltip)
			  .setTooltipSupplier(this::supplyTooltip)
			  .setErrorSupplier(this::supplyError)
			  .setTextGetter(sliderTextSupplier);
			return Optional.of(decorate(valBuilder).build());
		}
	}
}
