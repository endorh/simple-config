package endorh.simpleconfig.core.entry;

import endorh.simpleconfig.api.ConfigEntryHolder;
import endorh.simpleconfig.api.entry.ShortEntryBuilder;
import endorh.simpleconfig.core.IKeyEntry;
import endorh.simpleconfig.ui.api.ConfigFieldBuilder;
import endorh.simpleconfig.ui.impl.builders.FieldBuilder;
import endorh.simpleconfig.ui.impl.builders.IntFieldBuilder;
import endorh.simpleconfig.ui.impl.builders.IntSliderBuilder;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

@Deprecated
public class ShortEntry extends AbstractRangedEntry<Short, Number, Integer>
  implements IKeyEntry<Short> {
	@Internal public ShortEntry(
	  ConfigEntryHolder parent, String name, short value
	) {
		super(parent, name, value);
	}
	
	public static class Builder
	  extends AbstractRangedEntry.Builder<Short, Number, Integer, ShortEntry, ShortEntryBuilder, Builder>
	  implements ShortEntryBuilder {
		public Builder(Short value) {
			super(value, Short.class);
		}
		
		@Override @Contract(pure=true) public ShortEntryBuilder min(short min) {
			return super.min(min);
		}
		@Override @Contract(pure=true) public ShortEntryBuilder max(short max) {
			return super.max(max);
		}
		@Override @Contract(pure=true) public ShortEntryBuilder range(short min, short max) {
			return super.range(min, max);
		}
		
		@Override
		protected void checkBounds() {
			min = min == null ? Short.MIN_VALUE : min;
			max = max == null ? Short.MAX_VALUE : max;
			super.checkBounds();
		}
		
		@Override
		protected ShortEntry buildEntry(ConfigEntryHolder parent, String name) {
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
	@Override public Optional<FieldBuilder<Integer, ?, ?>> buildGUIEntry(
	  ConfigFieldBuilder builder
	) {
		if (!asSlider) {
			final IntFieldBuilder valBuilder = builder
			  .startIntField(getDisplayName(), get())
			  .setMin(forGui(min)).setMax(forGui(max));
			return Optional.of(decorate(valBuilder));
		} else {
			final IntSliderBuilder valBuilder = builder
			  .startIntSlider(getDisplayName(), get(), forGui(min), forGui(max))
			  .setTextGetter(g -> sliderTextSupplier.apply(fromGui(g)));
			return Optional.of(decorate(valBuilder));
		}
	}
	
}
