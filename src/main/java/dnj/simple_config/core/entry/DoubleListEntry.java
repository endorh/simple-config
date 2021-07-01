package dnj.simple_config.core.entry;

import dnj.simple_config.core.AbstractConfigEntry;
import dnj.simple_config.core.ISimpleConfigEntryHolder;
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.impl.builders.DoubleListBuilder;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class DoubleListEntry extends RangedListEntry<Double, Number, Double, DoubleListEntry> {
	public DoubleListEntry(@Nullable List<Double> value) {
		this(value, null, null);
	}
	
	public DoubleListEntry(
	  @Nullable List<Double> value,
	  @Nullable Double min, @Nullable Double max
	) {
		super(value, min != null ? min : Double.NEGATIVE_INFINITY,
		      max != null ? max : Double.POSITIVE_INFINITY);
	}
	
	@Override
	protected Double elemFromConfig(Number value) {
		return value.doubleValue();
	}
	
	@Override
	protected List<Double> get(ConfigValue<?> spec) {
		// Sometimes Night Config returns lists of subtypes, so we cast them
		//noinspection unchecked
		return ((List<Number>) (List<?>) super.get(spec))
		  .stream().map(Number::doubleValue).collect(Collectors.toList());
	}
	
	@OnlyIn(Dist.CLIENT)
	@Override
	protected Optional<AbstractConfigListEntry<?>> buildGUIEntry(
	  ConfigEntryBuilder builder, ISimpleConfigEntryHolder c
	) {
		final DoubleListBuilder valBuilder = builder
		  .startDoubleList(getDisplayName(), c.get(name))
		  .setDefaultValue(value)
		  .setMin(min).setMax(max)
		  .setExpanded(expand)
		  .setSaveConsumer(saveConsumer(c))
		  .setTooltipSupplier(this::supplyTooltip)
		  .setErrorSupplier(this::supplyError);
		return Optional.of(decorate(valBuilder).build());
	}
}
