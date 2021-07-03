package dnj.simple_config.core.entry;

import dnj.simple_config.core.ISimpleConfigEntryHolder;
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.impl.builders.LongListBuilder;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class LongListEntry extends RangedListEntry<Long, Number, Long, LongListEntry> {
	public LongListEntry(@Nullable List<Long> value) {
		this(value, null, null);
	}
	
	public LongListEntry(
	  @Nullable List<Long> value, @Nullable Long min, @Nullable Long max
	) {
		super(value, min != null ? min : Long.MIN_VALUE, max != null ? max : Long.MAX_VALUE);
	}
	
	public LongListEntry min(long min) {
		return super.min(min);
	}
	public LongListEntry max(long max) {
		return super.max(max);
	}
	
	@Override
	protected Long elemFromConfig(Number value) {
		return value != null? value.longValue() : null;
	}
	
	@Override
	protected List<Long> get(ConfigValue<?> spec) {
		//noinspection unchecked
		return ((ConfigValue<List<Number>>) spec).get().stream().map(this::elemFromConfig)
		  .collect(Collectors.toList());
	}
	
	@OnlyIn(Dist.CLIENT)
	@Override
	protected Optional<AbstractConfigListEntry<List<Long>>> buildGUIEntry(
	  ConfigEntryBuilder builder, ISimpleConfigEntryHolder c
	) {
		final LongListBuilder valBuilder = builder
		  .startLongList(getDisplayName(), c.get(name))
		  .setDefaultValue(value)
		  .setMin(min).setMax(max)
		  .setExpanded(expand)
		  .setSaveConsumer(saveConsumer(c))
		  .setTooltipSupplier(this::supplyTooltip)
		  .setErrorSupplier(this::supplyError);
		return Optional.of(decorate(valBuilder).build());
	}
}
