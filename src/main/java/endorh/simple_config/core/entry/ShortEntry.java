package endorh.simple_config.core.entry;

import endorh.simple_config.clothconfig2.api.AbstractConfigListEntry;
import endorh.simple_config.clothconfig2.api.ConfigEntryBuilder;
import endorh.simple_config.clothconfig2.impl.builders.IntFieldBuilder;
import endorh.simple_config.clothconfig2.impl.builders.IntSliderBuilder;
import endorh.simple_config.core.IKeyEntry;
import endorh.simple_config.core.ISimpleConfigEntryHolder;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

@Deprecated
public class ShortEntry extends AbstractRangedEntry<Short, Number, Integer, ShortEntry>
  implements IKeyEntry<Number, Short> {
	@Internal public ShortEntry(
	  ISimpleConfigEntryHolder parent, String name, short value
	) {
		super(parent, name, value);
		commentMin = Short.MIN_VALUE;
		commentMax = Short.MAX_VALUE;
	}
	
	public static class Builder extends AbstractRangedEntry.Builder<Short, Number, Integer, ShortEntry, Builder> {
		public Builder(Short value) {
			super(value, Short.class);
		}
		
		/** Set min (inclusive) */
		public Builder min(short min) {
			return super.min(min);
		}
		/** Set max (inclusive) */
		public Builder max(short max) {
			return super.max(max);
		}
		/** Set range (inclusive) */
		public Builder range(short min, short max) {
			return super.range(min, max);
		}
		
		@Override
		protected void checkBounds() {
			min = min == null ? Short.MIN_VALUE : min;
			max = max == null ? Short.MAX_VALUE : max;
			super.checkBounds();
		}
		
		@Override
		protected ShortEntry buildEntry(ISimpleConfigEntryHolder parent, String name) {
			return new ShortEntry(parent, name, value);
		}
		
		@Override protected Builder createCopy() {
			return new Builder(value);
		}
	}
	
	@Nullable
	@Override public Short fromConfig(@Nullable Number value) {
		return value != null? value.shortValue() : null;
	}
	
	@OnlyIn(Dist.CLIENT)
	@Override
	public Optional<AbstractConfigListEntry<Integer>> buildGUIEntry(
	  ConfigEntryBuilder builder
	) {
		if (!asSlider) {
			final IntFieldBuilder valBuilder = builder
			  .startIntField(getDisplayName(), get())
			  .setMin(forGui(min)).setMax(forGui(max));
			return Optional.of(decorate(valBuilder).build());
		} else {
			final IntSliderBuilder valBuilder = builder
			  .startIntSlider(getDisplayName(), get(), forGui(min), forGui(max))
			  .setTextGetter(g -> sliderTextSupplier.apply(fromGui(g)));
			return Optional.of(decorate(valBuilder).build());
		}
	}
	
	@Override public Optional<Number> deserializeStringKey(@NotNull String key) {
		try {
			return Optional.of(Short.parseShort(key));
		} catch (NumberFormatException e) {
			return Optional.empty();
		}
	}
}
