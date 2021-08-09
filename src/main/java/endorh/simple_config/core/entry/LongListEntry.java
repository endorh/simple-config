package endorh.simple_config.core.entry;

import endorh.simple_config.core.ISimpleConfigEntryHolder;
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.impl.builders.LongListBuilder;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;
import org.jetbrains.annotations.ApiStatus.Internal;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class LongListEntry extends RangedListEntry<Long, Number, Long, LongListEntry> {
	@Internal public LongListEntry(
	  ISimpleConfigEntryHolder parent, String name,
	  @Nullable List<Long> value
	) {
		super(parent, name, value);
	}
	
	public static class Builder extends RangedListEntry.Builder<Long, Number, Long, LongListEntry, Builder> {
		
		public Builder(List<Long> value) {
			super(value);
		}
		
		/**
		 * Set the minimum allowed value for the elements of this list entry (inclusive)
		 */
		public Builder min(long min) {
			return super.min(min);
		}
		
		/**
		 * Set the maximum allowed value for the elements of this list entry (inclusive)
		 */
		public Builder max(long max) {
			return super.max(max);
		}
		
		/**
		 * Set the minimum and the maximum allowed for the elements of this list entry (inclusive)
		 */
		public Builder range(long min, long max) {
			return super.range(min, max);
		}
		
		@Override
		protected void checkBounds() {
			min = min != null ? min : Long.MIN_VALUE;
			max = max != null ? max : Long.MAX_VALUE;
		}
		
		@Override
		protected LongListEntry buildEntry(ISimpleConfigEntryHolder parent, String name) {
			return new LongListEntry(parent, name, value);
		}
	}
	
	@Override
	protected Long elemFromConfig(Number value) {
		return value != null? value.longValue() : null;
	}
	
	@Override
	protected List<Long> get(ConfigValue<?> spec) {
		//noinspection unchecked
		return ((List<Number>) (List<?>) super.get(spec))
		  .stream().map(this::elemFromConfig).collect(Collectors.toList());
	}
	
	@OnlyIn(Dist.CLIENT)
	@Override
	public Optional<AbstractConfigListEntry<List<Long>>> buildGUIEntry(
	  ConfigEntryBuilder builder
	) {
		final LongListBuilder valBuilder = builder
		  .startLongList(getDisplayName(), get())
		  .setDefaultValue(value)
		  .setMin(min).setMax(max)
		  .setInsertInFront(insertInTop)
		  .setExpanded(expand)
		  .setSaveConsumer(saveConsumer())
		  .setTooltipSupplier(this::supplyTooltip)
		  .setErrorSupplier(this::supplyError);
		return Optional.of(decorate(valBuilder).build());
	}
}
