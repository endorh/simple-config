package endorh.simple_config.core.entry;

import endorh.simple_config.core.ISimpleConfigEntryHolder;
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.impl.builders.IntFieldBuilder;
import me.shedaniel.clothconfig2.impl.builders.IntSliderBuilder;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

@Deprecated
public class ByteEntry extends AbstractRangedEntry<Byte, Number, Integer, ByteEntry> {
	@Internal public ByteEntry(
	  ISimpleConfigEntryHolder parent, String name, byte value
	) {
		super(parent, name, value, Byte.class);
	}
	
	public static class Builder extends AbstractRangedEntry.Builder<Byte, Number, Integer, ByteEntry, Builder> {
		public Builder(Byte value) {
			super(value, Byte.class);
		}
		
		public Builder min(byte min) {
			return super.min(min);
		}
		public Builder max(byte max) {
			return super.max(max);
		}
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
		public ByteEntry buildEntry(ISimpleConfigEntryHolder parent, String name) {
			return new ByteEntry(parent, name, value);
		}
	}
	
	@Nullable
	@Override
	protected Byte fromConfig(@Nullable Number value) {
		return value != null? value.byteValue() : null;
	}
	
	@OnlyIn(Dist.CLIENT)
	@Override
	protected Optional<AbstractConfigListEntry<Integer>> buildGUIEntry(
	  ConfigEntryBuilder builder
	) {
		if (!asSlider) {
			final IntFieldBuilder valBuilder = builder
			  .startIntField(getDisplayName(), get())
			  .setDefaultValue(value)
			  .setMin(min).setMax(max)
			  .setSaveConsumer(saveConsumer())
			  .setTooltipSupplier(this::supplyTooltip)
			  .setErrorSupplier(this::supplyError);
			return Optional.of(decorate(valBuilder).build());
		} else {
			final IntSliderBuilder valBuilder = builder
			  .startIntSlider(getDisplayName(), get(), min, max)
			  .setDefaultValue(value)
			  .setSaveConsumer(saveConsumer())
			  .setTooltipSupplier(this::supplyTooltip)
			  .setTooltipSupplier(this::supplyTooltip)
			  .setErrorSupplier(this::supplyError)
			  .setTextGetter(g -> sliderTextSupplier.apply(fromGui(g)));
			return Optional.of(decorate(valBuilder).build());
		}
	}
}
