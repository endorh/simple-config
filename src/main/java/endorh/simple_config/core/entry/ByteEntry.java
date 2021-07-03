package endorh.simple_config.core.entry;

import endorh.simple_config.core.ISimpleConfigEntryHolder;
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.impl.builders.IntFieldBuilder;
import me.shedaniel.clothconfig2.impl.builders.IntSliderBuilder;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

@Deprecated
public class ByteEntry extends RangedEntry<Byte, Number, Integer, ByteEntry> {
	public ByteEntry(Byte value, Byte min, Byte max) {
		super(value,
		      min == null ? Byte.MIN_VALUE : min,
		      max == null ? Byte.MAX_VALUE : max, Byte.class);
	}
	
	public ByteEntry min(byte min) {
		return super.min(min);
	}
	public ByteEntry max(byte max) {
		return super.max(max);
	}
	
	@Nullable
	@Override
	protected Byte fromConfig(@Nullable Number value) {
		return value != null? value.byteValue() : null;
	}
	
	@Override protected Integer forGui(Byte value) {
		return value != null? value.intValue() : null;
	}
	
	@Nullable @Override protected Byte fromGui(@Nullable Integer value) {
		return value != null? value.byteValue() : null;
	}
	
	@OnlyIn(Dist.CLIENT)
	@Override
	protected Optional<AbstractConfigListEntry<Integer>> buildGUIEntry(
	  ConfigEntryBuilder builder, ISimpleConfigEntryHolder c
	) {
		if (!asSlider) {
			final IntFieldBuilder valBuilder = builder
			  .startIntField(getDisplayName(), forGui(c.get(name)))
			  .setDefaultValue(forGui(value))
			  .setMin(forGui(min)).setMax(forGui(max))
			  .setSaveConsumer(saveConsumer(c))
			  .setTooltipSupplier(this::supplyTooltip)
			  .setErrorSupplier(this::supplyError);
			return Optional.of(decorate(valBuilder).build());
		} else {
			final IntSliderBuilder valBuilder = builder
			  .startIntSlider(getDisplayName(), forGui(c.get(name)), forGui(min), forGui(max))
			  .setDefaultValue(forGui(value))
			  .setSaveConsumer(saveConsumer(c))
			  .setTooltipSupplier(this::supplyTooltip)
			  .setTooltipSupplier(this::supplyTooltip)
			  .setErrorSupplier(this::supplyError)
			  .setTextGetter(v -> sliderTextSupplier.apply(fromGui(v)));
			return Optional.of(decorate(valBuilder).build());
		}
	}
}
