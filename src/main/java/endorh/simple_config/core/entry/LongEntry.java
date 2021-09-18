package endorh.simple_config.core.entry;

import endorh.simple_config.clothconfig2.api.AbstractConfigListEntry;
import endorh.simple_config.clothconfig2.api.ConfigEntryBuilder;
import endorh.simple_config.clothconfig2.impl.builders.LongFieldBuilder;
import endorh.simple_config.clothconfig2.impl.builders.LongSliderBuilder;
import endorh.simple_config.core.IKeyEntry;
import endorh.simple_config.core.ISimpleConfigEntryHolder;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class LongEntry extends AbstractRangedEntry<Long, Number, Long, LongEntry>
  implements IKeyEntry<Number, Long> {
	@Internal public LongEntry(
	  ISimpleConfigEntryHolder parent, String name, long value
	) {
		super(parent, name, value);
		commentMin = Long.MIN_VALUE;
		commentMax = Long.MAX_VALUE;
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
		
		@Override protected Builder createCopy() {
			return new Builder(value);
		}
	}
	
	@Nullable
	@Override public Long fromConfig(@Nullable Number value) {
		return value != null? value.longValue() : null;
	}
	
	@OnlyIn(Dist.CLIENT)
	@Override
	public Optional<AbstractConfigListEntry<Long>> buildGUIEntry(
	  ConfigEntryBuilder builder
	) {
		if (!asSlider) {
			final LongFieldBuilder valBuilder = builder
			  .startLongField(getDisplayName(), get())
			  .setMin(min).setMax(max);
			return Optional.of(decorate(valBuilder).build());
		} else {
			final LongSliderBuilder valBuilder = builder
			  .startLongSlider(getDisplayName(), get(), min, max)
			  .setTextGetter(sliderTextSupplier);
			return Optional.of(decorate(valBuilder).build());
		}
	}
	
	@Override public Optional<Number> deserializeStringKey(@NotNull String key) {
		try {
			return Optional.of(Long.parseLong(key));
		} catch (NumberFormatException ignored) {
			return Optional.empty();
		}
	}
}
