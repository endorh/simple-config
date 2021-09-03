package endorh.simple_config.core.entry;

import endorh.simple_config.clothconfig2.api.AbstractConfigListEntry;
import endorh.simple_config.clothconfig2.api.ConfigEntryBuilder;
import endorh.simple_config.clothconfig2.impl.builders.IntFieldBuilder;
import endorh.simple_config.clothconfig2.impl.builders.IntSliderBuilder;
import endorh.simple_config.core.IKeyEntry;
import endorh.simple_config.core.ISimpleConfigEntryHolder;
import endorh.simple_config.core.entry.DoubleEntry.Builder;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class IntegerEntry extends AbstractRangedEntry<Integer, Number, Integer, IntegerEntry>
  implements IKeyEntry<Number, Integer> {
	@Internal public IntegerEntry(
	  ISimpleConfigEntryHolder parent, String name, int value
	) {
		super(parent, name, value, Integer.class);
	}
	
	public static class Builder extends AbstractRangedEntry.Builder<Integer, Number, Integer, IntegerEntry, Builder> {
		public Builder(Integer value) {
			super(value, Integer.class);
		}
		
		public Builder min(int min) {
			return super.min(min);
		}
		public Builder max(int max) {
			return super.max(max);
		}
		public Builder range(int min, int max) {
			return super.range(min, max);
		}
		
		@Override
		protected void checkBounds() {
			min = min == null ? Integer.MIN_VALUE : min;
			max = max == null ? Integer.MAX_VALUE : max;
			super.checkBounds();
		}
		
		@Override
		public IntegerEntry buildEntry(ISimpleConfigEntryHolder parent, String name) {
			return new IntegerEntry(parent, name, value);
		}
		
		@Override protected Builder createCopy() {
			return new Builder(value);
		}
	}
	
	@Nullable
	@Override public Integer fromConfig(@Nullable Number value) {
		return value != null? value.intValue() : null;
	}
	
	@OnlyIn(Dist.CLIENT)
	@Override
	public Optional<AbstractConfigListEntry<Integer>> buildGUIEntry(
	  ConfigEntryBuilder builder
	) {
		if (!asSlider) {
			final IntFieldBuilder valBuilder = builder
			  .startIntField(getDisplayName(), get())
			  .setMin(min).setMax(max);
			return Optional.of(decorate(valBuilder).build());
		} else {
			final IntSliderBuilder valBuilder = builder
			  .startIntSlider(getDisplayName(), get(), min, max)
			  .setTextGetter(sliderTextSupplier);
			return Optional.of(decorate(valBuilder).build());
		}
	}
	
	@Override public Optional<Number> deserializeStringKey(@NotNull String key) {
		try {
			return Optional.of(Integer.parseInt(key));
		} catch (NumberFormatException e) {
			return Optional.empty();
		}
	}
}
