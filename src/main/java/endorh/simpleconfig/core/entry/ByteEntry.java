package endorh.simpleconfig.core.entry;

import endorh.simpleconfig.api.ConfigEntryHolder;
import endorh.simpleconfig.api.entry.ByteEntryBuilder;
import endorh.simpleconfig.core.AtomicEntry;
import endorh.simpleconfig.core.EntryType;
import endorh.simpleconfig.ui.api.ConfigFieldBuilder;
import endorh.simpleconfig.ui.impl.builders.FieldBuilder;
import endorh.simpleconfig.ui.impl.builders.IntFieldBuilder;
import endorh.simpleconfig.ui.impl.builders.IntSliderBuilder;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

@Deprecated
public class ByteEntry extends AbstractRangedEntry<Byte, Number, Integer>
  implements AtomicEntry<Byte> {
	@Internal public ByteEntry(
	  ConfigEntryHolder parent, String name, byte value
	) {
		super(parent, name, value);
	}
	
	public static class Builder
	  extends AbstractRangedEntry.Builder<Byte, Number, Integer, ByteEntry, ByteEntryBuilder, Builder>
	  implements ByteEntryBuilder {
		public Builder(Byte value) {
			super(value, EntryType.of(Byte.class));
		}
		
		@Override
		protected void checkBounds() {
			min = min == null ? Byte.MIN_VALUE : min;
			max = max == null ? Byte.MAX_VALUE : max;
			super.checkBounds();
		}
		
		@Override
		protected ByteEntry buildEntry(ConfigEntryHolder parent, String name) {
			return new ByteEntry(parent, name, value);
		}
		
		@Override protected Builder createCopy(Byte value) {
			return new Builder(value);
		}
	}
	
	@Nullable
	@Override public Byte fromConfig(@Nullable Number value) {
		return value != null ? value.byteValue() : null;
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
			  .setSliderMin(sliderMin != null? (int) sliderMin : null)
			  .setSliderMax(sliderMax != null? (int) sliderMax : null)
			  .setSliderMap(sliderMap)
			  .setTextGetter(g -> sliderTextSupplier.apply(fromGui(g)));
			return Optional.of(decorate(valBuilder));
		}
	}
	
}
