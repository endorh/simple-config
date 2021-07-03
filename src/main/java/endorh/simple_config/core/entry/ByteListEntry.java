package endorh.simple_config.core.entry;

import endorh.simple_config.core.ISimpleConfigEntryHolder;
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.impl.builders.IntListBuilder;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Deprecated
public class ByteListEntry extends RangedListEntry<Byte, Number, Integer, ByteListEntry> {
	public ByteListEntry(@Nullable List<Byte> value) {
		this(value, null, null);
	}
	
	public ByteListEntry(
	  @Nullable List<Byte> value, @Nullable Byte min, @Nullable Byte max
	) {
		super(value, min != null ? min : Byte.MIN_VALUE, max != null ? max : Byte.MAX_VALUE);
	}
	
	public ByteListEntry min(byte min) {
		return super.min(min);
	}
	public ByteListEntry max(byte max) {
		return super.max(max);
	}
	
	@Override
	protected Byte elemFromConfig(Number value) {
		return value != null? value.byteValue() : null;
	}
	
	@Override
	protected Integer elemForGui(Byte value) {
		return value != null? value.intValue() : null;
	}
	
	@Override
	protected Byte elemFromGui(Integer value) {
		return value != null? value.byteValue() : null;
	}
	
	@Override
	protected List<Byte> get(ConfigValue<?> spec) {
		//noinspection unchecked
		return ((ConfigValue<List<Number>>) spec).get().stream().map(this::elemFromConfig)
		  .collect(Collectors.toList());
	}
	
	@OnlyIn(Dist.CLIENT)
	@Override
	protected Optional<AbstractConfigListEntry<List<Integer>>> buildGUIEntry(
	  ConfigEntryBuilder builder, ISimpleConfigEntryHolder c
	) {
		final IntListBuilder valBuilder = builder
		  .startIntList(getDisplayName(), forGui(c.get(name)))
		  .setDefaultValue(forGui(value))
		  .setMin(elemForGui(min)).setMax(elemForGui(max))
		  .setExpanded(expand)
		  .setSaveConsumer(saveConsumer(c))
		  .setTooltipSupplier(this::supplyTooltip)
		  .setErrorSupplier(this::supplyError);
		return Optional.of(decorate(valBuilder).build());
	}
}
