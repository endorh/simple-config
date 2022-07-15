package endorh.simpleconfig.core.entry;

import endorh.simpleconfig.core.ISimpleConfigEntryHolder;
import endorh.simpleconfig.ui.api.AbstractConfigListEntry;
import endorh.simpleconfig.ui.api.ConfigEntryBuilder;
import endorh.simpleconfig.ui.impl.builders.FloatListBuilder;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class FloatListEntry extends RangedListEntry<Float, Number, Float, FloatListEntry> {
	@Internal public FloatListEntry(
	  ISimpleConfigEntryHolder parent, String name,
	  @Nullable List<Float> value
	) {
		super(parent, name, value);
	}
	
	public static class Builder extends RangedListEntry.Builder<Float, Number, Float, FloatListEntry, Builder> {
		public Builder(List<Float> value) {
			super(value, Float.class);
		}
		
		/**
		 * Set the minimum allowed value for the elements of this list entry (inclusive)
		 */
		public Builder min(float min) {
			return super.min(min);
		}
		
		/**
		 * Set the maximum allowed value for the elements of this list entry (inclusive)
		 */
		public Builder max(float max) {
			return super.max(max);
		}
		
		/**
		 * Set the minimum and the maximum allowed for the elements of this list entry (inclusive)
		 */
		public Builder range(float min, float max) {
			return super.range(min, max);
		}
		
		@Override
		protected void checkBounds() {
			min = min != null ? min : Float.NEGATIVE_INFINITY;
			max = max != null ? max : Float.POSITIVE_INFINITY;
		}
		
		@Override
		protected FloatListEntry buildEntry(ISimpleConfigEntryHolder parent, String name) {
			return new FloatListEntry(parent, name, value);
		}
		
		@Override protected Builder createCopy() {
			return new Builder(value);
		}
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
	
	@OnlyIn(Dist.CLIENT) @Override
	public Optional<AbstractConfigListEntry<List<Float>>> buildGUIEntry(ConfigEntryBuilder builder) {
		final FloatListBuilder valBuilder = builder
		  .startFloatList(getDisplayName(), get());
		return Optional.of(decorate(valBuilder).build());
	}
}
