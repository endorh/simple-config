package endorh.simple_config.core.entry;

import endorh.simple_config.core.ISimpleConfigEntryHolder;
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.impl.builders.IntListBuilder;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;
import org.jetbrains.annotations.ApiStatus.Internal;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Deprecated
public class ByteListEntry extends RangedListEntry<Byte, Number, Integer, ByteListEntry> {
	@Internal
	public ByteListEntry(
	  ISimpleConfigEntryHolder parent, String name,
	  @Nullable List<Byte> value
	) {
		super(parent, name, value);
	}
	
	public static class Builder extends RangedListEntry.Builder<Byte, Number, Integer, ByteListEntry, Builder> {
		
		public Builder(List<Byte> value) {
			super(value);
		}
		
		/**
		 * Set the minimum allowed value for the elements of this list entry (inclusive)
		 */
		public Builder min(byte min) {
			return super.min(min);
		}
		
		/**
		 * Set the maximum allowed value for the elements of this list entry (inclusive)
		 */
		public Builder max(byte max) {
			return super.max(max);
		}
		
		/**
		 * Set the minimum and the maximum allowed for the elements of this list entry (inclusive)
		 */
		public Builder range(byte min, byte max) {
			return super.range(min, max);
		}
		
		@Override
		protected void checkBounds() {
			min = min != null ? min : Byte.MIN_VALUE;
			max = max != null ? max : Byte.MAX_VALUE;
		}
		
		@Override
		protected ByteListEntry buildEntry(ISimpleConfigEntryHolder parent, String name) {
			return new ByteListEntry(parent, name, value);
		}
	}
	
	@Override
	protected Byte elemFromConfig(Number value) {
		return value != null? value.byteValue() : null;
	}
	
	@Override
	protected List<Byte> get(ConfigValue<?> spec) {
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
