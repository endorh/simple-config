package endorh.simpleconfig.core.entry;

import endorh.simpleconfig.clothconfig2.api.AbstractConfigListEntry;
import endorh.simpleconfig.clothconfig2.api.ConfigEntryBuilder;
import endorh.simpleconfig.clothconfig2.impl.builders.IntListBuilder;
import endorh.simpleconfig.core.ISimpleConfigEntryHolder;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;
import org.jetbrains.annotations.ApiStatus.Internal;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class IntegerListEntry extends RangedListEntry<Integer, Number, Integer, IntegerListEntry> {
	@Internal
	public IntegerListEntry(
	  ISimpleConfigEntryHolder parent, String name,
	  @Nullable List<Integer> value
	) {
		super(parent, name, value);
	}
	
	public static class Builder extends RangedListEntry.Builder<Integer, Number, Integer, IntegerListEntry, Builder> {
		
		public Builder(List<Integer> value) {
			super(value, Integer.class);
		}
		
		/**
		 * Set the minimum allowed value for the elements of this list entry (inclusive)
		 */
		public Builder min(int min) {
			return super.min(min);
		}
		
		/**
		 * Set the maximum allowed value for the elements of this list entry (inclusive)
		 */
		public Builder max(int max) {
			return super.max(max);
		}
		
		/**
		 * Set the minimum and the maximum allowed for the elements of this list entry (inclusive)
		 */
		public Builder range(int min, int max) {
			return super.range(min, max);
		}
		
		@Override
		protected void checkBounds() {
			min = min != null ? min : Integer.MIN_VALUE;
			max = max != null ? max : Integer.MAX_VALUE;
		}
		
		@Override
		protected IntegerListEntry buildEntry(ISimpleConfigEntryHolder parent, String name) {
			return new IntegerListEntry(parent, name, value);
		}
		
		@Override protected Builder createCopy() {
			return new Builder(value);
		}
	}
	
	@Override
	protected Integer elemFromConfig(Number value) {
		return value != null? value.intValue() : null;
	}
	
	@Override
	protected List<Integer> get(ConfigValue<?> spec) {
		//noinspection unchecked
		return ((List<Number>) (List<?>) super.get(spec))
		  .stream().map(this::elemFromConfig).collect(Collectors.toList());
	}
	
	@OnlyIn(Dist.CLIENT) @Override
	public Optional<AbstractConfigListEntry<List<Integer>>> buildGUIEntry(ConfigEntryBuilder builder) {
		final IntListBuilder valBuilder = builder
		  .startIntList(getDisplayName(), get());
		return Optional.of(decorate(valBuilder).build());
	}
}
