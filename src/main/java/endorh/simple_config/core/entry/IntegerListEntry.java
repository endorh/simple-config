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

public class IntegerListEntry extends RangedListEntry<Integer, Number, Integer, IntegerListEntry> {
	public IntegerListEntry(@Nullable List<Integer> value) {
		this(value, null, null);
	}
	
	public IntegerListEntry(
	  @Nullable List<Integer> value, @Nullable Integer min, @Nullable Integer max
	) {
		super(value, min != null ? min : Integer.MIN_VALUE, max != null ? max : Integer.MAX_VALUE);
	}
	
	public IntegerListEntry min(int min) {
		return super.min(min);
	}
	public IntegerListEntry max(int max) {
		return super.max(max);
	}
	
	@Override
	protected Integer elemFromConfig(Number value) {
		return value != null? value.intValue() : null;
	}
	
	@Override
	protected List<Integer> get(ConfigValue<?> spec) {
		// Sometimes Night Config returns lists of subtypes, so we cast them
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
		  .startIntList(getDisplayName(), c.get(name))
		  .setDefaultValue(value)
		  .setMin(min).setMax(max)
		  .setExpanded(expand)
		  .setSaveConsumer(saveConsumer(c))
		  .setTooltipSupplier(this::supplyTooltip)
		  .setErrorSupplier(this::supplyError);
		return Optional.of(decorate(valBuilder).build());
	}
}
