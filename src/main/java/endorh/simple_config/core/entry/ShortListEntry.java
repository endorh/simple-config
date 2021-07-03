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
public class ShortListEntry extends RangedListEntry<Short, Number, Integer, ShortListEntry> {
	public ShortListEntry(@Nullable List<Short> value) {
		this(value, null, null);
	}
	
	public ShortListEntry(
	  @Nullable List<Short> value, @Nullable Short min, @Nullable Short max
	) {
		super(value, min != null ? min : Short.MIN_VALUE, max != null ? max : Short.MAX_VALUE);
	}
	
	public ShortListEntry min(short min) {
		return super.min(min);
	}
	public ShortListEntry max(short max) {
		return super.max(max);
	}
	
	@Override
	protected Short elemFromConfig(Number value) {
		return value != null? value.shortValue() : null;
	}
	
	@Override
	protected Integer elemForGui(Short value) {
		return value != null? value.intValue() : null;
	}
	
	@Override
	protected Short elemFromGui(Integer value) {
		return value != null? value.shortValue() : null;
	}
	
	@Override
	protected List<Short> get(ConfigValue<?> spec) {
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
