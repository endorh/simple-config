package endorh.simpleconfig.core.entry;

import endorh.simpleconfig.api.ConfigEntryHolder;
import endorh.simpleconfig.api.entry.IntegerEntryBuilder;
import endorh.simpleconfig.core.AtomicEntry;
import endorh.simpleconfig.ui.api.ConfigFieldBuilder;
import endorh.simpleconfig.ui.impl.builders.FieldBuilder;
import endorh.simpleconfig.ui.impl.builders.IntFieldBuilder;
import endorh.simpleconfig.ui.impl.builders.IntSliderBuilder;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class IntegerEntry extends AbstractRangedEntry<Integer, Number, Integer>
  implements AtomicEntry<Integer> {
	@Internal public IntegerEntry(
	  ConfigEntryHolder parent, String name, int value
	) {
		super(parent, name, value);
	}
	
	public static class Builder
	  extends AbstractRangedEntry.Builder<Integer, Number, Integer, IntegerEntry, IntegerEntryBuilder, Builder>
	  implements IntegerEntryBuilder {
		public Builder(Integer value) {
			super(value, Integer.class);
		}
		
		@Override @Contract(pure=true) public @NotNull IntegerEntryBuilder min(int min) {
			return super.min(min);
		}
		
		@Override @Contract(pure=true) public @NotNull IntegerEntryBuilder max(int max) {
			return super.max(max);
		}
		
		@Override @Contract(pure=true) public @NotNull IntegerEntryBuilder range(int min, int max) {
			return super.range(min, max);
		}
		
		@Override
		protected void checkBounds() {
			min = min == null ? Integer.MIN_VALUE : min;
			max = max == null ? Integer.MAX_VALUE : max;
			super.checkBounds();
		}
		
		@Override
		protected IntegerEntry buildEntry(ConfigEntryHolder parent, String name) {
			return new IntegerEntry(parent, name, value);
		}
		
		@Override protected Builder createCopy(Integer value) {
			return new Builder(value);
		}
	}
	
	@Nullable
	@Override public Integer fromConfig(@Nullable Number value) {
		return value != null ? value.intValue() : null;
	}
	
	@OnlyIn(Dist.CLIENT)
	@Override public Optional<FieldBuilder<Integer, ?, ?>> buildGUIEntry(
	  ConfigFieldBuilder builder
	) {
		if (!asSlider) {
			final IntFieldBuilder valBuilder = builder
			  .startIntField(getDisplayName(), get())
			  .setMin(min).setMax(max);
			return Optional.of(decorate(valBuilder));
		} else {
			final IntSliderBuilder valBuilder = builder
			  .startIntSlider(getDisplayName(), get(), min, max)
			  .setTextGetter(sliderTextSupplier);
			return Optional.of(decorate(valBuilder));
		}
	}
	
}
