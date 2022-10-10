package endorh.simpleconfig.core.entry;

import endorh.simpleconfig.api.ConfigEntryHolder;
import endorh.simpleconfig.api.entry.LongEntryBuilder;
import endorh.simpleconfig.core.IKeyEntry;
import endorh.simpleconfig.ui.api.ConfigFieldBuilder;
import endorh.simpleconfig.ui.impl.builders.FieldBuilder;
import endorh.simpleconfig.ui.impl.builders.LongFieldBuilder;
import endorh.simpleconfig.ui.impl.builders.LongSliderBuilder;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class LongEntry extends AbstractRangedEntry<Long, Number, Long>
  implements IKeyEntry<Long> {
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
			super(value, Long.class);
		}
		
		@Override @Contract(pure=true) public @NotNull LongEntryBuilder min(long min) {
			return super.min(min);
		}
		
		@Override @Contract(pure=true) public @NotNull LongEntryBuilder max(long max) {
			return super.max(max);
		}
		
		@Override @Contract(pure=true) public @NotNull LongEntryBuilder range(long min, long max) {
			return super.range(min, max);
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
		
		@Override protected Builder createCopy() {
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
			  .setTextGetter(sliderTextSupplier);
			return Optional.of(decorate(valBuilder));
		}
	}
	
}
