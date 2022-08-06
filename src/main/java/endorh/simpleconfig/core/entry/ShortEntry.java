package endorh.simpleconfig.core.entry;

import endorh.simpleconfig.core.IKeyEntry;
import endorh.simpleconfig.core.ISimpleConfigEntryHolder;
import endorh.simpleconfig.ui.api.AbstractConfigListEntry;
import endorh.simpleconfig.ui.api.ConfigEntryBuilder;
import endorh.simpleconfig.ui.impl.builders.IntFieldBuilder;
import endorh.simpleconfig.ui.impl.builders.IntSliderBuilder;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

@Deprecated
public class ShortEntry extends AbstractRangedEntry<Short, Number, Integer, ShortEntry>
  implements IKeyEntry<Short> {
	@Internal public ShortEntry(
	  ISimpleConfigEntryHolder parent, String name, short value
	) {
		super(parent, name, value);
	}
	
	public static class Builder
	  extends AbstractRangedEntry.Builder<Short, Number, Integer, ShortEntry, Builder> {
		public Builder(Short value) {
			super(value, Short.class);
		}
		
		/** Set min (inclusive) */
		@Contract(pure=true) public Builder min(short min) {
			return super.min(min);
		}
		/** Set max (inclusive) */
		@Contract(pure=true) public Builder max(short max) {
			return super.max(max);
		}
		/** Set range (inclusive) */
		@Contract(pure=true) public Builder range(short min, short max) {
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
	@Override public Optional<AbstractConfigListEntry<Integer>> buildGUIEntry(
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
	
}
