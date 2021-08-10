package endorh.simple_config.core.entry;

import endorh.simple_config.core.ISimpleConfigEntryHolder;
import endorh.simple_config.clothconfig2.api.AbstractConfigListEntry;
import endorh.simple_config.clothconfig2.api.ConfigEntryBuilder;
import endorh.simple_config.clothconfig2.impl.builders.IntListBuilder;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;
import org.jetbrains.annotations.ApiStatus.Internal;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Deprecated
public class ShortListEntry extends RangedListEntry<Short, Number, Integer, ShortListEntry> {
	@Internal
	public ShortListEntry(
	  ISimpleConfigEntryHolder parent, String name,
	  @Nullable List<Short> value
	) {
		super(parent, name, value);
	}
	
	public static class Builder extends RangedListEntry.Builder<Short, Number, Integer, ShortListEntry, Builder> {
		
		public Builder(List<Short> value) {
			super(value);
		}
		
		/**
		 * Set the minimum allowed value for the elements of this list entry (inclusive)
		 */
		public Builder min(short min) {
			return super.min(min);
		}
		
		/**
		 * Set the maximum allowed value for the elements of this list entry (inclusive)
		 */
		public Builder max(short max) {
			return super.max(max);
		}
		
		/**
		 * Set the minimum and the maximum allowed for the elements of this list entry (inclusive)
		 */
		public Builder range(short min, short max) {
			return super.range(min, max);
		}
		
		@Override
		protected void checkBounds() {
			min = min != null ? min : Short.MIN_VALUE;
			max = max != null ? max : Short.MAX_VALUE;
		}
		
		@Override
		protected ShortListEntry buildEntry(ISimpleConfigEntryHolder parent, String name) {
			return new ShortListEntry(parent, name, value);
		}
	}
	
	@Override
	protected Short elemFromConfig(Number value) {
		return value != null? value.shortValue() : null;
	}
	
	@Override
	protected List<Short> get(ConfigValue<?> spec) {
		//noinspection unchecked
		return ((List<Number>) (List<?>) super.get(spec))
		  .stream().map(this::elemFromConfig).collect(Collectors.toList());
	}
	
	@OnlyIn(Dist.CLIENT)
	@Override
	public Optional<AbstractConfigListEntry<List<Integer>>> buildGUIEntry(
	  ConfigEntryBuilder builder
	) {
		final IntListBuilder valBuilder = builder
		  .startIntList(getDisplayName(), forGui(get()))
		  .setDefaultValue(forGui(value))
		  .setMin(min).setMax(max)
		  .setInsertInFront(insertInTop)
		  .setExpanded(expand)
		  .setSaveConsumer(saveConsumer())
		  .setTooltipSupplier(this::supplyTooltip)
		  .setErrorSupplier(this::supplyError);
		return Optional.of(decorate(valBuilder).build());
	}
}
