package dnj.simple_config.core.entry;

import dnj.simple_config.core.ISimpleConfigEntryHolder;
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.impl.builders.FloatListBuilder;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class FloatListEntry extends RangedListEntry<Float, Number, Float, FloatListEntry> {
	public FloatListEntry(@Nullable List<Float> value) {
		this(value, null, null);
	}
	
	public FloatListEntry(
	  @Nullable List<Float> value,
	  @Nullable Float min, @Nullable Float max
	) {
		super(value, min != null ? min : Float.NEGATIVE_INFINITY,
		      max != null ? max : Float.POSITIVE_INFINITY);
	}
	
	public FloatListEntry min(float min) {
		return super.min(min);
	}
	public FloatListEntry max(float max) {
		return super.max(max);
	}
	
	@Override
	protected Float elemFromConfig(Number value) {
		return value != null? value.floatValue() : null;
	}
	
	@Override
	protected List<Float> get(ConfigValue<?> spec) {
		//noinspection unchecked
		return ((List<Number>) (List<?>) super.get(spec))
		  .stream().map(this::elemFromConfig).collect(Collectors.toList());
	}
	
	@OnlyIn(Dist.CLIENT)
	@Override
	protected Optional<AbstractConfigListEntry<List<Float>>> buildGUIEntry(
	  ConfigEntryBuilder builder, ISimpleConfigEntryHolder c
	) {
		final FloatListBuilder valBuilder = builder
		  .startFloatList(getDisplayName(), c.get(name))
		  .setDefaultValue(value)
		  .setMin(min).setMax(max)
		  .setExpanded(expand)
		  .setSaveConsumer(saveConsumer(c))
		  .setTooltipSupplier(this::supplyTooltip)
		  .setErrorSupplier(this::supplyError);
		return Optional.of(decorate(valBuilder).build());
	}
}
