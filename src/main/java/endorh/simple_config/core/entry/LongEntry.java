package endorh.simple_config.core.entry;

import endorh.simple_config.core.ISimpleConfigEntryHolder;
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.impl.builders.LongFieldBuilder;
import me.shedaniel.clothconfig2.impl.builders.LongSliderBuilder;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class LongEntry extends AbstractRangedEntry<Long, Number, Long, LongEntry> {
	@Internal public LongEntry(
	  ISimpleConfigEntryHolder parent, String name, long value
	) {
		super(parent, name, value, Long.class);
	}
	
	public static class Builder extends AbstractRangedEntry.Builder<Long, Number, Long, LongEntry, Builder> {
		
		public Builder(Long value) {
			super(value, Long.class);
		}
		
		public Builder min(long min) {
			return super.min(min);
		}
		public Builder max(long max) {
			return super.max(max);
		}
		public Builder range(long min, long max) {
			return super.range(min, max);
		}
		
		@Override
		protected void checkBounds() {
			min = min == null ? Long.MIN_VALUE : min;
			max = max == null ? Long.MAX_VALUE : max;
			super.checkBounds();
		}
		
		@Override
		public LongEntry buildEntry(ISimpleConfigEntryHolder parent, String name) {
			return new LongEntry(parent, name, value);
		}
	}
	
	@Nullable
	@Override
	protected Long fromConfig(@Nullable Number value) {
		return value != null? value.longValue() : null;
	}
	
	@OnlyIn(Dist.CLIENT)
	@Override
	protected Optional<AbstractConfigListEntry<Long>> buildGUIEntry(
	  ConfigEntryBuilder builder
	) {
		if (!asSlider) {
			final LongFieldBuilder valBuilder = builder
			  .startLongField(getDisplayName(), get())
			  .setDefaultValue(value)
			  .setMin(min).setMax(max)
			  .setSaveConsumer(saveConsumer())
			  .setTooltipSupplier(this::supplyTooltip)
			  .setErrorSupplier(this::supplyError);
			return Optional.of(decorate(valBuilder).build());
		} else {
			final LongSliderBuilder valBuilder = builder
			  .startLongSlider(getDisplayName(), get(), min, max)
			  .setDefaultValue(value)
			  .setSaveConsumer(saveConsumer())
			  .setTooltipSupplier(this::supplyTooltip)
			  .setTooltipSupplier(this::supplyTooltip)
			  .setErrorSupplier(this::supplyError)
			  .setTextGetter(sliderTextSupplier);
			return Optional.of(decorate(valBuilder).build());
		}
	}
}
