package endorh.simpleconfig.core.entry;

import endorh.simpleconfig.core.IKeyEntry;
import endorh.simpleconfig.core.ISimpleConfigEntryHolder;
import endorh.simpleconfig.ui.api.ConfigEntryBuilder;
import endorh.simpleconfig.ui.impl.builders.FieldBuilder;
import endorh.simpleconfig.ui.impl.builders.LongFieldBuilder;
import endorh.simpleconfig.ui.impl.builders.LongSliderBuilder;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class LongEntry extends AbstractRangedEntry<Long, Number, Long, LongEntry>
  implements IKeyEntry<Long> {
	@Internal public LongEntry(
	  ISimpleConfigEntryHolder parent, String name, long value
	) {
		super(parent, name, value);
		commentMin = Long.MIN_VALUE;
		commentMax = Long.MAX_VALUE;
	}
	
	public static class Builder
	  extends AbstractRangedEntry.Builder<Long, Number, Long, LongEntry, Builder> {
		
		public Builder(Long value) {
			super(value, Long.class);
		}
		
		/**
		 * Set min (inclusive)
		 */
		@Contract(pure=true) public Builder min(long min) {
			return super.min(min);
		}
		
		/**
		 * Set max (inclusive)
		 */
		@Contract(pure=true) public Builder max(long max) {
			return super.max(max);
		}
		
		/**
		 * Set range (inclusive)
		 */
		@Contract(pure=true) public Builder range(long min, long max) {
			return super.range(min, max);
		}
		
		@Override
		protected void checkBounds() {
			min = min == null ? Long.MIN_VALUE : min;
			max = max == null ? Long.MAX_VALUE : max;
			super.checkBounds();
		}
		
		@Override
		protected LongEntry buildEntry(ISimpleConfigEntryHolder parent, String name) {
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
	  ConfigEntryBuilder builder
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
