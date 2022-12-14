package endorh.simpleconfig.core.entry;

import endorh.simpleconfig.api.ConfigEntryHolder;
import endorh.simpleconfig.api.entry.LongEntryBuilder;
import endorh.simpleconfig.core.AtomicEntry;
import endorh.simpleconfig.core.EntryType;
import endorh.simpleconfig.ui.api.ConfigFieldBuilder;
import endorh.simpleconfig.ui.impl.builders.FieldBuilder;
import endorh.simpleconfig.ui.impl.builders.LongFieldBuilder;
import endorh.simpleconfig.ui.impl.builders.LongSliderBuilder;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class LongEntry extends AbstractRangedEntry<Long, Number, Long>
  implements AtomicEntry<Long> {
	@Internal public LongEntry(
	  ConfigEntryHolder parent, String name, long value
	) {
		super(parent, name, value);
		commentMin = Long.MIN_VALUE;
		commentMax = Long.MAX_VALUE;
	}
	
	public static class Builder
	  extends AbstractRangedEntry.Builder<Long, Number, Long, LongEntry, LongEntryBuilder, Builder>
	  implements LongEntryBuilder {
		
		public Builder(Long value) {
			super(value, EntryType.of(Long.class));
		}
		
		@Override
		protected void checkBounds() {
			min = min == null ? Long.MIN_VALUE : min;
			max = max == null ? Long.MAX_VALUE : max;
			super.checkBounds();
		}
		
		@Override
		protected LongEntry buildEntry(ConfigEntryHolder parent, String name) {
			return new LongEntry(parent, name, value);
		}
		
		@Override protected Builder createCopy(Long value) {
			return new Builder(value);
		}
	}
	
	@Nullable
	@Override public Long fromConfig(@Nullable Number value) {
		return value != null ? value.longValue() : null;
	}
	
	@OnlyIn(Dist.CLIENT)
	@Override public Optional<FieldBuilder<Long, ?, ?>> buildGUIEntry(
	  ConfigFieldBuilder builder
	) {
		if (!asSlider) {
			final LongFieldBuilder valBuilder = builder
			  .startLongField(getDisplayName(), get())
			  .setMin(min).setMax(max);
			return Optional.of(decorate(valBuilder));
		} else {
			final LongSliderBuilder valBuilder = builder
			  .startLongSlider(getDisplayName(), get(), min, max)
			  .setSliderMin(sliderMin)
			  .setSliderMax(sliderMax)
			  .setSliderMap(sliderMap)
			  .setTextGetter(sliderTextSupplier);
			return Optional.of(decorate(valBuilder));
		}
	}
	
}
