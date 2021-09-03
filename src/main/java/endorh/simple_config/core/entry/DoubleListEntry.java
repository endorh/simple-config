package endorh.simple_config.core.entry;

import endorh.simple_config.core.ISimpleConfigEntryHolder;
import endorh.simple_config.clothconfig2.api.AbstractConfigListEntry;
import endorh.simple_config.clothconfig2.api.ConfigEntryBuilder;
import endorh.simple_config.clothconfig2.impl.builders.DoubleListBuilder;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;
import org.jetbrains.annotations.ApiStatus.Internal;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class DoubleListEntry extends RangedListEntry<Double, Number, Double, DoubleListEntry> {
	@Internal public DoubleListEntry(
	  ISimpleConfigEntryHolder parent, String name,
	  @Nullable List<Double> value
	) {
		super(parent, name, value);
	}
	
	public static class Builder extends RangedListEntry.Builder<Double, Number, Double, DoubleListEntry, Builder> {
		public Builder(List<Double> value) {
			super(value, Double.class);
		}
		
		/**
		 * Set the minimum allowed value for the elements of this list entry (inclusive)
		 */
		public Builder min(double min) {
			return super.min(min);
		}
		
		/**
		 * Set the maximum allowed value for the elements of this list entry (inclusive)
		 */
		public Builder max(double max) {
			return super.max(max);
		}
		
		/**
		 * Set the minimum and the maximum allowed for the elements of this list entry (inclusive)
		 */
		public Builder range(double min, double max) {
			return super.range(min, max);
		}
		
		@Override
		protected void checkBounds() {
			min = min != null ? min : Double.NEGATIVE_INFINITY;
			max = max != null ? max : Double.POSITIVE_INFINITY;
		}
		
		@Override
		protected DoubleListEntry buildEntry(ISimpleConfigEntryHolder parent, String name) {
			return new DoubleListEntry(parent, name, value);
		}
		
		@Override protected Builder createCopy() {
			return new Builder(value);
		}
	}
	
	@Override
	protected Double elemFromConfig(Number value) {
		return value != null? value.doubleValue() : null;
	}
	
	@Override
	protected List<Double> get(ConfigValue<?> spec) {
		//noinspection unchecked
		return ((List<Number>) (List<?>) super.get(spec))
		  .stream().map(this::elemFromConfig).collect(Collectors.toList());
	}
	
	@OnlyIn(Dist.CLIENT) @Override
	public Optional<AbstractConfigListEntry<List<Double>>> buildGUIEntry(ConfigEntryBuilder builder) {
		final DoubleListBuilder valBuilder = builder
		  .startDoubleList(getDisplayName(), get());
		return Optional.of(decorate(valBuilder).build());
	}
}
