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
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

@Deprecated
public class ByteEntry extends AbstractRangedEntry<Byte, Number, Integer, ByteEntry>
  implements IKeyEntry<Byte> {
	@Internal public ByteEntry(
	  ISimpleConfigEntryHolder parent, String name, byte value
	) {
		super(parent, name, value);
	}
	
	public static class Builder extends
	                            AbstractRangedEntry.Builder<Byte, Number, Integer, ByteEntry, Builder> {
		public Builder(Byte value) {
			super(value, Byte.class);
		}
		
		/**
		 * Set min (inclusive)
		 */
		public Builder min(byte min) {
			return super.min(min);
		}
		
		/**
		 * Set max (inclusive)
		 */
		public Builder max(byte max) {
			return super.max(max);
		}
		
		/**
		 * Set range (inclusive)
		 */
		public Builder range(byte min, byte max) {
			return super.range(min, max);
		}
		
		@Override
		protected void checkBounds() {
			min = min == null ? Byte.MIN_VALUE : min;
			max = max == null ? Byte.MAX_VALUE : max;
			super.checkBounds();
		}
		
		@Override
		protected ByteEntry buildEntry(ISimpleConfigEntryHolder parent, String name) {
			return new ByteEntry(parent, name, value);
		}
		
		@Override protected Builder createCopy() {
			return new Builder(value);
		}
	}
	
	@Nullable
	@Override public Byte fromConfig(@Nullable Number value) {
		return value != null ? value.byteValue() : null;
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
	
}
